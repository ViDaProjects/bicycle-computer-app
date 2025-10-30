package com.beforbike.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterInputStream

class BleServerService : Service() {

    // --- Properties ---
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGattServer: BluetoothGattServer? = null
    private lateinit var advertiser: BluetoothLeAdvertiser
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectedDevices = mutableSetOf<BluetoothDevice>()
    private lateinit var dbHelper: com.beforbike.app.database.RideDbHelper
    private var isAdvertising = false
    private var currentCompanyId: Int = DEFAULT_COMPANY_ID
    private var currentSecretKey: String = DEFAULT_SECRET_KEY

    // Buffer for received data in chunks
    private val receivedDataBuffers = mutableMapOf<String, ByteArrayOutputStream>()

    // --- Constants ---
    companion object {
        private const val TAG = "BleServerService"
        const val ACTION_LOG_UPDATE = "com.beforbike.app.LOG_UPDATE"
        const val EXTRA_LOG_MESSAGE = "EXTRA_LOG_MESSAGE"
        const val ACTION_START_ADVERTISING = "com.beforbike.app.START_ADVERTISING"
        const val ACTION_STOP_ADVERTISING = "com.beforbike.app.STOP_ADVERTISING"
        const val EXTRA_COMPANY_ID = "EXTRA_COMPANY_ID"
        const val EXTRA_SECRET_KEY = "EXTRA_SECRET_KEY"
        internal const val DEFAULT_COMPANY_ID = 0xF0F0
        internal const val DEFAULT_SECRET_KEY = "Oficinas3"
        private const val ADVERTISING_RETRY_DELAY = 5000L
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "ble_server_channel_v1"
        private const val NOTIFICATION_CHANNEL_NAME = "BLE Server Service"
    }

    // AdvertiseCallback
    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            isAdvertising = true
            log("✓ Advertising started successfully!")
            updateForegroundNotification("Advertising active.")
            mainHandler.removeCallbacksAndMessages(null)
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val errorString = advertiseErrorToString(errorCode)
            log("!!! ADVERTISING FAILURE: $errorCode ($errorString)")
            updateForegroundNotification("Advertising failure ($errorString).")

            if(errorCode != AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                mainHandler.postDelayed({
                    if (!isAdvertising) startAdvertising(currentCompanyId, currentSecretKey)
                }, ADVERTISING_RETRY_DELAY)
            }
        }
    }

    // --- Service Lifecycle ---
    override fun onCreate() {
        super.onCreate()
        log("Service creating...")
        dbHelper = com.beforbike.app.database.RideDbHelper(this)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) { log("!!! Bluetooth disabled."); stopSelf(); return }
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) { log("!!! Warning: Multiple advertising may not be supported.") }
        try { advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run { log("!!! Critical Error: Advertiser null."); stopSelf(); return }
        } catch (e: Exception) { log("!!! Error getting Advertiser: ${e.message}."); stopSelf(); return }

        createNotificationChannel()
        val intentFilter = IntentFilter().apply { addAction(ACTION_START_ADVERTISING); addAction(ACTION_STOP_ADVERTISING) }
        ContextCompat.registerReceiver(this, commandReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        log("Advertising receiver registered.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("BLE service started (onStartCommand).")
        startForegroundNotification()
        startServer()
        if (!isAdvertising) {
            log("Trying to start advertising automatically...")
            val companyId = intent?.getIntExtra(EXTRA_COMPANY_ID, DEFAULT_COMPANY_ID) ?: DEFAULT_COMPANY_ID
            val secretKey = intent?.getStringExtra(EXTRA_SECRET_KEY) ?: DEFAULT_SECRET_KEY
            startAdvertising(companyId, secretKey)
        } else { log("Advertising already active.") }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Service destroying (onDestroy)...")
        mainHandler.removeCallbacksAndMessages(null) // Remove advertising retries
        stopAdvertising() // Ensure advertising stops
        stopServer()
        try { unregisterReceiver(commandReceiver) } catch (ignore: Exception) {}
        dbHelper.close()
        log("BLE service stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Main BLE Server Logic ---
    private fun startServer() {
        // FORCE start server regardless of permissions - never fail
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                log("⚠️  No BLUETOOTH_CONNECT permission - server may have limitations");
            }
        }

        if (bluetoothGattServer == null) {
            // Force attempt to create GATT server regardless of permissions
            bluetoothGattServer = try {
                log("Forcing GATT server creation...")
                bluetoothManager.openGattServer(this, gattServerCallback)
            } catch (e: Exception) {
                log("!!! Exception creating GATT server: ${e.message} - but continuing anyway")
                null // Continue without GATT server
            }

            if (bluetoothGattServer != null) {
                log("✓ GATT server successfully created")
                setupGattService()
            } else {
                log("⚠️  GATT server null - running in limited mode")
                // Continue anyway - service will run but with limited functionality
            }
        } else {
            log("GATT server already open.")
        }
    }

    private fun setupGattService() {
        if (bluetoothGattServer == null) { log("!!! setupGattService called without GATT server."); return }
        bluetoothGattServer?.clearServices()
        val service = BluetoothGattService(GattProfile.UUID_SERVICE_TRANSFER, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val dataCharacteristic = BluetoothGattCharacteristic(
            GattProfile.UUID_CHAR_DATA,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(dataCharacteristic)
        val added = bluetoothGattServer?.addService(service)
        log(if (added == true) "GATT service (${service.uuid}) added." else "!!! Failed to add GATT service.")
    }

    private fun stopServer() {
        if (bluetoothGattServer == null) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! No BLUETOOTH_CONNECT permission for stopServer. Cannot disconnect clients or close server.")
            bluetoothGattServer = null
            return
        }
        try {
            if (connectedDevices.isNotEmpty()) {
                log("Desconectando ${connectedDevices.size} dispositivo(s) conectado(s)...")
                for (device in connectedDevices.toList()) {
                    val deviceAddress = try { device.address } catch(e: Exception) {"Perm. negada"}
                    log("  -> Desconectando $deviceAddress")
                    bluetoothGattServer?.cancelConnection(device)
                }
                connectedDevices.clear()
            }
            bluetoothGattServer?.close()
            log("GATT server closed.")
        } catch (e: SecurityException) {
            log("!!! SecurityException stopping server/disconnecting clients: ${e.message}")
        } catch (e: Exception) {
            log("!!! Exception closing GATT server: ${e.message}")
        } finally {
            bluetoothGattServer = null
        }
    }

    // --- startAdvertising ---
    private fun startAdvertising(companyId: Int, secretKey: String) {
        if (isAdvertising) { log("Advertising already active."); return }
        mainHandler.removeCallbacksAndMessages(null) // Clear previous retries
        currentCompanyId = companyId
        currentSecretKey = secretKey
        // Only check BLUETOOTH_ADVERTISE permission on Android 12+ (but don't block if missing)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            log("⚠️  No BLUETOOTH_ADVERTISE permission, but continuing anyway (advertising might not work)");
            // Don't return - try to start advertising anyway and let it fail gracefully
        }
        if (!::advertiser.isInitialized || advertiser == null) { log("!!! Advertiser not available."); return }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true).setTimeout(0).build()

        val dataBuilder = AdvertiseData.Builder().setIncludeDeviceName(false)
        val keyBytes = secretKey.toByteArray(Charsets.UTF_8)
        if (keyBytes.isNotEmpty()) {
            try { dataBuilder.addManufacturerData(companyId, keyBytes)
                log("Incluindo Manufacturer Data: ID=0x${companyId.toString(16).uppercase(Locale.ROOT)}, Key='${secretKey}'")
            } catch (e: IllegalArgumentException) { log("!!! Erro Manufacturer Data: ${e.message}"); updateForegroundNotification("ERRO: Manuf. Data Inválido!"); }
        } else { log("Manufacturer Data Key vazia.") }

        val data: AdvertiseData = try { dataBuilder.build() } catch (e: Exception) { log("!!! Erro ao construir AdvertiseData: ${e.message}"); return }

        try {
            log("Enviando comando para iniciar advertising...")
            advertiser.startAdvertising(settings, data, mAdvertiseCallback)
        }
        catch (e: SecurityException) { isAdvertising = false; log("!!! SecurityException in startAdvertising.") }
        catch (e: Exception) { isAdvertising = false; log("!!! GENERAL EXCEPTION in startAdvertising: ${e.message}") }
    }

    private fun stopAdvertising() {
        mainHandler.removeCallbacksAndMessages(null) // Cancel any pending retries
        if (!::advertiser.isInitialized || advertiser == null) {
            log("!!! Advertiser not available to stop.")
            isAdvertising = false;
            return
        }
        try {
            log("Stopping advertising...")
            advertiser.stopAdvertising(mAdvertiseCallback)
            log("Stop advertising command sent.")
        } catch (e: Exception) {
            log("!!! EXCEPTION in stopAdvertising: ${e.message}")
        }
        finally {
            isAdvertising = false; // Set state as stopped
            updateForegroundNotification("Service active. Advertising stopped.")
        }
    }

    // Receiver (no changes)
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_START_ADVERTISING -> {
                    val companyId = intent.getIntExtra(EXTRA_COMPANY_ID, DEFAULT_COMPANY_ID)
                    val secretKey = intent.getStringExtra(EXTRA_SECRET_KEY) ?: DEFAULT_SECRET_KEY
                    log("Comando START_ADVERTISING recebido (pode ser redundante).")
                    startAdvertising(companyId, secretKey) // Try to start (harmless if already active)
                }
                ACTION_STOP_ADVERTISING -> {
                    log("Comando STOP_ADVERTISING recebido.")
                    stopAdvertising()
                }
            }
        }
    }

    // --- Received Data Processing ---
    private fun processReceivedData(deviceAddress: String, data: ByteArray): Boolean {
        try {
            var jsonString: String

            // Try to decompress if GZIP
            if (isGzipped(data)) {
                log("Compressed data detected (${data.size} bytes), decompressing...")
                jsonString = decompressGzip(data)
                log("Decompressed data: ${jsonString.length} characters")
            } else {
                jsonString = data.toString(Charsets.UTF_8)
                log("Uncompressed data: ${jsonString.length} characters")
            }

            // Process as JSONL (lines separated by \n)
            val lines = jsonString.trim().split("\n").filter { it.isNotBlank() }
            log("Processing ${lines.size} JSONL line(s)")

            var totalSaved = 0
            for (line in lines) {
                try {
                    val jsonObj = JSONObject(line.trim())
                    val rideIdFromJson = jsonObj.optLong("ride_id", -1L)

                    if (rideIdFromJson <= 0) {
                        log("!!! JSON without valid 'ride_id' in line: $line")
                        continue
                    }

                    if (!dbHelper.ensureRideExists(rideIdFromJson)) {
                        log("!!! Failed to ensure/create ride ID $rideIdFromJson")
                        continue
                    }

                    val power = jsonObj.optDouble("power", Double.NaN)
                    val lat = jsonObj.optDouble("latitude", Double.NaN)
                    val lon = jsonObj.optDouble("longitude", Double.NaN)
                    val alt = jsonObj.optDouble("altitude", Double.NaN)
                    val vel = jsonObj.optDouble("velocity", Double.NaN)
                    val cad = jsonObj.optDouble("cadence", Double.NaN)

                    var savedCount = 0
                    if (power.isFinite()) { if (dbHelper.insertPower(rideIdFromJson, power.toFloat())) savedCount++ }
                    if (lat.isFinite() && lon.isFinite()) {
                        val altF = if (alt.isFinite()) alt.toFloat() else null
                        if (dbHelper.insertMapData(rideIdFromJson, lat.toFloat(), lon.toFloat(), altF)) savedCount++
                    }
                    if (vel.isFinite()) { if (dbHelper.insertVelocity(rideIdFromJson, vel.toFloat())) savedCount++ }
                    if (cad.isFinite()) { if (dbHelper.insertCadence(rideIdFromJson, cad.toFloat())) savedCount++ }

                    if (savedCount > 0) {
                        totalSaved += savedCount
                    }

                } catch (e: Exception) {
                    log("!!! Error processing JSON line: ${e.message} - Line: $line")
                }
            }

            log("Processing completed: $totalSaved data point(s) saved from ${lines.size} line(s)")
            return true

        } catch (e: Exception) {
            log("!!! General error in data processing: ${e.message}")
            return false
        }
    }

    private fun isGzipped(data: ByteArray): Boolean {
        // Check if first bytes indicate GZIP compression
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }

    private fun decompressGzip(data: ByteArray): String {
        try {
            // Skip GZIP header (10 bytes) and use InflaterInputStream
            val inflaterStream = InflaterInputStream(ByteArrayInputStream(data, 10, data.size - 10))
            return inflaterStream.bufferedReader(Charsets.UTF_8).use { reader: java.io.BufferedReader -> reader.readText() }
        } catch (e: Exception) {
            log("GZIP decompression error: ${e.message}")
            throw e
        }
    }

    // --- BLE Callbacks ---
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            mainHandler.post {
                val statusText = BluetoothUtils.gattStatusToString(status)
                val stateText = BluetoothUtils.connectionStateToString(newState)
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }
                val deviceName = try { if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) device.name ?: "No name" else "Perm. denied" } catch (ignore: Exception) { "?" }
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    log("Device connected: $deviceAddress ($deviceName) - Status: $statusText")
                    connectedDevices.add(device)
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    log("Device disconnected: $deviceAddress - Status: $statusText")
                    connectedDevices.remove(device)
                    // Clear buffer for disconnected device
                    receivedDataBuffers.remove(deviceAddress)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            mainHandler.post {
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    log("!!! No CONNECT permission in Write Request for $deviceAddress.")
                    if (responseNeeded) { try { bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED, 0, null) } catch (ignore: Exception) {} }
                    return@post
                }
                var gattStatus = BluetoothGatt.GATT_SUCCESS
                if (characteristic.uuid == GattProfile.UUID_CHAR_DATA) {
                    try {
                        val deviceKey = deviceAddress

                        // Adiciona dados ao buffer
                        val buffer = receivedDataBuffers.getOrPut(deviceKey) { ByteArrayOutputStream() }
                        buffer.write(value)

                        // Tenta processar os dados (verifica se é o fim dos dados)
                        val processed = processReceivedData(deviceAddress, buffer.toByteArray())

                        if (processed) {
                            // Dados processados com sucesso, limpa o buffer
                            receivedDataBuffers.remove(deviceKey)
                            log("Dados processados e buffer limpo para $deviceAddress")
                        } else {
                            // Ainda aguardando mais dados
                            log("Aguardando mais dados de $deviceAddress (${buffer.size()} bytes recebidos)")
                        }

                    } catch (e: Exception) {
                        log("!!! EXCEPTION in data processing: ${e.message}")
                        // Clear buffer on error
                        receivedDataBuffers.remove(deviceAddress)
                        gattStatus = BluetoothGatt.GATT_FAILURE
                    }
                } else {
                    log("Write to unknown UUID.")
                    gattStatus = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
                }
                if (responseNeeded) { sendGattResponse(device, requestId, gattStatus) }
            }
        }

        override fun onDescriptorWriteRequest( device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray ) {
            val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }
            mainHandler.post {
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { /*...*/ return@post }
                if (descriptor.uuid == GattProfile.CLIENT_CHARACTERISTIC_CONFIG) { log("Write to CCCD from $deviceAddress."); if (responseNeeded) sendGattResponse(device, requestId, BluetoothGatt.GATT_SUCCESS) }
                else { log("WARNING: Write to descriptor ${descriptor.uuid}"); if (responseNeeded) sendGattResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) }
            }
        }
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) { mainHandler.post { log("Serviço ${service?.uuid} adicionado: ${BluetoothUtils.gattStatusToString(status)}") } }
        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) { mainHandler.post { val addr = try{device?.address}catch(ignore:Exception){"?"}; log("MTU alterado para $mtu com $addr") } }
    } // Fim gattServerCallback

    // --- Métodos Helper ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Serviço BLE em execução"; setShowBadge(false) }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                try { manager.createNotificationChannel(channel); log("Canal de notificação criado.") }
                catch (e: Exception) { log("!!! Erro ao criar canal: ${e.message}") }
            } else { log("Canal de notificação já existe.") }
        }
    }

    private fun updateForegroundNotification(contentText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão POST_NOTIFICATIONS para atualizar."); return
        }
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Servidor BLE Ativo").setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try { manager.notify(NOTIFICATION_ID, notification) }
        catch (e: Exception) { Log.e(TAG, "Erro ao atualizar notificação: ${e.message}") }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão POST_NOTIFICATIONS para iniciar Foreground.")
        }
        val initialNotification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Servidor BLE Ativo").setContentText("Inicializando...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var foregroundType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                // Add Bluetooth foreground type for Android 14+ if available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        val bluetoothType = ServiceInfo::class.java.getField("FOREGROUND_SERVICE_TYPE_BLUETOOTH").getInt(null)
                        foregroundType = foregroundType or bluetoothType
                    } catch (e: Exception) {
                        // FOREGROUND_SERVICE_TYPE_BLUETOOTH not available, use only CONNECTED_DEVICE
                    }
                }

                // Adicionar tipos futuros (Android 15+) dinamicamente, se disponíveis
                val futureTypes = listOf("FOREGROUND_SERVICE_TYPE_BLE", "FOREGROUND_SERVICE_TYPE_BLUETOOTH_ADVANCED")
                for (type in futureTypes) {
                    try {
                        val typeValue = ServiceInfo::class.java.getField(type).getInt(null)
                        foregroundType = foregroundType or typeValue
                    } catch (e: Exception) {
                        // Tipo não disponível, ignorar
                    }
                }
                startForeground(NOTIFICATION_ID, initialNotification, foregroundType)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            log("Serviço iniciado em primeiro plano.")
            updateForegroundNotification("Aguardando conexões e comandos.")
        } catch (e: Exception) {
            log("!!! Erro ao iniciar startForeground: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { stopSelf() } // Para se falhar no Android 12+
        }
    }

    private fun sendGattResponse(device: BluetoothDevice, requestId: Int, status: Int) {
        val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. negada" }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! PERMISSÃO CONNECT FALTANTE ao enviar resposta GATT para $deviceAddress."); return
        }
        if (bluetoothGattServer == null) { log("!!! Servidor GATT nulo ao enviar resposta para $deviceAddress."); return }
        try {
            val statusString = BluetoothUtils.gattStatusToString(status)
            log("Enviando resposta GATT ($statusString) para $deviceAddress, Req ID: $requestId")
            bluetoothGattServer?.sendResponse(device, requestId, status, 0, null)
        } catch (e: Exception) { log("!!! EXCEÇÃO ao enviar resposta GATT para $deviceAddress: ${e.message}") }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        val intent = Intent(ACTION_LOG_UPDATE).apply { putExtra(EXTRA_LOG_MESSAGE, message) }
        sendBroadcast(intent)
    }

    private fun advertiseErrorToString(errorCode: Int): String {
        return when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ALREADY_STARTED"; AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "DATA_TOO_LARGE"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED"; AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "TOO_MANY_ADVERTISERS"; else -> "Erro desconhecido $errorCode"
        }
    }
}

// Classe BluetoothUtils (sem alterações)
object BluetoothUtils {
    fun gattStatusToString(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "SUCCESS(0)"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "READ_NOT_PERMITTED(2)"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "WRITE_NOT_PERMITTED(3)"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "INSUFFICIENT_AUTHENTICATION(5)"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "REQUEST_NOT_SUPPORTED(6)"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "INSUFFICIENT_ENCRYPTION(15)"
            BluetoothGatt.GATT_INVALID_OFFSET -> "INVALID_OFFSET(7)"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "INVALID_ATTRIBUTE_LENGTH(13)"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "CONNECTION_CONGESTED(143)"
            BluetoothGatt.GATT_FAILURE -> "FAILURE(257)"
            133 -> "GATT_ERROR(133)"
            8 -> "INSUFFICIENT_AUTHORIZATION(8)" // Código numérico
            else -> "Status GATT $status"
        }
    }
    fun connectionStateToString(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"; BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"; BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "Estado $state"
        }
    }
}
