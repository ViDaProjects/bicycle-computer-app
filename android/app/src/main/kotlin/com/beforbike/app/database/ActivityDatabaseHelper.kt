package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

class ActivityDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "activity_database.db"
        private const val DATABASE_VERSION = 4

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
            put(COLUMN_CALORIES, 200.0) // Low calories - light activity
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

        // GPS path for Activity 1 (São Paulo route - much more detailed)
        val saoPauloLocations = listOf(
            Triple(-23.550520, -46.633308, 15.2), // Start near Paulista Avenue
            Triple(-23.550600, -46.633400, 15.5),
            Triple(-23.550700, -46.633500, 16.1),
            Triple(-23.550800, -46.633600, 16.8),
            Triple(-23.550900, -46.633700, 17.2),
            Triple(-23.551000, -46.633800, 17.8),
            Triple(-23.551100, -46.633900, 18.1),
            Triple(-23.551200, -46.634000, 18.5),
            Triple(-23.551300, -46.634100, 18.9),
            Triple(-23.551400, -46.634200, 19.2),
            Triple(-23.551500, -46.634300, 19.5),
            Triple(-23.551600, -46.634400, 19.8),
            Triple(-23.551700, -46.634500, 20.1),
            Triple(-23.551800, -46.634600, 20.3),
            Triple(-23.551900, -46.634700, 20.5),
            Triple(-23.552000, -46.634800, 20.7),
            Triple(-23.552100, -46.634900, 20.8),
            Triple(-23.552200, -46.635000, 20.9),
            Triple(-23.552300, -46.635100, 21.0),
            Triple(-23.552400, -46.635200, 21.1),
            Triple(-23.552500, -46.635300, 21.2),
            Triple(-23.552600, -46.635400, 21.3),
            Triple(-23.552700, -46.635500, 21.4),
            Triple(-23.552800, -46.635600, 21.5),
            Triple(-23.552900, -46.635700, 21.6),
            Triple(-23.553000, -46.635800, 21.7),
            Triple(-23.553100, -46.635900, 21.8),
            Triple(-23.553200, -46.636000, 21.9),
            Triple(-23.553300, -46.636100, 22.0),
            Triple(-23.553400, -46.636200, 22.1),
            Triple(-23.553500, -46.636300, 22.2),
            Triple(-23.553600, -46.636400, 22.3),
            Triple(-23.553700, -46.636500, 22.4),
            Triple(-23.553800, -46.636600, 22.5),
            Triple(-23.553900, -46.636700, 22.6),
            Triple(-23.554000, -46.636800, 22.7),
            Triple(-23.554100, -46.636900, 22.8),
            Triple(-23.554200, -46.637000, 22.9),
            Triple(-23.554300, -46.637100, 23.0),
            Triple(-23.554400, -46.637200, 23.1),
            Triple(-23.554500, -46.637300, 23.2),
            Triple(-23.554600, -46.637400, 23.3),
            Triple(-23.554700, -46.637500, 23.4),
            Triple(-23.554800, -46.637600, 23.5),
            Triple(-23.554900, -46.637700, 23.6),
            Triple(-23.555000, -46.637800, 23.7),
            Triple(-23.555100, -46.637900, 23.8),
            Triple(-23.555200, -46.638000, 23.9),
            Triple(-23.555300, -46.638100, 24.0),
            Triple(-23.555400, -46.638200, 24.1),
            Triple(-23.555500, -46.638300, 24.2),
            Triple(-23.555600, -46.638400, 24.3),
            Triple(-23.555700, -46.638500, 24.4),
            Triple(-23.555800, -46.638600, 24.5),
            Triple(-23.555900, -46.638700, 24.6),
            Triple(-23.556000, -46.638800, 24.7),
            Triple(-23.556100, -46.638900, 24.8),
            Triple(-23.556200, -46.639000, 24.9),
            Triple(-23.556300, -46.639100, 25.0),
            Triple(-23.556400, -46.639200, 25.1),
            Triple(-23.556500, -46.639300, 25.2),
            Triple(-23.556600, -46.639400, 25.3),
            Triple(-23.556700, -46.639500, 25.4),
            Triple(-23.556800, -46.639600, 25.5),
            Triple(-23.556900, -46.639700, 25.6),
            Triple(-23.557000, -46.639800, 25.7),
            Triple(-23.557100, -46.639900, 25.8),
            Triple(-23.557200, -46.640000, 25.9),
            Triple(-23.557300, -46.640100, 26.0),
            Triple(-23.557400, -46.640200, 26.1),
            Triple(-23.557500, -46.640300, 26.2),
            Triple(-23.557600, -46.640400, 26.3),
            Triple(-23.557700, -46.640500, 26.4),
            Triple(-23.557800, -46.640600, 26.5),
            Triple(-23.557900, -46.640700, 26.6),
            Triple(-23.558000, -46.640800, 26.7),
            Triple(-23.558100, -46.640900, 26.8),
            Triple(-23.558200, -46.641000, 26.9),
            Triple(-23.558300, -46.641100, 27.0),
            Triple(-23.558400, -46.641200, 27.1),
            Triple(-23.558500, -46.641300, 27.2),
            Triple(-23.558600, -46.641400, 27.3),
            Triple(-23.558700, -46.641500, 27.4),
            Triple(-23.558800, -46.641600, 27.5),
            Triple(-23.558900, -46.641700, 27.6),
            Triple(-23.559000, -46.641800, 27.7),
            Triple(-23.559100, -46.641900, 27.8),
            Triple(-23.559200, -46.642000, 27.9),
            Triple(-23.559300, -46.642100, 28.0),
            Triple(-23.559400, -46.642200, 28.1),
            Triple(-23.559500, -46.642300, 28.2),
            Triple(-23.559600, -46.642400, 28.3),
            Triple(-23.559700, -46.642500, 28.4),
            Triple(-23.559800, -46.642600, 28.5),
            Triple(-23.559900, -46.642700, 28.6),
            Triple(-23.560000, -46.642800, 28.7),
            Triple(-23.560100, -46.642900, 28.8),
            Triple(-23.560200, -46.643000, 28.9),
            Triple(-23.560300, -46.643100, 29.0),
            Triple(-23.560400, -46.643200, 29.1),
            Triple(-23.560500, -46.643300, 29.2),
            Triple(-23.560600, -46.643400, 29.3),
            Triple(-23.560700, -46.643500, 29.4),
            Triple(-23.560800, -46.643600, 29.5),
            Triple(-23.560900, -46.643700, 29.6),
            Triple(-23.561000, -46.643800, 29.7),
            Triple(-23.561100, -46.643900, 29.8),
            Triple(-23.561200, -46.644000, 29.9),
            Triple(-23.561300, -46.644100, 30.0),
            Triple(-23.561400, -46.644200, 30.1),
            Triple(-23.561500, -46.644300, 30.2),
            Triple(-23.561600, -46.644400, 30.3),
            Triple(-23.561700, -46.644500, 30.4),
            Triple(-23.561800, -46.644600, 30.5),
            Triple(-23.561900, -46.644700, 30.6),
            Triple(-23.562000, -46.644800, 30.7),
            Triple(-23.562100, -46.644900, 30.8),
            Triple(-23.562200, -46.645000, 30.9),
            Triple(-23.562300, -46.645100, 31.0),
            Triple(-23.562400, -46.645200, 31.1),
            Triple(-23.562500, -46.645300, 31.2),
            Triple(-23.562600, -46.645400, 31.3),
            Triple(-23.562700, -46.645500, 31.4),
            Triple(-23.562800, -46.645600, 31.5),
            Triple(-23.562900, -46.645700, 31.6),
            Triple(-23.563000, -46.645800, 31.7),
            Triple(-23.563100, -46.645900, 31.8),
            Triple(-23.563200, -46.646000, 31.9),
            Triple(-23.563300, -46.646100, 32.0),
            Triple(-23.563400, -46.646200, 32.1),
            Triple(-23.563500, -46.646300, 32.2),
            Triple(-23.563600, -46.646400, 32.3),
            Triple(-23.563700, -46.646500, 32.4),
            Triple(-23.563800, -46.646600, 32.5),
            Triple(-23.563900, -46.646700, 32.6),
            Triple(-23.564000, -46.646800, 32.7),
            Triple(-23.564100, -46.646900, 32.8),
            Triple(-23.564200, -46.647000, 32.9),
            Triple(-23.564300, -46.647100, 33.0),
            Triple(-23.564400, -46.647200, 33.1),
            Triple(-23.564500, -46.647300, 33.2),
            Triple(-23.564600, -46.647400, 33.3),
            Triple(-23.564700, -46.647500, 33.4),
            Triple(-23.564800, -46.647600, 33.5),
            Triple(-23.564900, -46.647700, 33.6),
            Triple(-23.565000, -46.647800, 33.7),
            Triple(-23.565100, -46.647900, 33.8),
            Triple(-23.565200, -46.648000, 33.9),
            Triple(-23.565300, -46.648100, 34.0),
            Triple(-23.565400, -46.648200, 34.1),
            Triple(-23.565500, -46.648300, 34.2),
            Triple(-23.565600, -46.648400, 34.3),
            Triple(-23.565700, -46.648500, 34.4),
            Triple(-23.565800, -46.648600, 34.5),
            Triple(-23.565900, -46.648700, 34.6),
            Triple(-23.566000, -46.648800, 34.7),
            Triple(-23.566100, -46.648900, 34.8),
            Triple(-23.566200, -46.649000, 34.9),
            Triple(-23.566300, -46.649100, 35.0),
            Triple(-23.566400, -46.649200, 35.1),
            Triple(-23.566500, -46.649300, 35.2),
            Triple(-23.566600, -46.649400, 35.3),
            Triple(-23.566700, -46.649500, 35.4),
            Triple(-23.566800, -46.649600, 35.5),
            Triple(-23.566900, -46.649700, 35.6),
            Triple(-23.567000, -46.649800, 35.7),
            Triple(-23.567100, -46.649900, 35.8),
            Triple(-23.567200, -46.650000, 35.9),
            Triple(-23.567300, -46.650100, 36.0),
            Triple(-23.567400, -46.650200, 36.1),
            Triple(-23.567500, -46.650300, 36.2),
            Triple(-23.567600, -46.650400, 36.3),
            Triple(-23.567700, -46.650500, 36.4),
            Triple(-23.567800, -46.650600, 36.5),
            Triple(-23.567900, -46.650700, 36.6),
            Triple(-23.568000, -46.650800, 36.7),
            Triple(-23.568100, -46.650900, 36.8),
            Triple(-23.568200, -46.651000, 36.9),
            Triple(-23.568300, -46.651100, 37.0),
            Triple(-23.568400, -46.651200, 37.1),
            Triple(-23.568500, -46.651300, 37.2),
            Triple(-23.568600, -46.651400, 37.3),
            Triple(-23.568700, -46.651500, 37.4),
            Triple(-23.568800, -46.651600, 37.5),
            Triple(-23.568900, -46.651700, 37.6),
            Triple(-23.569000, -46.651800, 37.7),
            Triple(-23.569100, -46.651900, 37.8),
            Triple(-23.569200, -46.652000, 37.9)  // End near Ibirapuera Park
        )

        saoPauloLocations.forEachIndexed { index, (lat, lng, speed) ->
            val locationId = "${activity1Id}_loc_$index"
            val locationTime = activity1Start + (index * 15000L) // 15 seconds apart

            // Create varied curves using mathematical functions
            val progress = index.toDouble() / saoPauloLocations.size
            val speedVariation = sin(progress * 4 * Math.PI) * 3.0 + cos(progress * 2 * Math.PI) * 2.0
            val cadenceVariation = sin(progress * 6 * Math.PI) * 8.0 + cos(progress * 3 * Math.PI) * 5.0
            val powerVariation = sin(progress * 5 * Math.PI) * 15.0 + cos(progress * 2.5 * Math.PI) * 10.0
            val altitudeVariation = sin(progress * 3 * Math.PI) * 20.0 + cos(progress * 1.5 * Math.PI) * 15.0

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity1Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, (speed + speedVariation).coerceIn(20.0, 45.0))
                put(COLUMN_LOCATION_CADENCE, (85.0 + cadenceVariation).coerceIn(70.0, 110.0))
                put(COLUMN_LOCATION_POWER, (180.0 + powerVariation).coerceIn(150.0, 250.0))
                put(COLUMN_LOCATION_ALTITUDE, 780.0 + (index * 1.5) + altitudeVariation)
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
            put(COLUMN_CALORIES, 1800.0) // Very high calories - intense activity
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

            // Create varied curves for running activity
            val progress = index.toDouble() / rioLocations.size
            val speedVariation = sin(progress * 3 * Math.PI) * 1.5 + cos(progress * 1.8 * Math.PI) * 1.0
            val cadenceVariation = sin(progress * 4 * Math.PI) * 12.0 + cos(progress * 2.2 * Math.PI) * 8.0
            val powerVariation = sin(progress * 3.5 * Math.PI) * 20.0 + cos(progress * 2 * Math.PI) * 15.0
            val altitudeVariation = sin(progress * 2.5 * Math.PI) * 8.0 + cos(progress * 1.2 * Math.PI) * 6.0

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity2Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, (speed + speedVariation).coerceIn(3.0, 15.0))
                put(COLUMN_LOCATION_CADENCE, (165.0 + cadenceVariation).coerceIn(140.0, 190.0))
                put(COLUMN_LOCATION_POWER, (220.0 + powerVariation).coerceIn(180.0, 280.0))
                put(COLUMN_LOCATION_ALTITUDE, 15.0 + (index * 0.8) + altitudeVariation)
            }
            db.insert(TABLE_LOCATIONS, null, locationValues)
        }

        // Activity 3: Moderate bike ride (medium-low calories)
        val activity3Id = "fake_activity_3"
        val activity3Start = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 22, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val activity3End = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 22, 8, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val activity3Values = ContentValues().apply {
            put(COLUMN_ACTIVITY_ID, activity3Id)
            put(COLUMN_ACTIVITY_TYPE, "cycling")
            put(COLUMN_START_DATETIME, activity3Start)
            put(COLUMN_END_DATETIME, activity3End)
            put(COLUMN_DISTANCE, 25.5)
            put(COLUMN_SPEED, 18.2)
            put(COLUMN_CADENCE, 85.0)
            put(COLUMN_CALORIES, 600.0) // Medium-low calories
            put(COLUMN_POWER, 220.0)
            put(COLUMN_ALTITUDE, 150.0)
            put(COLUMN_TIME, 5400.0) // 90 minutes in seconds
            put(COLUMN_USER_ID, "user_3")
            put(COLUMN_USER_NAME, "Carlos Silva")
            put(COLUMN_USER_AVATAR, "https://example.com/avatar3.jpg")
            put(COLUMN_LIKES_COUNT, 12.0)
            put(COLUMN_HAS_CURRENT_USER_LIKED, 0)
        }
        db.insert(TABLE_ACTIVITIES, null, activity3Values)

        // Activity 4: Intense walk/run (medium-high calories)
        val activity4Id = "fake_activity_4"
        val activity4Start = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 23, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val activity4End = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 23, 18, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val activity4Values = ContentValues().apply {
            put(COLUMN_ACTIVITY_ID, activity4Id)
            put(COLUMN_ACTIVITY_TYPE, "running")
            put(COLUMN_START_DATETIME, activity4Start)
            put(COLUMN_END_DATETIME, activity4End)
            put(COLUMN_DISTANCE, 5.8)
            put(COLUMN_SPEED, 7.8)
            put(COLUMN_CADENCE, 170.0)
            put(COLUMN_CALORIES, 1000.0) // Medium-high calories
            put(COLUMN_POWER, 250.0)
            put(COLUMN_ALTITUDE, 45.0)
            put(COLUMN_TIME, 2700.0) // 45 minutes in seconds
            put(COLUMN_USER_ID, "user_4")
            put(COLUMN_USER_NAME, "Ana Costa")
            put(COLUMN_USER_AVATAR, "https://example.com/avatar4.jpg")
            put(COLUMN_LIKES_COUNT, 18.0)
            put(COLUMN_HAS_CURRENT_USER_LIKED, 1)
        }
        db.insert(TABLE_ACTIVITIES, null, activity4Values)

        // GPS path for Activity 3 (moderate bike ride - similar to activity 1 but different variations)
        val moderateBikeLocations = listOf(
            Triple(-23.550520, -46.633308, 16.0), // Start near Paulista Avenue
            Triple(-23.550600, -46.633400, 16.5),
            Triple(-23.550700, -46.633500, 17.2),
            Triple(-23.550800, -46.633600, 17.8),
            Triple(-23.550900, -46.633700, 18.3),
            Triple(-23.551000, -46.633800, 18.7),
            Triple(-23.551100, -46.633900, 19.1),
            Triple(-23.551200, -46.634000, 19.5),
            Triple(-23.551300, -46.634100, 19.8),
            Triple(-23.551400, -46.634200, 20.2),
            Triple(-23.551500, -46.634300, 20.6),
            Triple(-23.551600, -46.634400, 21.0),
            Triple(-23.551700, -46.634500, 21.3),
            Triple(-23.551800, -46.634600, 21.7),
            Triple(-23.551900, -46.634700, 22.1),
            Triple(-23.552000, -46.634800, 22.4),
            Triple(-23.552100, -46.634900, 22.8),
            Triple(-23.552200, -46.635000, 23.2),
            Triple(-23.552300, -46.635100, 23.5),
            Triple(-23.552400, -46.635200, 23.9)  // End point
        )

        moderateBikeLocations.forEachIndexed { index, (lat, lng, speed) ->
            val locationId = "${activity3Id}_loc_$index"
            val locationTime = activity3Start + (index * 18000L) // 18 seconds apart

            // Create moderate variations for cycling
            val progress = index.toDouble() / moderateBikeLocations.size
            val speedVariation = sin(progress * 3 * Math.PI) * 2.5 + cos(progress * 1.5 * Math.PI) * 1.8
            val cadenceVariation = sin(progress * 5 * Math.PI) * 6.0 + cos(progress * 2.5 * Math.PI) * 4.0
            val powerVariation = sin(progress * 4 * Math.PI) * 12.0 + cos(progress * 2 * Math.PI) * 8.0
            val altitudeVariation = sin(progress * 2 * Math.PI) * 15.0 + cos(progress * 1 * Math.PI) * 10.0

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity3Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, (speed + speedVariation).coerceIn(15.0, 30.0))
                put(COLUMN_LOCATION_CADENCE, (85.0 + cadenceVariation).coerceIn(75.0, 100.0))
                put(COLUMN_LOCATION_POWER, (220.0 + powerVariation).coerceIn(180.0, 260.0))
                put(COLUMN_LOCATION_ALTITUDE, 150.0 + (index * 0.5) + altitudeVariation)
            }
            db.insert(TABLE_LOCATIONS, null, locationValues)
        }

        // GPS path for Activity 4 (intense run/walk - Rio de Janeiro route with higher intensity)
        val intenseRunLocations = listOf(
            Triple(-22.906847, -43.172896, 7.0), // Start near Copacabana
            Triple(-22.907000, -43.173000, 7.3),
            Triple(-22.907200, -43.173200, 7.8),
            Triple(-22.907400, -43.173400, 8.2),
            Triple(-22.907600, -43.173600, 8.6),
            Triple(-22.907800, -43.173800, 9.1),
            Triple(-22.908000, -43.174000, 9.4),
            Triple(-22.908200, -43.174200, 9.8),
            Triple(-22.908400, -43.174400, 10.2),
            Triple(-22.908600, -43.174600, 10.5),
            Triple(-22.908800, -43.174800, 10.9),
            Triple(-22.909000, -43.175000, 11.3),
            Triple(-22.909200, -43.175200, 11.6),
            Triple(-22.909400, -43.175400, 12.0),
            Triple(-22.909600, -43.175600, 12.3),
            Triple(-22.909800, -43.175800, 12.7),
            Triple(-22.910000, -43.176000, 13.0),
            Triple(-22.910200, -43.176200, 13.4),
            Triple(-22.910400, -43.176400, 13.7),
            Triple(-22.910600, -43.176600, 14.0)  // End point
        )

        intenseRunLocations.forEachIndexed { index, (lat, lng, speed) ->
            val locationId = "${activity4Id}_loc_$index"
            val locationTime = activity4Start + (index * 8000L) // 8 seconds apart (faster pace)

            // Create intense variations for running
            val progress = index.toDouble() / intenseRunLocations.size
            val speedVariation = sin(progress * 4 * Math.PI) * 1.8 + cos(progress * 2.2 * Math.PI) * 1.3
            val cadenceVariation = sin(progress * 6 * Math.PI) * 10.0 + cos(progress * 3 * Math.PI) * 7.0
            val powerVariation = sin(progress * 5 * Math.PI) * 18.0 + cos(progress * 2.5 * Math.PI) * 12.0
            val altitudeVariation = sin(progress * 3.5 * Math.PI) * 12.0 + cos(progress * 1.8 * Math.PI) * 8.0

            val locationValues = ContentValues().apply {
                put(COLUMN_LOCATION_ID, locationId)
                put(COLUMN_LOCATION_ACTIVITY_ID, activity4Id)
                put(COLUMN_LOCATION_DATETIME, locationTime)
                put(COLUMN_LOCATION_LATITUDE, lat)
                put(COLUMN_LOCATION_LONGITUDE, lng)
                put(COLUMN_LOCATION_SPEED, (speed + speedVariation).coerceIn(6.0, 16.0))
                put(COLUMN_LOCATION_CADENCE, (170.0 + cadenceVariation).coerceIn(150.0, 190.0))
                put(COLUMN_LOCATION_POWER, (250.0 + powerVariation).coerceIn(200.0, 300.0))
                put(COLUMN_LOCATION_ALTITUDE, 45.0 + (index * 0.8) + altitudeVariation)
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