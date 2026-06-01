package com.shahin.iran.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.shahin.iran.variants.debugLog
import com.shahin.iran.global.updateStoredPreference
import com.shahin.iran.utils.logException
import com.shahin.iran.utils.update

/**
 * The Calendar Service that updates widget time and clock and build/update
 * calendar notification.
 */
class ApplicationService : Service() {
    private val receiver = BroadcastReceivers()

    override fun onBind(paramIntent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        debugLog("${ApplicationService::class.java.name} start")

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            // addAction(Intent.ACTION_TIME_TICK)
        }
        registerReceiver(receiver, intentFilter)

        updateStoredPreference(applicationContext)
        update(applicationContext, updateDate = true)

        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }.onFailure(logException)
        super.onDestroy()
    }
}
