package com.shahin.iran.variants

import android.util.Log
import com.shahin.iran.LOG_TAG

fun debugLog(vararg message: Any?) {
    Log.d(LOG_TAG, message.joinToString(", "))
}

inline val <T> T.debugAssertNotNull: T inline get() = checkNotNull(this)
