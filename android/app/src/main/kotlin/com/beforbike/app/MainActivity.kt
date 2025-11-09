package com.beforbike.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.annotation.SuppressLint
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import com.beforbike.app.BleServerService // Importe o serviço
import com.beforbike.app.database.RideDbHelper
import com.beforbike.app.database.SeedData
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.beforbike.ble"

    // Variáveis para a lógica de permissão assíncrona
    private var permissionRequestResult: MethodChannel.Result? = null
    private val REQUEST_CODE_PERMISSIONS = 1 // O código de requisição

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var dbHelper: RideDbHelper
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Callback para quando o usuário responde ao pedido de permissão.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Importante

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d("BLE", "Todas as permissões foram concedidas pelo usuário.")
                // Envia o sucesso DE VOLTA para o Flutter
                this.permissionRequestResult?.success(true)
            } else {
                Log.e("BLE", "Permissões negadas pelo usuário.")
                // Envia um erro DE VOLTA para o Flutter
                this.permissionRequestResult?.error("PERMISSIONS_DENIED", "Usuário negou as permissões", null)
            }
            // Limpa o 'result' para a próxima vez
            this.permissionRequestResult = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = RideDbHelper(this)

        // Enable seed data: uncomment next line to insert sample ride for testing
        // SeedData.insertSampleRide(applicationContext)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        // <<< MUDANÇA: Inicia o serviço SOMENTE se ele não estiver rodando (usando a flag estática)
        if (bluetoothAdapter.isEnabled && !BleServerService.isServiceRunning) {
            Log.d("BLE", "Starting BLE service automatically on app launch")
            startBleServerService()
        } else if (BleServerService.isServiceRunning) {
            Log.d("BLE", "Service already running, no need to start automatically.")
        }

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, "com.beforbike.app/database").setMethodCallHandler { call, result ->
            when (call.method) {
                "getAllActivities" -> {
                    val rideIds = dbHelper.getAllRideIds()
                    val activities = mutableListOf<Map<String, Any>>()
                    for (id in rideIds) {
                        val data = dbHelper.getRideSummary(id)
                        if (data != null) {
                            val activity = mapRideToActivity(id, data)
                            activities.add(activity)
                        }
                    }
                    result.success(activities)
                }
                "getActivityLocations" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    val telemetryData = dbHelper.getRideTelemetryData(rideId)
                    val locations = telemetryData.mapNotNull { data ->
                        val timestampStr = data["gps_timestamp"] as? String
                        val lat = data["latitude"] as? Double
                        val lon = data["longitude"] as? Double
                        if (timestampStr != null && lat != null && lon != null) {
                            val timestamp = try {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                                dateFormat.parse(timestampStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            mapOf(
                                "id" to "loc_${timestampStr}_${System.currentTimeMillis()}",
                                "datetime" to timestamp,
                                "latitude" to lat,
                                "longitude" to lon
                            )
                        } else null
                    }
                    result.success(locations)
                }
                "deleteActivity" -> {
                    val activityId = call.argument<String>("activityId") ?: ""
                    val rideId = activityId.toLongOrNull() ?: 0L
                    dbHelper.deleteRide(rideId)
                    result.success(null)
                }
                "sendData" -> {
                    val dataList = call.argument<List<Int>>("data")
                    if (dataList != null) {
                        val data = ByteArray(dataList.size) { dataList[it].toByte() }
                        val intent = Intent("com.beforbike.app.SEND_DATA").apply {
                            putExtra("EXTRA_DATA", data)
                        }
                        sendBroadcast(intent)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGUMENT", "Data is null", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // <<< MUDANÇA: Lógica de permissão assíncrona
                "requestPermissions" -> {
                    // 1. Salva o 'result' para responder depois
                    this.permissionRequestResult = result
                    // 2. Chama a função que abre a caixa de diálogo
                    requestBlePermissions()
                    // NÃO chame result.success(true) aqui!
                }

                // <<< MUDANÇA: Usa a flag estática
                "isBleEnabled" -> {
                    result.success(BleServerService.isServiceRunning)
                }
                "isBluetoothAdapterEnabled" -> {
                    result.success(bluetoothAdapter.isEnabled)
                }
                "requestEnableBluetooth" -> {
                    if (!bluetoothAdapter.isEnabled) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, 1)
                        result.success(true)
                    } else {
                        result.success(false) // Already enabled
                    }
                }
                "scanAndConnectToDevice" -> {
                    scanAndConnectDevice()
                    result.success(true)
                }
                "disconnectDevice" -> {
                    disconnectDevice()
                    result.success(true)
                }
                "getConnectedStatus" -> {
                    val status = connectedDevice != null && bluetoothGatt != null
                    result.success(status)
                }
                "getConnectedDeviceName" -> {
                    val name = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        connectedDevice?.name ?: "Unknown Device"
                    } else {
                        "Permission denied"
                    }
                    result.success(name)
                }
                "getConnectedDeviceMac" -> {
                    val mac = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        connectedDevice?.address ?: ""
                    } else {
                        "Permission denied"
                    }
                    result.success(mac)
                }
                "getLocalBluetoothName" -> {
                    val name = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.name ?: "Unknown"
                    } else {
                        "Permission denied"
                    }
                    result.success(name)
                }
                "getLocalBluetoothMac" -> {
                    @SuppressLint("MissingPermission")
                    val mac = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, "android.permission.LOCAL_MAC_ADDRESS") == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.address ?: ""
                    } else {
                        "Permission denied"
                    }
                    result.success(mac)
                }

                // <<< MUDANÇA: Usa a flag estática
                "setBleEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    val isRunning = BleServerService.isServiceRunning // Usa a nova flag

                    if (enabled && !isRunning) {
                        // Start BLE service if not already running
                        Log.d("BLE", "Starting BLE service (via setBleEnabled)")
                        startBleServerService()
                    } else if (!enabled && isRunning) {
                        // Stop BLE service if running
                        Log.d("BLE", "Stopping BLE service (via setBleEnabled)")
                        stopBleServerService()
                    }
                    result.success(true)
                }
                "getAllRideIds" -> {
                    val ids = dbHelper.getAllRideIds()
                    result.success(ids)
                }
                "getRideData" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val data = dbHelper.getRideSummary(rideId)
                    result.success(data)
                }
                "calculateRideStatistics" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val stats = dbHelper.calculateRideStatistics(rideId)
                    result.success(stats)
                }
                "getRideVelocities" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val telemetryData = dbHelper.getRideTelemetryData(rideId)
                    val velocities = telemetryData.mapNotNull { data ->
                        data["gps_speed"] as? Double ?: data["crank_speed"] as? Double
                    }
                    result.success(velocities.map { it.toString() }) // Simplified
                }
                "getRideMapData" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val telemetryData = dbHelper.getRideTelemetryData(rideId)
                    val mapData = telemetryData.mapNotNull { data ->
                        val timestamp = data["gps_timestamp"] as? String
                        val lat = data["latitude"] as? Double
                        val lon = data["longitude"] as? Double
                        if (timestamp != null && lat != null && lon != null) {
                            "$timestamp:$lat:$lon"
                        } else null
                    }
                    result.success(mapData) // Simplified
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    /**
     * Lógica de permissão corrigida para lidar com o caso "já concedido".
     */
    private fun requestBlePermissions() {
        // (O seu código de construir a 'permissions list' continua o mesmo)
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, // Adicione se necessário
        ).plus(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                )
            } else emptyList()
        ).plus(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else emptyList()
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            Log.d("BLE", "Requisitando permissões: ${missingPermissions.joinToString()}")
            // Use a constante
            ActivityCompat.requestPermissions(this, missingPermissions, REQUEST_CODE_PERMISSIONS)
        } else {
            Log.d("BLE", "Todas as permissões já estavam concedidas.")
            // Se já temos, responda ao Flutter imediatamente
            this.permissionRequestResult?.success(true)
            this.permissionRequestResult = null
        }
    }

    private fun scanAndConnectDevice() {
        if (!bluetoothAdapter.isEnabled) {
            Log.e("BLE", "Bluetooth not enabled")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "No scan permission")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        val scanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val device = result.device
                val data = result.scanRecord
                if (data?.serviceUuids?.contains(ParcelUuid.fromString("12345678-1234-5678-1234-56789abcdef0")) == true) {
                    // Found bike computer
                    connectToDevice(device)
                    @SuppressLint("MissingPermission")
                    scanner.stopScan(this)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e("BLE", "Scan failed: $errorCode")
            }
        }

        scanning = true
        scanner.startScan(null, ScanSettings.Builder().build(), scanCallback)

        handler.postDelayed({
            if (scanning) {
                scanning = false
                scanner.stopScan(scanCallback)
            }
        }, SCAN_PERIOD)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "No connect permission")
            return
        }
        connectedDevice = device
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLE", "Connected to ${device.address}")
                    @SuppressLint("MissingPermission")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BLE", "Disconnected from ${device.address}")
                    connectedDevice = null
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("BLE", "Services discovered")
                    // Could read/write characteristics here if needed
                }
            }
        })
    }

    private fun disconnectDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "No connect permission")
            return
        }
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
    }

    // <<< MUDANÇA: FUNÇÃO DELETADA >>>
    /*
    private fun isBleServiceRunning(): Boolean {
        // ... Esta função foi removida ...
    }
    */

    private fun startBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        startService(intent)
    }

    private fun stopBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        stopService(intent)
    }

    private fun mapRideToActivity(rideId: Long, data: Map<String, Any?>?): Map<String, Any> {
        val currentTime = System.currentTimeMillis()

        // Parse start and end times from strings
        val startTimeStr = data?.get("start_time") as? String
        val endTimeStr = data?.get("end_time") as? String
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        val startTime = try { dateFormat.parse(startTimeStr ?: "")?.time ?: currentTime } catch (e: Exception) { currentTime }
        val endTime = try { dateFormat.parse(endTimeStr ?: "")?.time ?: (currentTime + 1000) } catch (e: Exception) { currentTime + 1000 }

        return mapOf(
            "id" to rideId.toString(),
            "startDatetime" to startTime,
            "endDatetime" to endTime,
            "distance" to (data?.get("total_distance_km") as? Double ?: 0.0),
            "speed" to (data?.get("avg_velocity_kmh") as? Double ?: 0.0),
            "cadence" to (data?.get("avg_cadence") as? Double ?: 0.0),
            "calories" to (data?.get("calories") as? Double ?: 0.0),
            "power" to (data?.get("avg_power") as? Double ?: 0.0),
            "altitude" to 900.0, // Placeholder
            "time" to ((endTime - startTime) / 1000.0), // Duration in seconds
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
        dbHelper.close()
    }
}