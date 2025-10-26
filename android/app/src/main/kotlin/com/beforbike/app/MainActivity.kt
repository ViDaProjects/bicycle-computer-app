package com.beforbike.app

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import com.beforbike.app.database.ActivityDatabaseHelper

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.beforbike.app/database"
    private lateinit var databaseHelper: ActivityDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = ActivityDatabaseHelper(this)

        MethodChannel(flutterEngine?.dartExecutor?.binaryMessenger!!, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getActivityData" -> {
                    val activityId = call.argument<String>("activityId")
                    if (activityId != null) {
                        try {
                            Log.d("MainActivity", "getActivityData called with activityId: $activityId")
                            val data = databaseHelper.getActivityData(activityId)
                            Log.d("MainActivity", "getActivityData returned ${data.size} data points")
                            result.success(data)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error getting activity data", e)
                            result.error("DATABASE_ERROR", "Failed to get activity data", e.message)
                        }
                    } else {
                        result.error("INVALID_ARGUMENT", "Activity ID is required", null)
                    }
                }
                "getAllActivities" -> {
                    try {
                        val activities = databaseHelper.getAllActivities()
                        Log.d("MainActivity", "getAllActivities returned ${activities.size} activities")
                        if (activities.isNotEmpty()) {
                            Log.d("MainActivity", "First activity: ${activities.first()}")
                        }
                        result.success(activities)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error getting activities", e)
                        result.error("DATABASE_ERROR", "Failed to get activities", e.message)
                    }
                }
                "getActivityLocations" -> {
                    val activityId = call.argument<String>("activityId")
                    if (activityId != null) {
                        try {
                            val locations = databaseHelper.getActivityLocations(activityId)
                            result.success(locations)
                        } catch (e: Exception) {
                            result.error("DATABASE_ERROR", "Failed to get activity locations", e.message)
                        }
                    } else {
                        result.error("INVALID_ARGUMENT", "Activity ID is required", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
