package com.beforbike.app.database

import android.content.Context
import android.provider.BaseColumns
import com.beforbike.app.database.RideDbHelper

object SeedData {

    private const val SAMPLE_RIDE_ID = 999L
    private const val RIDES_TABLE_NAME = "Rides"
    private const val TELEMETRY_TABLE_NAME = "TelemetryData"
    private const val COLUMN_RIDE_ID = "ride_id"

    fun insertSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        android.util.Log.d("BeForBike", "Starting sample ride insertion for ride $SAMPLE_RIDE_ID")

        // First remove any existing sample ride
        android.util.Log.d("BeForBike", "Removing existing sample ride first")
        removeSampleRide(context)

        android.util.Log.d("BeForBike", "Proceeding with insertion")
        val baseTime = System.currentTimeMillis()
        // Use specific GPS coordinates for the test ride with larger distances
        val path = listOf(
            // Start: -25.4290, -49.2721
            Triple(-25.4290f, -49.2721f, 880f),
            // Checkpoint 1: -25.4270, -49.2700 (larger move)
            Triple(-25.4270f, -49.2700f, 885f),
            // Checkpoint 2: -25.4250, -49.2680
            Triple(-25.4310f, -49.2680f, 890f),
            // Checkpoint 3: -25.4230, -49.2660
            Triple(-25.4300f, -49.2660f, 895f),
            // End: -25.4210, -49.2640
            Triple(-25.4250f, -49.2640f, 900f)
        )

        val totalPoints = path.size
        val timeIntervalSeconds = 75 // 75 seconds between each checkpoint for a 5-minute ride (4 intervals * 75s = 300s = 5 minutes)

        // Generate corresponding velocities, powers, and cadences (5 values for 5 points)
        val velocities = listOf(17.5f, 18.2f, 16.8f, 19.1f, 17.9f)
        val powers = listOf(165f, 172f, 158f, 185f, 175f)
        val cadences = listOf(88f, 92f, 85f, 95f, 89f)

        // Ensure ride exists first (pass a proper timestamp string, not a label)
        val startTimeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(baseTime))
        if (!dbHelper.ensureRideExists(SAMPLE_RIDE_ID, startTimeStr)) {
            android.util.Log.e("BeForBike", "Failed to ensure ride exists")
            return
        }

        android.util.Log.d("BeForBike", "Inserting $totalPoints GPS points for sample ride")
        path.forEachIndexed { index, (lat, lon, alt) ->
            val timestamp = baseTime + index * timeIntervalSeconds * 1000L // 75 seconds apart
            val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp))

            val infoMap = mapOf(
                "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
                "time" to java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
            )
            val gpsMap = mapOf(
                "timestamp" to timestampStr,
                "latitude" to lat.toDouble(),
                "longitude" to lon.toDouble(),
                "altitude" to alt.toDouble(),
                "speed" to velocities[index] / 3.6, // Convert km/h to m/s
                "direction" to 0.0,
                "fix_satellites" to 8,
                "fix_quality" to 1
            )
            val crankMap = mapOf(
                "power" to powers[index].toDouble(),
                "cadence" to cadences[index].toDouble(),
                "joules" to 0.0,
                "calories" to 0.0, // Will be calculated by RideDbHelper
                "speed_ms" to velocities[index] / 3.6, // Convert km/h to m/s
                "speed" to velocities[index].toDouble(),
                "distance" to 0.0 // Will be calculated by RideDbHelper
            )

            val success = dbHelper.insertTelemetryData(SAMPLE_RIDE_ID, infoMap, gpsMap, crankMap)
            if (!success) {
                android.util.Log.w("BeForBike", "Failed to insert telemetry data for point $index")
            }
        }
        android.util.Log.d("BeForBike", "Sample ride insertion completed")
    }

    fun removeSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        val db = dbHelper.writableDatabase

        // Delete telemetry data
        db.delete(TELEMETRY_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        // Delete ride summary
        db.delete(RIDES_TABLE_NAME, "$COLUMN_RIDE_ID = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
    }
}
