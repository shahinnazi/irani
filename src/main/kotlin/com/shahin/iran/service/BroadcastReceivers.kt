package com.shahin.iran.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast
import com.shahin.iran.variants.debugLog
import com.shahin.iran.ADD_EVENT
import com.shahin.iran.BROADCAST_ALARM
import com.shahin.iran.BROADCAST_RESTART_APP
import com.shahin.iran.BROADCAST_UPDATE_APP
import com.shahin.iran.KEY_EXTRA_PRAYER
import com.shahin.iran.KEY_EXTRA_PRAYER_TIME
import com.shahin.iran.MONTH_NEXT_COMMAND
import com.shahin.iran.MONTH_PREV_COMMAND
import com.shahin.iran.MONTH_RESET_COMMAND
import com.shahin.iran.R
import com.shahin.iran.entities.PrayTime
import com.shahin.iran.ui.calendar.AddEventData
import com.shahin.iran.utils.logException
import com.shahin.iran.utils.startAthan
import com.shahin.iran.utils.startWorker
import com.shahin.iran.utils.update
import com.shahin.iran.utils.updateMonthWidget

class BroadcastReceivers : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        when (val action = intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            TelephonyManager.ACTION_PHONE_STATE_CHANGED,
            BROADCAST_RESTART_APP -> startWorker(context)

            Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> update(context, true)
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_SCREEN_ON, BROADCAST_UPDATE_APP ->
                update(context, false)

            ADD_EVENT -> runCatching {
                val addEventIntent = AddEventData.upcoming().asIntent()
                context.startActivity(addEventIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure(logException).onFailure {
                Toast.makeText(context, R.string.device_does_not_support, Toast.LENGTH_SHORT).show()
            }

            BROADCAST_ALARM -> {
                val key = PrayTime.fromName(intent.getStringExtra(KEY_EXTRA_PRAYER)) ?: return
                val intendedTime = intent.getLongExtra(KEY_EXTRA_PRAYER_TIME, 0).takeIf { it != 0L }
                debugLog("Alarms: AlarmManager for $key")
                startAthan(context, key, intendedTime)
            }

            null -> Unit
            else -> {
                if (action.startsWith(MONTH_PREV_COMMAND)) {
                    action.replace(MONTH_PREV_COMMAND, "").toIntOrNull()?.let { id ->
                        updateMonthWidget(context, id, -1)
                    }
                } else if (action.startsWith(MONTH_NEXT_COMMAND)) {
                    action.replace(MONTH_NEXT_COMMAND, "").toIntOrNull()?.let { id ->
                        updateMonthWidget(context, id, 1)
                    }
                } else if (action.startsWith(MONTH_RESET_COMMAND)) {
                    action.replace(MONTH_RESET_COMMAND, "").toIntOrNull()?.let { id ->
                        updateMonthWidget(context, id, 0)
                    }
                }
            }
        }
    }
}
