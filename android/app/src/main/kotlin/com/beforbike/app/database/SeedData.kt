package com.beforbike.app.database

import android.content.Context
import android.provider.BaseColumns
import com.beforbike.app.database.RideDbHelper
import java.util.*

object SeedData {

    private const val SAMPLE_RIDE_ID = 999L

    fun insertSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        android.util.Log.d("BeForBike", "Starting sample ride insertion for ride $SAMPLE_RIDE_ID")

        // First remove any existing sample ride
        android.util.Log.d("BeForBike", "Removing existing sample ride first")
        removeSampleRide(context)

        android.util.Log.d("BeForBike", "Proceeding with insertion")
        val baseTime = System.currentTimeMillis()
        // Use specific GPS coordinates for the test ride
        val path = listOf(
            // Start: -25.4290, -49.2721
            Triple(-25.4290f, -49.2721f, 880f),
            // Checkpoint 1: -25.4285, -49.2730
            Triple(-25.4285f, -49.2730f, 885f),
            // Checkpoint 2: -25.4292, -49.2738
            Triple(-25.4292f, -49.2738f, 890f),
            // Checkpoint 3: -25.4301, -49.2735
            Triple(-25.4301f, -49.2735f, 895f),
            // End: -25.4303, -49.2732 (closer to Checkpoint 3 for better visual connection)
            Triple(-25.4303f, -49.2732f, 900f)
        )

        val totalPoints = path.size
        val timeIntervalSeconds = 75 // 75 seconds between each checkpoint for a 5-minute ride (4 intervals * 75s = 300s = 5 minutes)

        // Generate corresponding velocities, powers, and cadences (5 values for 5 points)
        val velocities = listOf(17.5f, 18.2f, 16.8f, 19.1f, 17.9f)
        val powers = listOf(165f, 172f, 158f, 185f, 175f)
        val cadences = listOf(88f, 92f, 85f, 95f, 89f)

        // Ensure ride exists first
        if (!dbHelper.ensureRideExists(SAMPLE_RIDE_ID, "Sample Ride")) {
            android.util.Log.e("BeForBike", "Failed to ensure ride exists")
            return
        }

        android.util.Log.d("BeForBike", "Inserting $totalPoints GPS points for sample ride")
        path.forEachIndexed { index, (lat, lon, alt) ->
            val timestamp = baseTime + index * timeIntervalSeconds * 1000L // 75 seconds apart
            val success = dbHelper.insertMapData(SAMPLE_RIDE_ID, lat, lon, alt, timestamp = timestamp)
            if (success) {
                dbHelper.insertVelocity(SAMPLE_RIDE_ID, velocities[index], timestamp = timestamp)
                dbHelper.insertPower(SAMPLE_RIDE_ID, powers[index], timestamp = timestamp)
                dbHelper.insertCadence(SAMPLE_RIDE_ID, cadences[index], timestamp = timestamp)
            }
        }
        android.util.Log.d("BeForBike", "Sample ride insertion completed")
    }

    fun removeSampleRide(context: Context) {
        val dbHelper = RideDbHelper(context)
        val db = dbHelper.writableDatabase

        // Delete related data
        db.delete(RideDbHelper.TABLE_CADENCE, "${RideDbHelper.COLUMN_RIDE_ID} = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        db.delete(RideDbHelper.TABLE_POWER, "${RideDbHelper.COLUMN_RIDE_ID} = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        db.delete(RideDbHelper.TABLE_MAPDATA, "${RideDbHelper.COLUMN_RIDE_ID} = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        db.delete(RideDbHelper.TABLE_VELOCITY, "${RideDbHelper.COLUMN_RIDE_ID} = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
        db.delete(RideDbHelper.TABLE_RIDES, "${BaseColumns._ID} = ?", arrayOf(SAMPLE_RIDE_ID.toString()))
    }
}
