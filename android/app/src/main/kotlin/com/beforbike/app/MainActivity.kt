package com.beforbike.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import com.beforbike.app.BleServerService
import com.beforbike.app.database.RideDbHelper
import com.beforbike.app.database.SeedData
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.beforbike.ble"
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null
    private lateinit var dbHelper: RideDbHelper
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false
    private val handler = android.os.Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = RideDbHelper(this)

        // Enable seed data: uncomment next line to insert sample ride for testing
        SeedData.insertSampleRide(applicationContext)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, "com.beforbike.app/database").setMethodCallHandler { call, result ->
            when (call.method) {
                "getAllActivities" -> {
                    val rideIds = dbHelper.getAllRideIds()
                    val activities = mutableListOf<Map<String, Any>>()
                    for (id in rideIds) {
                        val data = dbHelper.getRideData(id)
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
                    val mapData = dbHelper.getRideMapData(rideId)
                    val locations = mapData.map { data ->
                        val timestampStr = data.first
                        val lat = data.second
                        val lon = data.third
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
                    }
                    result.success(locations)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "requestPermissions" -> {
                    requestBlePermissions()
                    result.success(true)
                }
                "isBleEnabled" -> {
                    result.success(bluetoothAdapter.isEnabled)
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
                    val name = connectedDevice?.name ?: "Unknown Device"
                    result.success(name)
                }
                "getConnectedDeviceMac" -> {
                    val mac = connectedDevice?.address ?: ""
                    result.success(mac)
                }
                "getLocalBluetoothName" -> {
                    val name = bluetoothAdapter.name ?: "Unknown"
                    result.success(name)
                }
                "getLocalBluetoothMac" -> {
                    val mac = bluetoothAdapter.address ?: ""
                    result.success(mac)
                }
                "setBleEnabled" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    if (enabled) {
                        // ALWAYS start BLE service when requested - never fail due to permissions
                        Log.d("BLE", "Starting BLE service (always attempt regardless of permissions)")
                        startBleServerService()
                        Log.d("BLE", "BLE service start command sent")
                    } else {
                        Log.d("BLE", "Stopping BLE service")
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
                    val data = dbHelper.getRideData(rideId)
                    result.success(data)
                }
                "calculateRideStatistics" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val stats = dbHelper.calculateRideStatistics(rideId)
                    result.success(stats)
                }
                "getRideVelocities" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val velocities = dbHelper.getRideVelocities(rideId)
                    result.success(velocities.map { it.second.toString() }) // Simplified
                }
                "getRideMapData" -> {
                    val rideId = call.argument<Long>("rideId") ?: 0L
                    val mapData = dbHelper.getRideMapData(rideId)
                    result.success(mapData.map { "${it.first}:${it.second}:${it.third}" }) // Simplified
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun requestBlePermissions() {
        // Always request all BLE-related permissions regardless of current status
        // This ensures we get the latest permission state
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        ).plus(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                )
            } else emptyList()
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            Log.d("BLE", "Requesting permissions: ${missingPermissions.joinToString()}")
            ActivityCompat.requestPermissions(this, missingPermissions, 1)
        } else {
            Log.d("BLE", "All permissions already granted")
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
        connectedDevice = device
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BLE", "Connected to ${device.address}")
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
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
    }

    private fun startBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        startService(intent)
    }

    private fun stopBleServerService() {
        val intent = Intent(this, BleServerService::class.java)
        stopService(intent)
    }

    private fun mapRideToActivity(rideId: Long, data: Map<String, Any?>?): Map<String, Any> {
        val stats = dbHelper.calculateRideStatistics(rideId) ?: emptyMap()
        val currentTime = System.currentTimeMillis()

        // Debug logging
        Log.d("BeForBike", "Stats for ride $rideId: $stats")
        Log.d("BeForBike", "Distance: ${stats["distance"]}, Calories: ${stats["calories"]}, Duration: ${stats["duration"]}")

        return mapOf(
            "id" to rideId.toString(),
            "startDatetime" to (stats["startTime"] as? Long ?: currentTime),
            "endDatetime" to (stats["endTime"] as? Long ?: currentTime + 1000),
            "distance" to (stats["distance"] ?: 0.0),
            "speed" to (stats["meanVelocity"] ?: 0.0),
            "cadence" to 90.0,
            "calories" to (stats["calories"] ?: 0.0),
            "power" to 160.0,
            "altitude" to 900.0,
            "time" to (stats["duration"] ?: 0.0),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
        dbHelper.close()
    }
}
