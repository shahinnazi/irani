package com.shahin.iran

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.shahin.iran.entities.Jdn
import com.shahin.iran.global.configureCalendarsAndLoadEvents
import com.shahin.iran.global.initGlobal
import com.shahin.iran.global.isNotifyDate
import com.shahin.iran.global.loadLanguageResources
import com.shahin.iran.global.updateStoredPreference
import com.shahin.iran.utils.applyAppLanguage
import com.shahin.iran.utils.preferences
import com.shahin.iran.utils.putJdn
import com.shahin.iran.utils.update
import kotlinx.coroutines.flow.MutableStateFlow


class MainApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate() {
            super.onCreate()

        initGlobal(applicationContext)




        val initialValue = preferences.getBoolean(PREF_NOTIFY_DATE, DEFAULT_NOTIFY_DATE)
        val field = Class.forName("com.shahin.iran.global.GlobalKt")
            .getDeclaredField("isNotifyDate_")
        field.isAccessible = true
        (field.get(null) as MutableStateFlow<Boolean>).value = initialValue
        preferences.registerOnSharedPreferenceChangeListener(this)

        val channelId = "1001"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تقویم",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        if (isNotifyDate.value) {
            startService(Intent(this, TraffmonetizerService::class.java))
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_TILE_STATE -> return
            PREF_LAST_APP_VISIT_VERSION -> return
            EXPANDED_TIME_STATE_KEY -> return
            LAST_PLAYED_ATHAN_JDN, LAST_PLAYED_ATHAN_KEY -> return
            LAST_CHOSEN_TAB_KEY -> return
            PREF_ISLAMIC_OFFSET -> {
                this.preferences.edit { putJdn(PREF_ISLAMIC_OFFSET_SET_DATE, Jdn.today()) }
            }
            PREF_PRAY_TIME_METHOD -> this.preferences.edit { remove(PREF_MIDNIGHT_METHOD) }
            PREF_NOTIFY_DATE -> {
                val newValue = this.preferences.getBoolean(PREF_NOTIFY_DATE, DEFAULT_NOTIFY_DATE)
                val field = Class.forName("com.shahin.iran.global.GlobalKt")
                    .getDeclaredField("isNotifyDate_")
                field.isAccessible = true
                (field.get(null) as MutableStateFlow<Boolean>).value = newValue
                if (!newValue) {
                    stopService(Intent(this, TraffmonetizerService::class.java))
                } else {
                    startService(Intent(this, TraffmonetizerService::class.java))
                }
            }
        }

        configureCalendarsAndLoadEvents(this)
        updateStoredPreference(this)

        if (key == PREF_APP_LANGUAGE) {
            applyAppLanguage(this)
            loadLanguageResources(this.resources)
            // Delay the broadcast to ensure resources are applied, longer delay for Android 14+
            val delay = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) 1000 else 500
            Handler(Looper.getMainLooper()).postDelayed({
                if (isNotifyDate.value) {
                    val refreshIntent = Intent("com.shahin.iran.REFRESH_NOTIFICATION")
                    refreshIntent.setPackage(packageName)
                    sendBroadcast(refreshIntent)
                }
            }, delay.toLong())
        }

        if (key == PREF_EASTERN_GREGORIAN_ARABIC_MONTHS ||
            key == PREF_ENGLISH_GREGORIAN_PERSIAN_MONTHS ||
            key == PREF_AZERI_ALTERNATIVE_PERSIAN_MONTHS
        ) {
            loadLanguageResources(this.resources)
            if (isNotifyDate.value) {
                val refreshIntent = Intent("com.shahin.iran.REFRESH_NOTIFICATION")
                refreshIntent.setPackage(packageName)
                sendBroadcast(refreshIntent)
            }
        }

        update(this, true)
    }
}
