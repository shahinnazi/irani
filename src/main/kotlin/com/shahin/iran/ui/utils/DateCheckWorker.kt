package com.shahin.iran

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.shahin.iran.utils.update
import android.util.Log

class DateCheckWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        try {
            Log.d("DateCheckWorker", "Checking date change at ${System.currentTimeMillis()}")
            Log.d("DateCheckWorker", "Worker executed successfully at ${System.currentTimeMillis()}")
            // Send broadcast to service to update foreground notification
            val intent = Intent("com.shahin.iran.DATE_CHECK_UPDATE")
            intent.setPackage(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)
            Log.d("DateCheckWorker", "Broadcast sent for date check update at ${System.currentTimeMillis()}")
            update(applicationContext, true) { notification ->
                Log.d("DateCheckWorker", "Inside update lambda at ${System.currentTimeMillis()}")
                if (notification != null) {
                    val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
                    notificationManager.notify(1001, notification) // Force update
                    Log.d("DateCheckWorker", "Notification updated due to date change at ${System.currentTimeMillis()}")
                } else {
                    Log.w("DateCheckWorker", "Notification is null at ${System.currentTimeMillis()}, update skipped")
                }
            }
            Log.d("DateCheckWorker", "Update call completed at ${System.currentTimeMillis()}")
            return Result.success()
        } catch (e: Exception) {
            Log.e("DateCheckWorker", "Error in doWork at ${System.currentTimeMillis()}", e)
            return Result.failure()
        }
    }
}
