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
        // Insert larger GPS path for visible map display (U shape: east-south-east)
        val baseLat = -25.4284f
        val baseLon = -49.2733f
        val baseAlt = 880f
        // Create a 10-minute ride (600 seconds) with GPS points every 10 seconds = 60 points
        val totalPoints = 60
        val timeIntervalSeconds = 10
        val path = mutableListOf<Triple<Float, Float, Float>>().apply {
            // East segment: decrease lon (west) - 20 points
            for (i in 0..19) {
                val progress = i.toFloat() / 19f
                add(Triple(baseLat, baseLon - progress * 0.2f, baseAlt + progress * 50f))
            }
            // South segment: increase lat - 20 points
            val turnPoint = last()
            for (i in 1..20) {
                val progress = i.toFloat() / 20f
                add(Triple(turnPoint.first + progress * 0.2f, turnPoint.second, turnPoint.third + progress * 30f))
            }
            // East segment: increase lon (east, back) - 20 points
            val turn2Point = last()
            for (i in 1..20) {
                val progress = i.toFloat() / 20f
                add(Triple(turn2Point.first, turn2Point.second + progress * 0.2f, turn2Point.third + progress * 20f))
            }
        }

        // Generate corresponding velocities, powers, and cadences (60 values)
        val velocities = mutableListOf<Float>()
        val powers = mutableListOf<Float>()
        val cadences = mutableListOf<Float>()

        // Create realistic cycling data with some variation
        for (i in 0 until totalPoints) {
            val baseVelocity = 15.0f + (i % 10) * 0.5f // 15-19.5 km/h with pattern
            velocities.add(baseVelocity + (-1..1).random() * 0.5f) // Add some noise

            val basePower = 150f + (i % 15) * 5f // 150-215 watts with pattern
            powers.add(basePower + (-5..5).random()) // Add some noise

            val baseCadence = 85f + (i % 10) * 2f // 85-103 rpm with pattern
            cadences.add(baseCadence + (-3..3).random()) // Add some noise
        }

        // Ensure ride exists first
        if (!dbHelper.ensureRideExists(SAMPLE_RIDE_ID, "Sample Ride")) {
            android.util.Log.e("BeForBike", "Failed to ensure ride exists")
            return
        }

        android.util.Log.d("BeForBike", "Inserting $totalPoints GPS points for sample ride")
        path.forEachIndexed { index, (lat, lon, alt) ->
            val timestamp = baseTime + index * timeIntervalSeconds * 1000L // 10 seconds apart
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
