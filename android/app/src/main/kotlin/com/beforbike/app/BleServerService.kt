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
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
// Adicionada importação para o Executor
import java.util.concurrent.Executors

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

    /**
     * Um thread pool dedicado (com um único thread) para processar dados recebidos
     * (GZIP, JSON, e escrita no DB) fora da thread principal, evitando ANRs.
     */
    private val dataProcessingExecutor = Executors.newSingleThreadExecutor()

    // --- Constants ---
    companion object {
        private const val TAG = "BleServerService"
        const val ACTION_LOG_UPDATE = "com.beforbike.app.LOG_UPDATE"
        const val EXTRA_LOG_MESSAGE = "EXTRA_LOG_MESSAGE"
        const val ACTION_START_ADVERTISING = "com.beforbike.app.START_ADVERTISING"
        const val ACTION_STOP_ADVERTISING = "com.beforbike.app.STOP_ADVERTISING"
        const val ACTION_SEND_DATA = "com.beforbike.app.SEND_DATA"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_COMPANY_ID = "EXTRA_COMPANY_ID"
        const val EXTRA_SECRET_KEY = "EXTRA_SECRET_KEY"
        internal const val DEFAULT_COMPANY_ID = 0xF0F0
        internal const val DEFAULT_SECRET_KEY = "Oficinas3"
        private const val ADVERTISING_RETRY_DELAY = 5000L
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "ble_server_channel_v1"
        private const val NOTIFICATION_CHANNEL_NAME = "BLE Server Service"

        // Constante para o atraso de reinício do advertising
        private const val ADVERTISING_RESTART_DELAY_MS = 500L

        // <<< MUDANÇA: Flag estática para o estado do serviço
        @JvmStatic var isServiceRunning = false
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
        // <<< MUDANÇA: Atualiza a flag estática
        isServiceRunning = true

        log("Service creating...")
        dbHelper = com.beforbike.app.database.RideDbHelper(this)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) { log("!!! Bluetooth disabled."); stopSelf(); return }
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) { log("!!! Warning: Multiple advertising may not be supported.") }
        try { advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run { log("!!! Critical Error: Advertiser null."); stopSelf(); return }
        } catch (e: Exception) { log("!!! Error getting Advertiser: ${e.message}."); stopSelf(); return }

        createNotificationChannel()
        val intentFilter = IntentFilter().apply { addAction(ACTION_START_ADVERTISING); addAction(ACTION_STOP_ADVERTISING); addAction(ACTION_SEND_DATA) }
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()
        // <<< MUDANÇA: Atualiza a flag estática
        isServiceRunning = false

        log("Service destroying (onDestroy)...")
        mainHandler.removeCallbacksAndMessages(null) // Remove advertising retries
        stopAdvertising() // Ensure advertising stops
        stopServer()
        try { unregisterReceiver(commandReceiver) } catch (ignore: Exception) {}
        dbHelper.close()
        // Desliga o executor
        try {
            dataProcessingExecutor.shutdown()
        } catch (e: Exception) {
            log("!!! Erro ao desligar dataProcessingExecutor: ${e.message}")
        }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! No BLUETOOTH_CONNECT permission for setupGattService.")
            return
        }
        bluetoothGattServer?.clearServices()
        val service = BluetoothGattService(GattProfile.UUID_SERVICE_TRANSFER, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val dataCharacteristic = BluetoothGattCharacteristic(
            GattProfile.UUID_CHAR_DATA,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        // Add CCCD descriptor for notifications
        val cccd = BluetoothGattDescriptor(
            GattProfile.CLIENT_CHARACTERISTIC_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        dataCharacteristic.addDescriptor(cccd)
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
        val isAndroid12OrHigher = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        val hasAdvertisePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        if (isAndroid12OrHigher && !hasAdvertisePermission) {
            log("⚠️  No BLUETOOTH_ADVERTISE permission, but continuing anyway (advertising might not work)");
            // Don't return - try to start advertising anyway and let it fail gracefully
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true).setTimeout(0).build()

        val dataBuilder = AdvertiseData.Builder().setIncludeDeviceName(false)
        val keyBytes = secretKey.toByteArray(Charsets.UTF_8)
        if (keyBytes.isNotEmpty()) {
            try { dataBuilder.addManufacturerData(companyId, keyBytes)
                log("Incluindo Manufacturer Data: ID=0x${companyId.toString(16).uppercase(Locale.ROOT)}, Key='${secretKey}'")
            } catch (e: IllegalArgumentException) { log("!!! Erro Manufacturer Data: ${e.message}"); updateForegroundNotification("ERROR: Invalid Manufacturer Data!"); }
        } else { log("Manufacturer Data Key vazia.") }

        val data: AdvertiseData = try { dataBuilder.build() } catch (e: Exception) { log("!!! Erro ao construir AdvertiseData: ${e.message}"); return }

        try {
            log("Enviando comando para iniciar advertising...")
            advertiser.startAdvertising(settings, data, mAdvertiseCallback)
        }
        catch (e: SecurityException) { isAdvertising = false; log("!!! SecurityException in startAdvertising.") }
        catch (e: Exception) { isAdvertising = false; log("!!! GENERAL EXCEPTION in startAdvertising: ${e.message}") }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopAdvertising() {
        mainHandler.removeCallbacksAndMessages(null) // Cancel any pending retries
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
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
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
                ACTION_SEND_DATA -> {
                    val data = intent.getByteArrayExtra(EXTRA_DATA)
                    if (data != null) {
                        log("Comando SEND_DATA recebido (${data.size} bytes).")
                        sendData(data)
                    } else {
                        log("!!! Comando SEND_DATA recebido sem dados.")
                    }
                }
            }
        }
    }

    // --- Received Data Processing ---
    // Esta função agora é chamada a partir de um thread de segundo plano
    private fun processReceivedData(deviceAddress: String, data: ByteArray): Boolean {
        try {
            var jsonString: String

            // 1. Descomprimir (lógica Gzip/descompressão permanece a mesma)
            if (isGzipped(data)) {
                log("Compressed data detected (${data.size} bytes), decompressing...")
                jsonString = decompressGzip(data)
                log("Decompressed data: ${jsonString.length} characters")
            } else {
                jsonString = data.toString(Charsets.UTF_8)
                log("Uncompressed data: ${jsonString.length} characters")
            }

            // 2. Carrega o Array de strings (JSON Array)
            val jsonArray = JSONArray(jsonString.trim())
            log("Processing ${jsonArray.length()} items from received JSON Array")

            var totalSaved = 0
            var rideIdFromJson = -1L // Para rastrear o rideId do lote

            for (i in 0 until jsonArray.length()) {
                try {
                    // 'line' é a string: {"info":{...}, "gps":{...}, "crank":{...}}
                    val line = jsonArray.getString(i)
                    val jsonObj = JSONObject(line.trim()) // Carrega o objeto principal

                    // 3. Extrai os objetos aninhados do JSON
                    val infoObj = jsonObj.optJSONObject("info")
                    val gpsObj = jsonObj.optJSONObject("gps")
                    val crankObj = jsonObj.optJSONObject("crank") // Pode ser nulo

                    // 4. Validação essencial (info e gps são obrigatórios)
                    if (infoObj == null || gpsObj == null) {
                        log("!!! JSON item missing 'info' or 'gps' object. Skipping item $i")
                        continue
                    }

                    // 5. Pega o ride_id (assumindo que está em 'info')
                    rideIdFromJson = infoObj.optLong("ride_id", -1L) // (Sua suposição)
                    if (rideIdFromJson <= 0) {
                        log("!!! JSON 'info' object without valid 'ride_id' in item: $line")
                        continue
                    }

                    // 6. Garante que a "Corrida" (Tabela 1) exista
                    // Pega a data/hora do pacote para usar como 'start_time' da corrida
                    val startTime = infoObj.optString("date") + " " + infoObj.optString("time")
                    if (!dbHelper.ensureRideExists(rideIdFromJson, startTime)) {
                        log("!!! Failed to ensure/create ride ID $rideIdFromJson")
                        continue // Falha crítica no DB, pula este item
                    }

                    // 7. Converte os JSONObjects em Maps (para o DbHelper)
                    val infoMap = infoObj.toMap()
                    val gpsMap = gpsObj.toMap()
                    val crankMap = crankObj?.toMap() // Será null se crankObj for nulo

                    // 8. Chama a NOVA função de inserção unificada (Tabela 2)
                    val success = dbHelper.insertTelemetryData(
                        rideId = rideIdFromJson,
                        infoMap = infoMap,
                        gpsMap = gpsMap,
                        crankMap = crankMap
                    )

                    if (success) {
                        totalSaved++
                    }

                } catch (e: Exception) {
                    log("!!! Error processing JSON item from array: ${e.message} - Item index: $i")
                }
            }

            log("Processing completed: $totalSaved data point(s) saved from ${jsonArray.length()} item(s)")

            // (Opcional) Chamar o cálculo de estatísticas aqui se for o fim
            // if (totalSaved > 0 && rideIdFromJson > 0) {
            //     val stats = dbHelper.calculateRideStatistics(rideIdFromJson)
            //     if (stats != null) dbHelper.updateRideSummary(rideIdFromJson, stats)
            // }

            // Retorna 'true' se o processamento foi concluído (mesmo que alguns itens tenham falhado)
            return true

        } catch (e: Exception) {
            log("!!! General error in data processing (is data a valid JSONArray?): ${e.message}")
            // Retorna 'false' se a estrutura inteira falhou (ex: não é um JSONArray)
            // Isso fará com que o buffer NÃO seja limpo e espere por mais dados.
            return false
        }
    }

    private fun isGzipped(data: ByteArray): Boolean {
        // Check if first bytes indicate GZIP compression
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }

    private fun decompressGzip(data: ByteArray): String {
        try {
            // USA GZIPInputStream (em vez de InflaterInputStream)
            val gzipStream = GZIPInputStream(ByteArrayInputStream(data))

            return gzipStream.bufferedReader(Charsets.UTF_8).use { reader: java.io.BufferedReader -> reader.readText() }

        } catch (e: Exception) {
            log("GZIP decompression error: ${e.message}")
            throw e
        }
    }

    // --- BLE Callbacks ---
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        /**
         * LÓGICA CORRIGIDA para 'onConnectionStateChange'
         * Esta função agora lida corretamente com o início/parada do advertising
         * para evitar a "condição de corrida".
         */
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            mainHandler.post {
                // --- Logging ---
                val statusText = BluetoothUtils.gattStatusToString(status)
                val stateText = BluetoothUtils.connectionStateToString(newState)
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }
                val deviceName = try {
                    if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                        device.name ?: "No name"
                    else "Perm. denied"
                } catch (ignore: Exception) { "?" }
                // --- Fim Logging ---


                // --- LÓGICA CORRIGIDA ---
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // A. ESTADO: CONECTADO
                    log("Device connected: $deviceAddress ($deviceName) - Status: $statusText")
                    connectedDevices.add(device)

                    // Pare o advertising
                    if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                        stopAdvertising()
                    } else {
                        log("!!! Não pode parar o advertising (sem permissão)")
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // B. ESTADO: DESCONECTADO
                    log("Device disconnected: $deviceAddress - Status: $statusText")
                    connectedDevices.remove(device)
                    receivedDataBuffers.remove(deviceAddress)

                    // Reinicie o advertising (COM O ATRASO)
                    if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {

                        // Use o atraso para evitar a "race condition"
                        mainHandler.postDelayed({
                            // Verifique o estado NOVAMENTE, caso algo tenha mudado
                            if (!isAdvertising && bluetoothGattServer != null) {
                                log("Reiniciando advertising (após atraso) para aceitar novas conexões...")
                                startAdvertising(currentCompanyId, currentSecretKey)
                            } else {
                                log("Não foi necessário reiniciar o advertising (já ativo ou servidor parado).")
                            }
                        }, ADVERTISING_RESTART_DELAY_MS) // Usa a constante

                    } else {
                        log("!!! Não pode reiniciar o advertising (sem permissão)")
                    }
                }
                // --- FIM DA LÓGICA CORRIGIDA ---
            }
        }

        /**
         * LÓGICA CORRIGIDA para 'onCharacteristicWriteRequest' (NÃO-BLOQUEANTE)
         * Esta função agora move o trabalho pesado (DB) para um
         * thread em segundo plano, mantendo a thread principal livre.
         */
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            mainHandler.post {
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }

                // 1. Validação de permissão (na main thread)
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    log("!!! No CONNECT permission in Write Request for $deviceAddress.")
                    if (responseNeeded) { sendGattResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED) }
                    return@post
                }

                if (characteristic.uuid == GattProfile.UUID_CHAR_DATA) {
                    try {
                        // 2. Adiciona dados ao buffer (na main thread - rápido)
                        val deviceKey = deviceAddress
                        val buffer = receivedDataBuffers.getOrPut(deviceKey) { ByteArrayOutputStream() }
                        buffer.write(value)

                        // Copia os dados para enviar ao outro thread
                        val dataToProcess = buffer.toByteArray()

                        // 3. Envia o trabalho pesado (processReceivedData) para o thread de processamento
                        dataProcessingExecutor.submit {
                            var processingSuccess = false
                            var gattStatus = BluetoothGatt.GATT_FAILURE

                            try {
                                // 4. Isso agora roda em SEGUNDO PLANO
                                processingSuccess = processReceivedData(deviceAddress, dataToProcess)

                                if (processingSuccess) {
                                    // Deu certo, limpa o buffer
                                    mainHandler.post { receivedDataBuffers.remove(deviceKey) }
                                    log("Dados processados e buffer limpo para $deviceAddress")
                                    gattStatus = BluetoothGatt.GATT_SUCCESS
                                } else {
                                    // Não deu certo (provavelmente dados parciais)
                                    log("Aguardando mais dados de $deviceAddress (${dataToProcess.size} bytes recebidos)")
                                    // Se NÃO foi sucesso, não limpa o buffer
                                    // Responde SUCESSO mesmo assim para o cliente
                                    gattStatus = BluetoothGatt.GATT_SUCCESS
                                }

                            } catch (e: Exception) {
                                log("!!! EXCEPTION in data processing (background): ${e.message}")
                                // Se deu erro, limpa o buffer
                                mainHandler.post { receivedDataBuffers.remove(deviceKey) }
                                gattStatus = BluetoothGatt.GATT_FAILURE

                            } finally {
                                // 5. Responde ao cliente DEPOIS de processar
                                if (responseNeeded) {
                                    mainHandler.post { // A resposta DEVE ser na main thread
                                        sendGattResponse(device, requestId, gattStatus)
                                    }
                                }
                            }
                        } // Fim do dataProcessingExecutor.submit

                    } catch (e: Exception) {
                        log("!!! EXCEPTION in write request (main): ${e.message}")
                        receivedDataBuffers.remove(deviceAddress)
                        if (responseNeeded) { sendGattResponse(device, requestId, BluetoothGatt.GATT_FAILURE) }
                    }
                } else {
                    log("Write to unknown UUID.")
                    if (responseNeeded) { sendGattResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) }
                }
            }
        } // Fim do onCharacteristicWriteRequest

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
            .setContentTitle("BLE Server Active").setContentText(contentText)
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
            .setContentTitle("BLE Server Active").setContentText("Initializing...")
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
            updateForegroundNotification("Waiting for connections and commands.")
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

    // Function to send data to all connected devices via BLE notification
    fun sendData(data: ByteArray) {
        if (bluetoothGattServer == null) {
            log("!!! Cannot send data: GATT server not initialized")
            return
        }

        if (connectedDevices.isEmpty()) {
            log("No connected devices to send data to")
            return
        }

        val service = bluetoothGattServer?.getService(GattProfile.UUID_SERVICE_TRANSFER)
        val characteristic = service?.getCharacteristic(GattProfile.UUID_CHAR_DATA)

        if (characteristic == null) {
            log("!!! Cannot send data: characteristic not found")
            return
        }

        for (device in connectedDevices) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    log("⚠️  Cannot send data: BLUETOOTH_CONNECT permission not granted")
                    continue
                }
                // Set characteristic value before notifying (API level 24+ compatible)
                characteristic.value = data
                val success = bluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. denied" }
                log("Data sent to $deviceAddress: ${success ?: "null"} (${data.size} bytes)")
            } catch (e: Exception) {
                log("!!! Error sending data to device: ${e.message}")
            }
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
// [No arquivo BleServerService.kt, cole estas funções]

/**
 * Converte um JSONObject em um Map<String, Any?>.
 */
fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    val keysItr = this.keys()
    while (keysItr.hasNext()) {
        val key = keysItr.next()
        val value: Any? = this.get(key)
        val processedValue = when (value) {
            is JSONArray -> value.toList()
            is JSONObject -> value.toMap()
            JSONObject.NULL -> null
            else -> value
        }
        map[key] = processedValue
    }
    return map
}

/**
 * Converte um JSONArray em um List<Any?>.
 */
fun JSONArray.toList(): List<Any?> {
    val list = mutableListOf<Any?>()
    for (i in 0 until this.length()) {
        var value = this.get(i)
        when (value) {
            is JSONArray -> value = value.toList()
            is JSONObject -> value = value.toMap()
            JSONObject.NULL -> value = null
        }
        list.add(value)
    }
    return list
}