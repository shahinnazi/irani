package com.shahin.iran.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import com.shahin.iran.variants.debugAssertNotNull
import com.shahin.iran.DEFAULT_ATHAN_CHANNEL_ID
import com.shahin.iran.KEY_EXTRA_PRAYER
import com.shahin.iran.PREF_ATHAN_CHANNEL_ID
import com.shahin.iran.R
import com.shahin.iran.entities.PrayTime
import com.shahin.iran.entities.PrayTime.Companion.get
import com.shahin.iran.global.athanVibration
import com.shahin.iran.global.calculationMethod
import com.shahin.iran.global.cityName
import com.shahin.iran.global.coordinates
import com.shahin.iran.global.notificationAthan
import com.shahin.iran.global.spacedComma
import com.shahin.iran.ui.athan.AthanActivity
import com.shahin.iran.ui.athan.AthanActivity.Companion.CANCEL_ATHAN_NOTIFICATION
import com.shahin.iran.ui.athan.PreventPhoneCallIntervention
import com.shahin.iran.utils.applyAppLanguage
import com.shahin.iran.utils.calculatePrayTimes
import com.shahin.iran.utils.getAthanUri
import com.shahin.iran.utils.logException
import com.shahin.iran.utils.preferences
import com.shahin.iran.utils.setDirection
import com.shahin.iran.utils.startAthanActivity
import kotlin.time.Duration.Companion.minutes

class AthanNotification : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_STICKY
        applyAppLanguage(this)

        val athanVibration = athanVibration.value
        val notificationAthan = notificationAthan.value
        val notificationId = currentChannelId(this)
        val notificationChannelId = "$notificationId"

        val notificationManager = getSystemService<NotificationManager>()

        val prayTime = PrayTime.fromName(
            intent.getStringExtra(KEY_EXTRA_PRAYER)
        ).debugAssertNotNull ?: PrayTime.FAJR
        if (!notificationAthan) startAthanActivity(this, prayTime)

        val soundUri = if (notificationAthan) getAthanUri(this) else null
        if (soundUri != null) runCatching {
            // ensure custom reminder sounds play well
            grantUriPermission(
                "com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }.onFailure(logException)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId, getString(R.string.athan),
                if (notificationAthan) NotificationManager.IMPORTANCE_HIGH
                else NotificationManager.IMPORTANCE_DEFAULT
            ).also {
                it.description = getString(R.string.athan)
                it.enableLights(true)
                it.lightColor = Color.GREEN
                if (athanVibration) it.vibrationPattern = LongArray(2) { 500 }
                it.enableVibration(athanVibration)
                it.setBypassDnd(prayTime.isBypassDnd)
                if (soundUri == null) it.setSound(null, null) else it.setSound(
                    soundUri, AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )
            }
            notificationManager?.createNotificationChannel(notificationChannel)
        }

        val cityName = cityName.value
        val prayTimeName = getString(prayTime.stringRes)
        val title =
            if (cityName == null) prayTimeName
            else "$prayTimeName$spacedComma${getString(R.string.in_city_time, cityName)}"

        val prayTimes = coordinates.value?.calculatePrayTimes()
        val isJafari = calculationMethod.value.isJafari
        val subtitle = prayTime.upcomingTimes(isJafari).joinToString(" - ") {
            "${getString(it.stringRes)}: ${prayTimes?.get(it)?.toFormattedString() ?: ""}"
        }

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        notificationBuilder.setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(prayTime.drawable)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, AthanActivity::class.java)
                        .setAction(CANCEL_ATHAN_NOTIFICATION)
                        .putExtra(KEY_EXTRA_PRAYER, prayTime)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )
            )

        if (notificationAthan) {
            notificationBuilder.priority = NotificationCompat.PRIORITY_MAX
            notificationBuilder.setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            notificationBuilder.setSound(null)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cv = RemoteViews(applicationContext?.packageName, R.layout.custom_notification)
            cv.setDirection(R.id.custom_notification_root, this.resources)
            cv.setTextViewText(R.id.title, title)
            if (subtitle.isEmpty()) {
                cv.setViewVisibility(R.id.body, View.GONE)
            } else {
                cv.setTextViewText(R.id.body, subtitle)
            }

            notificationBuilder
                .setCustomContentView(cv)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }

        notificationManager?.notify(notificationId, notificationBuilder.build())

        val preventPhoneCallIntervention =
            if (notificationAthan) PreventPhoneCallIntervention(cleanUp) else null
        cleanUp = {
            preventPhoneCallIntervention?.stopListener?.invoke()
            notificationManager?.cancel(notificationId)
        }

        preventPhoneCallIntervention?.startListener(this)
        Handler(Looper.getMainLooper()).postDelayed(6.minutes.inWholeMilliseconds) { cleanUp() }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
    }

    private var cleanUp = {}

    companion object {
        fun invalidateChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val current = context.preferences
                .getInt(PREF_ATHAN_CHANNEL_ID, DEFAULT_ATHAN_CHANNEL_ID)
            context.getSystemService<NotificationManager>()
                ?.deleteNotificationChannel("$current")
            context.preferences.edit { putInt(PREF_ATHAN_CHANNEL_ID, current + 1) }
        }

        private fun currentChannelId(context: Context): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return DEFAULT_ATHAN_CHANNEL_ID
            val preferences = context.preferences
            return if (PREF_ATHAN_CHANNEL_ID !in preferences) {
                context.getSystemService<NotificationManager>()?.let { nm ->
                    // Just clean up historical ids along the way
                    (3000..3003).forEach { nm.deleteNotificationChannel("$it") }
                }
                context.preferences.edit { putInt(PREF_ATHAN_CHANNEL_ID, DEFAULT_ATHAN_CHANNEL_ID) }
                DEFAULT_ATHAN_CHANNEL_ID
            } else context.preferences.getInt(PREF_ATHAN_CHANNEL_ID, DEFAULT_ATHAN_CHANNEL_ID)
        }
    }
}
