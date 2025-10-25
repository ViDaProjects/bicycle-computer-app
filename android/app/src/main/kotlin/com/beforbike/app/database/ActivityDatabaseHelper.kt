package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*

class ActivityDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "activity_database.db"
        private const val DATABASE_VERSION = 1

        // Activities table
        const val TABLE_ACTIVITIES = "activities"
        const val COLUMN_ACTIVITY_ID = "id"
        const val COLUMN_ACTIVITY_TYPE = "type"
        const val COLUMN_START_DATETIME = "start_datetime"
        const val COLUMN_END_DATETIME = "end_datetime"
        const val COLUMN_DISTANCE = "distance"
        const val COLUMN_SPEED = "speed"
        const val COLUMN_CADENCE = "cadence"
        const val COLUMN_CALORIES = "calories"
        const val COLUMN_POWER = "power"
        const val COLUMN_ALTITUDE = "altitude"
        const val COLUMN_TIME = "time"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USER_NAME = "user_name"
        const val COLUMN_USER_AVATAR = "user_avatar"
        const val COLUMN_LIKES_COUNT = "likes_count"
        const val COLUMN_HAS_CURRENT_USER_LIKED = "has_current_user_liked"

        // Locations table
        const val TABLE_LOCATIONS = "locations"
        const val COLUMN_LOCATION_ID = "id"
        const val COLUMN_LOCATION_ACTIVITY_ID = "activity_id"
        const val COLUMN_LOCATION_DATETIME = "datetime"
        const val COLUMN_LOCATION_LATITUDE = "latitude"
        const val COLUMN_LOCATION_LONGITUDE = "longitude"
        const val COLUMN_LOCATION_SPEED = "speed"
        const val COLUMN_LOCATION_CADENCE = "cadence"
        const val COLUMN_LOCATION_POWER = "power"
        const val COLUMN_LOCATION_ALTITUDE = "altitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create activities table
        val createActivitiesTable = """
            CREATE TABLE $TABLE_ACTIVITIES (
                $COLUMN_ACTIVITY_ID TEXT PRIMARY KEY,
                $COLUMN_ACTIVITY_TYPE TEXT NOT NULL,
                $COLUMN_START_DATETIME INTEGER NOT NULL,
                $COLUMN_END_DATETIME INTEGER NOT NULL,
                $COLUMN_DISTANCE REAL NOT NULL,
                $COLUMN_SPEED REAL NOT NULL,
                $COLUMN_CADENCE REAL NOT NULL,
                $COLUMN_CALORIES REAL NOT NULL,
                $COLUMN_POWER REAL NOT NULL,
                $COLUMN_ALTITUDE REAL NOT NULL,
                $COLUMN_TIME REAL NOT NULL,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_AVATAR TEXT,
                $COLUMN_LIKES_COUNT REAL NOT NULL,
                $COLUMN_HAS_CURRENT_USER_LIKED INTEGER NOT NULL
            )
        """.trimIndent()

        // Create locations table
        val createLocationsTable = """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_LOCATION_ID TEXT PRIMARY KEY,
                $COLUMN_LOCATION_ACTIVITY_ID TEXT NOT NULL,
                $COLUMN_LOCATION_DATETIME INTEGER NOT NULL,
                $COLUMN_LOCATION_LATITUDE REAL NOT NULL,
                $COLUMN_LOCATION_LONGITUDE REAL NOT NULL,
                $COLUMN_LOCATION_SPEED REAL,
                $COLUMN_LOCATION_CADENCE REAL,
                $COLUMN_LOCATION_POWER REAL,
                $COLUMN_LOCATION_ALTITUDE REAL,
                FOREIGN KEY ($COLUMN_LOCATION_ACTIVITY_ID) REFERENCES $TABLE_ACTIVITIES ($COLUMN_ACTIVITY_ID)
            )
        """.trimIndent()

        db.execSQL(createActivitiesTable)
        db.execSQL(createLocationsTable)

        Log.d("ActivityDatabaseHelper", "Database tables created")

        // Insert fake data
        insertFakeActivities(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIVITIES")
        onCreate(db)
    }

    private fun insertFakeActivities(db: SQLiteDatabase) {
        Log.d("ActivityDatabaseHelper", "Inserting fake activities...")

        // Activity 1: Morning bike ride in São Paulo
        val activity1Id = "fake_activity_1"
        val activity1Start = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 20, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val activity1End = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 20, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val activity1Values = ContentValues().apply {
            put(COLUMN_ACTIVITY_ID, activity1Id)
            put(COLUMN_ACTIVITY_TYPE, "cycling")
            put(COLUMN_START_DATETIME, activity1Start)
            put(COLUMN_END_DATETIME, activity1End)
            put(COLUMN_DISTANCE, 25.5)
            put(COLUMN_SPEED, 18.2)
            put(COLUMN_CADENCE, 85.0)
            put(COLUMN_CALORIES, 650.0)
            put(COLUMN_POWER, 180.0)
            put(COLUMN_ALTITUDE, 150.0)
            put(COLUMN_TIME, 5400.0) // 90 minutes in seconds
            put(COLUMN_USER_ID, "user_1")
            put(COLUMN_USER_NAME, "João Silva")
            put(COLUMN_USER_AVATAR, "https://example.com/avatar1.jpg")
            put(COLUMN_LIKES_COUNT, 12.0)
            put(COLUMN_HAS_CURRENT_USER_LIKED, 0)
        }
        db.insert(TABLE_ACTIVITIES, null, activity1Values)

        // GPS path for Activity 1 (São Paulo route)
        val saoPauloLocations = listOf(
            Triple(-23.550520, -46.633308, 15.2), // Start near Paulista Avenue
            Triple(-23.551000, -46.634000, 16.1),
            Triple(-23.552000, -46.635000, 17.8),
            Triple(-23.553000, -46.636000, 18.5),
            Triple(-23.554000, -46.637000, 19.2),
            Triple(-23.555000, -46.638000, 18.9),
            Triple(-23.556000, -46.639000, 17.6),
            Triple(-23.557000, -46.640000, 16.8),
            Triple(-23.558000, -46.641000, 15.9),
            Triple(-23.559000, -46.642000, 14.7),
            Triple(-23.560000, -46.643000, 13.8),
            Triple(-23.561000, -46.644000, 12.9),
            Triple(-23.562000, -46.645000, 11.7),
            Triple(-23.563000, -46.646000, 10.8),
            Triple(-23.564000, -46.647000, 9.9),
            Triple(-23.565000, -46.648000, 8.7),
            Triple(-23.566000, -46.649000, 7.8),
            Triple(-23.567000, -46.650000, 6.9),
            Triple(-23.568000, -46.651000, 5.7),
            Triple(-23.569000, -46.652000, 4.8)  // End near Ibirapuera Park
        )

        saoPauloLocations.forEachIndexed { index, (lat, lng, speed) ->
            val locationId = "${activity1Id}_loc_$index"
            val locationTime = activity1Start + (index * 270000) // 4.5 minutes apart

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity1Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, speed)
                put(COLUMN_LOCATION_CADENCE, 82.0 + (index % 10))
                put(COLUMN_LOCATION_POWER, 175.0 + (index % 20))
                put(COLUMN_LOCATION_ALTITUDE, 780.0 + (index * 2))
            }
            db.insert(TABLE_LOCATIONS, null, locationValues)
        }

        // Activity 2: Afternoon run in Rio de Janeiro
        val activity2Id = "fake_activity_2"
        val activity2Start = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 21, 16, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val activity2End = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 21, 17, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val activity2Values = ContentValues().apply {
            put(COLUMN_ACTIVITY_ID, activity2Id)
            put(COLUMN_ACTIVITY_TYPE, "running")
            put(COLUMN_START_DATETIME, activity2Start)
            put(COLUMN_END_DATETIME, activity2End)
            put(COLUMN_DISTANCE, 8.2)
            put(COLUMN_SPEED, 9.8)
            put(COLUMN_CADENCE, 165.0)
            put(COLUMN_CALORIES, 480.0)
            put(COLUMN_POWER, 220.0)
            put(COLUMN_ALTITUDE, 85.0)
            put(COLUMN_TIME, 4500.0) // 75 minutes in seconds
            put(COLUMN_USER_ID, "user_2")
            put(COLUMN_USER_NAME, "Maria Santos")
            put(COLUMN_USER_AVATAR, "https://example.com/avatar2.jpg")
            put(COLUMN_LIKES_COUNT, 8.0)
            put(COLUMN_HAS_CURRENT_USER_LIKED, 0)
        }
        db.insert(TABLE_ACTIVITIES, null, activity2Values)

        // GPS path for Activity 2 (Rio de Janeiro route)
        val rioLocations = listOf(
            Triple(-22.906847, -43.172896, 8.5), // Start near Copacabana
            Triple(-22.907000, -43.173000, 9.2),
            Triple(-22.908000, -43.174000, 10.1),
            Triple(-22.909000, -43.175000, 9.8),
            Triple(-22.910000, -43.176000, 10.5),
            Triple(-22.911000, -43.177000, 11.2),
            Triple(-22.912000, -43.178000, 10.9),
            Triple(-22.913000, -43.179000, 9.7),
            Triple(-22.914000, -43.180000, 8.8),
            Triple(-22.915000, -43.181000, 7.9),
            Triple(-22.916000, -43.182000, 6.7),
            Triple(-22.917000, -43.183000, 5.8),
            Triple(-22.918000, -43.184000, 4.9),
            Triple(-22.919000, -43.185000, 3.7),
            Triple(-22.920000, -43.186000, 2.8)  // End near Ipanema
        )

        rioLocations.forEachIndexed { index, (lat, lng, speed) ->
            val locationId = "${activity2Id}_loc_$index"
            val locationTime = activity2Start + (index * 300000) // 5 minutes apart

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity2Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, speed)
                put(COLUMN_LOCATION_CADENCE, 160.0 + (index % 15))
                put(COLUMN_LOCATION_POWER, 210.0 + (index % 25))
                put(COLUMN_LOCATION_ALTITUDE, 15.0 + (index * 1.5))
            }
            db.insert(TABLE_LOCATIONS, null, locationValues)
        }

        Log.d("ActivityDatabaseHelper", "Fake activities inserted successfully")
    }

    // Method to get activity data for statistics
    fun getActivityData(activityId: String): List<Map<String, Any>> {
        val data = mutableListOf<Map<String, Any>>()

        val db = readableDatabase
        val query = """
            SELECT l.$COLUMN_LOCATION_DATETIME,
                   l.$COLUMN_LOCATION_SPEED,
                   l.$COLUMN_LOCATION_CADENCE,
                   l.$COLUMN_LOCATION_POWER,
                   l.$COLUMN_LOCATION_ALTITUDE
            FROM $TABLE_LOCATIONS l
            WHERE l.$COLUMN_LOCATION_ACTIVITY_ID = ?
            ORDER BY l.$COLUMN_LOCATION_DATETIME ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(activityId))

        if (cursor.moveToFirst()) {
            do {
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_DATETIME))
                val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_SPEED))
                val cadence = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_CADENCE))
                val power = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_POWER))
                val altitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_ALTITUDE))

                data.add(mapOf(
                    "timestamp" to Date(timestamp),
                    "speed" to speed,
                    "cadence" to cadence,
                    "power" to power,
                    "altitude" to altitude
                ))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return data
    }

    // Method to get all activities
    fun getAllActivities(): List<Map<String, Any>> {
        val activities = mutableListOf<Map<String, Any>>()

        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_ACTIVITIES
            ORDER BY $COLUMN_START_DATETIME DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val activity = mapOf(
                    "id" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_ID)),
                    "type" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACTIVITY_TYPE)),
                    "startDatetime" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_DATETIME)),
                    "endDatetime" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_DATETIME)),
                    "distance" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DISTANCE)),
                    "speed" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPEED)),
                    "cadence" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CADENCE)),
                    "calories" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_CALORIES)),
                    "power" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_POWER)),
                    "altitude" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ALTITUDE)),
                    "time" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                    "userId" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    "userName" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)),
                    "userAvatar" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_AVATAR)),
                    "likesCount" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LIKES_COUNT)),
                    "hasCurrentUserLiked" to (cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HAS_CURRENT_USER_LIKED)) == 1)
                )
                activities.add(activity)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return activities
    }

    // Method to get locations for an activity
    fun getActivityLocations(activityId: String): List<Map<String, Any>> {
        val locations = mutableListOf<Map<String, Any>>()

        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_LOCATIONS
            WHERE $COLUMN_LOCATION_ACTIVITY_ID = ?
            ORDER BY $COLUMN_LOCATION_DATETIME ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(activityId))

        if (cursor.moveToFirst()) {
            do {
                val location = mapOf(
                    "id" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_ID)),
                    "datetime" to cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_DATETIME)),
                    "latitude" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_LATITUDE)),
                    "longitude" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_LONGITUDE))
                )
                locations.add(location)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return locations
    }
}