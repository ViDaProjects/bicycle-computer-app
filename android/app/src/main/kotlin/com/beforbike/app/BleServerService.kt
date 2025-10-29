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

class BleServerService : Service() {

    // --- Propriedades ---
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGattServer: BluetoothGattServer? = null
    private lateinit var advertiser: BluetoothLeAdvertiser
    private val mainHandler = Handler(Looper.getMainLooper())
    private val connectedDevices = mutableSetOf<BluetoothDevice>()
    private lateinit var dbHelper: com.beforbike.app.database.RideDbHelper
    private var isAdvertising = false
    private var currentCompanyId: Int = DEFAULT_COMPANY_ID
    private var currentSecretKey: String = DEFAULT_SECRET_KEY

    // --- Constantes ---
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
            log("✓ Advertising iniciado com sucesso!")
            updateForegroundNotification("Advertising ativo.")
            mainHandler.removeCallbacksAndMessages(null)
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            val errorString = advertiseErrorToString(errorCode)
            log("!!! FALHA advertising: $errorCode ($errorString)")
            updateForegroundNotification("Falha Advertising ($errorString).")

            if(errorCode != AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
                mainHandler.postDelayed({
                    if (!isAdvertising) startAdvertising(currentCompanyId, currentSecretKey)
                }, ADVERTISING_RETRY_DELAY)
            }
        }
    }

    // --- Ciclo de Vida do Serviço ---
    override fun onCreate() {
        super.onCreate()
        log("Serviço criando...")
        dbHelper = com.beforbike.app.database.RideDbHelper(this)
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) { log("!!! Bluetooth desabilitado."); stopSelf(); return }
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) { log("!!! Aviso: Advertising múltiplo pode não ser suportado.") }
        try { advertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: run { log("!!! Erro Crítico: Advertiser nulo."); stopSelf(); return }
        } catch (e: Exception) { log("!!! Erro ao obter Advertiser: ${e.message}."); stopSelf(); return }

        createNotificationChannel()
        val intentFilter = IntentFilter().apply { addAction(ACTION_START_ADVERTISING); addAction(ACTION_STOP_ADVERTISING) }
        ContextCompat.registerReceiver(this, commandReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        log("Receiver de advertising registrado.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("Serviço BLE iniciado (onStartCommand).")
        startForegroundNotification()
        startServer()
        if (!isAdvertising) {
            log("Tentando iniciar advertising automaticamente...")
            val companyId = intent?.getIntExtra(EXTRA_COMPANY_ID, DEFAULT_COMPANY_ID) ?: DEFAULT_COMPANY_ID
            val secretKey = intent?.getStringExtra(EXTRA_SECRET_KEY) ?: DEFAULT_SECRET_KEY
            startAdvertising(companyId, secretKey)
        } else { log("Advertising já estava ativo.") }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Serviço destruindo (onDestroy)...")
        mainHandler.removeCallbacksAndMessages(null) // Remove retentativas de advertising
        stopAdvertising() // Garante que o advertising pare
        stopServer()
        try { unregisterReceiver(commandReceiver) } catch (ignore: Exception) {}
        dbHelper.close()
        log("Serviço BLE parado.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Lógica Principal do Servidor BLE ---
    private fun startServer() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão BLUETOOTH_CONNECT para startServer."); return
        }
        if (bluetoothGattServer == null) {
            bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)
            if (bluetoothGattServer == null) { log("!!! Falha Crítica: openGattServer retornou nulo."); stopSelf(); return }
            log("Servidor GATT aberto.")
            setupGattService()
        } else { log("Servidor GATT já estava aberto.") }
    }

    private fun setupGattService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão BLUETOOTH_CONNECT para setupGattService."); return
        }
        if (bluetoothGattServer == null) { log("!!! setupGattService chamado sem servidor GATT."); return }
        bluetoothGattServer?.clearServices()
        val service = BluetoothGattService(GattProfile.UUID_SERVICE_TRANSFER, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val dataCharacteristic = BluetoothGattCharacteristic(
            GattProfile.UUID_CHAR_DATA,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(dataCharacteristic)
        val added = bluetoothGattServer?.addService(service)
        log(if (added == true) "Serviço GATT (${service.uuid}) adicionado." else "!!! Falha ao adicionar serviço GATT.")
    }

    private fun stopServer() {
        if (bluetoothGattServer == null) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão BLUETOOTH_CONNECT para stopServer. Não é possível desconectar clientes ou fechar o servidor.")
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
            log("Servidor GATT fechado.")
        } catch (e: SecurityException) {
            log("!!! SecurityException ao parar servidor/desconectar clientes: ${e.message}")
        } catch (e: Exception) {
            log("!!! Exceção ao fechar servidor GATT: ${e.message}")
        } finally {
            bluetoothGattServer = null
        }
    }

    // --- startAdvertising ---
    private fun startAdvertising(companyId: Int, secretKey: String) {
        if (isAdvertising) { log("Advertising já ativo."); return }
        mainHandler.removeCallbacksAndMessages(null) // Limpa retentativas anteriores
        currentCompanyId = companyId
        currentSecretKey = secretKey
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            log("!!! Sem permissão BLUETOOTH_ADVERTISE."); return
        }
        if (!::advertiser.isInitialized || advertiser == null) { log("!!! Advertiser não disponível."); return }

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
        catch (e: SecurityException) { isAdvertising = false; log("!!! SecurityException em startAdvertising.") }
        catch (e: Exception) { isAdvertising = false; log("!!! EXCEÇÃO GERAL em startAdvertising: ${e.message}") }
    }

    private fun stopAdvertising() {
        mainHandler.removeCallbacksAndMessages(null) // Cancela quaisquer retentativas pendentes
        if (!::advertiser.isInitialized || advertiser == null) {
            log("!!! Advertiser não disponível para parar.")
            isAdvertising = false;
            return
        }
        try {
            log("Parando advertising...")
            advertiser.stopAdvertising(mAdvertiseCallback)
            log("Comando para parar advertising enviado.")
        } catch (e: Exception) {
            log("!!! EXCEÇÃO em stopAdvertising: ${e.message}")
        }
        finally {
            isAdvertising = false; // Define o estado como parado
            updateForegroundNotification("Serviço ativo. Advertising parado.")
        }
    }

    // Receiver (sem alterações)
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_START_ADVERTISING -> {
                    val companyId = intent.getIntExtra(EXTRA_COMPANY_ID, DEFAULT_COMPANY_ID)
                    val secretKey = intent.getStringExtra(EXTRA_SECRET_KEY) ?: DEFAULT_SECRET_KEY
                    log("Comando START_ADVERTISING recebido (pode ser redundante).")
                    startAdvertising(companyId, secretKey) // Tenta iniciar (não faz mal se já estiver ativo)
                }
                ACTION_STOP_ADVERTISING -> {
                    log("Comando STOP_ADVERTISING recebido.")
                    stopAdvertising()
                }
            }
        }
    }

    // --- Callbacks BLE ---
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            mainHandler.post {
                val statusText = BluetoothUtils.gattStatusToString(status)
                val stateText = BluetoothUtils.connectionStateToString(newState)
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. negada" }
                val deviceName = try { if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) device.name ?: "Sem nome" else "Perm. negada" } catch (ignore: Exception) { "?" }
                if (newState == BluetoothProfile.STATE_CONNECTED) { log("Dispositivo conectado: $deviceAddress ($deviceName) - Status: $statusText"); connectedDevices.add(device) }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) { log("Dispositivo desconectado: $deviceAddress - Status: $statusText"); connectedDevices.remove(device) }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            mainHandler.post {
                val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. negada" }
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    log("!!! Sem permissão CONNECT em Write Request para $deviceAddress.")
                    if (responseNeeded) { try { bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_WRITE_NOT_PERMITTED, 0, null) } catch (ignore: Exception) {} }
                    return@post
                }
                var gattStatus = BluetoothGatt.GATT_SUCCESS
                if (characteristic.uuid == GattProfile.UUID_CHAR_DATA) {
                    try {
                        val jsonString = value.toString(Charsets.UTF_8)
                        log("JSON de $deviceAddress (${value.size} bytes): $jsonString")
                        val jsonObj = JSONObject(jsonString)
                        val rideIdFromJson = jsonObj.optLong("ride_id", -1L)
                        if (rideIdFromJson <= 0) { log("!!! JSON sem 'ride_id' válido."); gattStatus = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH }
                        else {
                            if (!dbHelper.ensureRideExists(rideIdFromJson)) { log("!!! Falha ao garantir/criar ride ID $rideIdFromJson."); gattStatus = BluetoothGatt.GATT_FAILURE }
                            else {
                                log("Proc. dados p/ ride ID $rideIdFromJson...")
                                val power = jsonObj.optDouble("power", Double.NaN)
                                val lat = jsonObj.optDouble("latitude", Double.NaN)
                                val lon = jsonObj.optDouble("longitude", Double.NaN)
                                val alt = jsonObj.optDouble("altitude", Double.NaN)
                                val vel = jsonObj.optDouble("velocity", Double.NaN)
                                var savedCount = 0
                                if (power.isFinite()) { if (dbHelper.insertPower(rideIdFromJson, power.toFloat())) savedCount++ }
                                if (lat.isFinite() && lon.isFinite()) { val altF = if (alt.isFinite()) alt.toFloat() else null; if (dbHelper.insertMapData(rideIdFromJson, lat.toFloat(), lon.toFloat(), altF)) savedCount++ }
                                if (vel.isFinite()) { if (dbHelper.insertVelocity(rideIdFromJson, vel.toFloat())) savedCount++ }
                                log(if (savedCount > 0) "  => $savedCount dado(s) salvo(s)." else "  -> Nenhum dado válido.")
                            }
                        }
                    } catch (e: Exception) { log("!!! EXCEÇÃO JSON/DB: ${e.message}"); gattStatus = BluetoothGatt.GATT_FAILURE }
                } else { log("Escrita em UUID desconhecido."); gattStatus = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED }
                if (responseNeeded) { sendGattResponse(device, requestId, gattStatus) }
            } // Fim post
        }

        override fun onDescriptorWriteRequest( device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray ) {
            val deviceAddress = try { device.address } catch (ignore: SecurityException) { "Perm. negada" }
            mainHandler.post {
                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { /*...*/ return@post }
                if (descriptor.uuid == GattProfile.CLIENT_CHARACTERISTIC_CONFIG) { log("Escrita no CCCD de $deviceAddress."); if (responseNeeded) sendGattResponse(device, requestId, BluetoothGatt.GATT_SUCCESS) }
                else { log("AVISO: Escrita em descritor ${descriptor.uuid}"); if (responseNeeded) sendGattResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED) }
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
                startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
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
