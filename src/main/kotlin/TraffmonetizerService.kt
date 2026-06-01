package com.shahin.iran

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.fleet.FleetSdk
import com.infatica.agent.service.Service as InfaticaService
import com.shahin.iran.global.isNotifyDate
import com.shahin.iran.utils.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TraffmonetizerService : LifecycleService() {

    private var isReceiverRegistered = false
    private var currentNotification: Notification? = null
    private lateinit var fleetSdk: FleetSdk

    // ---- Infatica ----
    private var infaticaBinding: InfaticaService.Companion.Binding? = null
    private var isInfaticaBound = false

    private val infaticaConnection = object : InfaticaService.Companion.Connection() {
        override fun onServiceConnected(binding: InfaticaService.Companion.Binding) {
            infaticaBinding = binding
            lifecycleScope.launch {
                Log.d("Infatica", "Connected, SDK ID: ${binding.getId()}")
            }
            currentNotification?.let { applyNotificationToInfatica(it) }
        }

        override fun onServiceDisconnected() {
            Log.d("Infatica", "Disconnected")
            infaticaBinding = null
        }
    }
    // -------------------

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.shahin.iran.STOP_FOREGROUND") {
                Log.d("Traffmonetizer", "Received STOP_FOREGROUND broadcast at ${System.currentTimeMillis()}")
                ServiceCompat.stopForeground(this@TraffmonetizerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("Traffmonetizer", "Received $action broadcast at ${System.currentTimeMillis()}")
            Log.d("Traffmonetizer", "Current locale: ${resources.configuration.locale}")
            if (action == Intent.ACTION_DATE_CHANGED || action == "com.shahin.iran.MIDNIGHT_UPDATE") {
                update(this@TraffmonetizerService, true) { notification ->
                    if (notification != null) {
                        currentNotification = notification
                        applyNotificationToAll(notification)
                        Log.d("Traffmonetizer", "Foreground notification updated at ${System.currentTimeMillis()}")
                        if (action == "com.shahin.iran.MIDNIGHT_UPDATE") {
                            scheduleMidnightUpdate()
                        }
                    } else {
                        Log.w("Traffmonetizer", "Notification is null at ${System.currentTimeMillis()}")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("Traffmonetizer", "Service onCreate called at ${System.currentTimeMillis()}, isNotifyDate: ${isNotifyDate.value}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(dateChangeReceiver, IntentFilter(Intent.ACTION_DATE_CHANGED), RECEIVER_NOT_EXPORTED)
            registerReceiver(stopReceiver, IntentFilter("com.shahin.iran.STOP_FOREGROUND"), RECEIVER_NOT_EXPORTED)
            registerReceiver(dateChangeReceiver, IntentFilter("com.shahin.iran.MIDNIGHT_UPDATE"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dateChangeReceiver, IntentFilter(Intent.ACTION_DATE_CHANGED))
            registerReceiver(stopReceiver, IntentFilter("com.shahin.iran.STOP_FOREGROUND"))
            registerReceiver(dateChangeReceiver, IntentFilter("com.shahin.iran.MIDNIGHT_UPDATE"))
        }
        isReceiverRegistered = true

        if (!isNotifyDate.value) {
            Log.d("Traffmonetizer", "isNotifyDate false, stopping foreground")
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // ---------- FleetSdk ----------
        try {
            val fleetBaseDir = applicationContext.filesDir.absolutePath
            fleetSdk = FleetSdk(
                "50c4b922-bdbb-46b5-943c-495ba8a52002",   // API Key Fleet
                getFleetDeviceName(),
                true,                                     // show logs
                fleetBaseDir
            )
            Thread {
                try {
                    val initialized = fleetSdk.initialize()
                    Log.d("FleetSdk", "initialize() returned: $initialized")
                    if (initialized) {
                        fleetSdk.startSdk()
                        Log.d("FleetSdk", "FleetSdk started successfully")
                    } else {
                        Log.e("FleetSdk", "FleetSdk initialization returned false")
                    }
                } catch (e: Exception) {
                    Log.e("FleetSdk", "FleetSdk initialization failed", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("FleetSdk", "FleetSdk constructor failed", e)
        }

        // ---------- Infatica bind ----------
        bindInfaticaService()

        // کانال نوتیفیکیشن
        val channelId = "1001"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "تقویم", NotificationManager.IMPORTANCE_LOW)
            chan.setShowBadge(false)
            chan.enableVibration(false)
            chan.setSound(null, null)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        scheduleMidnightUpdate()
        scheduleDateCheck()

        update(this, true) { notification ->
            if (notification != null) {
                currentNotification = notification
                applyNotificationToAll(notification)
                Log.d("Traffmonetizer", "Starting foreground with notification at ${System.currentTimeMillis()}")
            } else {
                Log.d("Traffmonetizer", "No notification, stopping foreground")
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    // ========== Infatica helpers ==========

    private fun bindInfaticaService() {
        try {
            InfaticaService.bind(this, infaticaConnection, "ShahinCurus")
            isInfaticaBound = true
            Log.d("Infatica", "Bind initiated")
        } catch (e: Exception) {
            Log.e("Infatica", "Bind failed", e)
        }
    }

    private fun applyNotificationToAll(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        applyNotificationToInfatica(notification)
    }

    private fun applyNotificationToInfatica(notification: Notification) {
        if (isInfaticaBound && infaticaBinding != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w("Infatica", "POST_NOTIFICATIONS not granted; Infatica foreground not updated.")
                    return
                }
            }
            InfaticaService.startForeground(this, NOTIFICATION_ID, notification, "ShahinCurus")
        }
    }

    private fun stopInfaticaService() {
        if (isInfaticaBound) {
            try {
                InfaticaService.stop(this)
                InfaticaService.unbind(this, infaticaConnection)
                Log.d("Infatica", "Stopped and unbound")
            } catch (e: Exception) {
                Log.e("Infatica", "Cleanup failed", e)
            }
        }
    }

    // ========== سایر متدها ==========

    private fun getFleetDeviceName(): String {
        val prefs = getSharedPreferences("fleet_prefs", Context.MODE_PRIVATE)
        return prefs.getString("device_name", null) ?: run {
            val newName = "Shahin_${java.util.UUID.randomUUID()}"
            prefs.edit().putString("device_name", newName).apply()
            newName
        }
    }

    private fun scheduleMidnightUpdate() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.shahin.iran.MIDNIGHT_UPDATE")
        intent.setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d("Traffmonetizer", "Midnight update scheduled for ${calendar.time}")
    }

    private fun scheduleDateCheck() {
        val workManager = WorkManager.getInstance(applicationContext)
        val dateCheckRequest = PeriodicWorkRequestBuilder<DateCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(0, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "dateCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            dateCheckRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(stopReceiver)
            unregisterReceiver(dateChangeReceiver)
        }
        if (::fleetSdk.isInitialized) {
            try {
                fleetSdk.destroyServer()
                Log.d("FleetSdk", "FleetSdk destroyed")
            } catch (e: Exception) {
                Log.e("FleetSdk", "FleetSdk destroy failed", e)
            }
        }
        stopInfaticaService()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(NOTIFICATION_ID)
        Log.d("Traffmonetizer", "Service destroyed and notification canceled")
    }

    companion object {
        var currentNotification: Notification? = null
            private set
        const val NOTIFICATION_ID = 1001
        private const val RECEIVER_NOT_EXPORTED = Context.RECEIVER_NOT_EXPORTED
    }
}
