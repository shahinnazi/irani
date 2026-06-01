package com.shahin.iran.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.shahin.iran.DEFAULT_ATHAN_VOLUME
import com.shahin.iran.DEFAULT_CITY
import com.shahin.iran.PREF_ALTITUDE
import com.shahin.iran.PREF_APP_LANGUAGE
import com.shahin.iran.PREF_ASR_HANAFI_JURISTIC
import com.shahin.iran.PREF_ATHAN_VOLUME
import com.shahin.iran.PREF_GEOCODED_CITYNAME
import com.shahin.iran.PREF_HOLIDAY_TYPES
import com.shahin.iran.PREF_ISLAMIC_OFFSET_SET_DATE
import com.shahin.iran.PREF_LATITUDE
import com.shahin.iran.PREF_LOCAL_DIGITS
import com.shahin.iran.PREF_LONGITUDE
import com.shahin.iran.PREF_MAIN_CALENDAR_KEY
import com.shahin.iran.PREF_OTHER_CALENDARS_KEY
import com.shahin.iran.PREF_PRAY_TIME_METHOD
import com.shahin.iran.PREF_SELECTED_LOCATION
import com.shahin.iran.PREF_WEEK_ENDS
import com.shahin.iran.PREF_WEEK_START
import com.shahin.iran.entities.CityItem
import com.shahin.iran.entities.EventsRepository
import com.shahin.iran.entities.Jdn
import com.shahin.iran.entities.Language
import com.shahin.iran.global.overrideCoordinatesGlobalVariable
import io.github.persiancalendar.praytimes.Coordinates
import java.util.Locale

// Instead of:
//   androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
// Per https://stackoverflow.com/a/62897591
val Context.preferences: SharedPreferences
    get() = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

fun SharedPreferences.Editor.putJdn(key: String, jdn: Jdn) {
    putLong(key, jdn.value)
}

fun SharedPreferences.getJdnOrNull(key: String): Jdn? =
    getLong(key, -1).takeIf { it != -1L }?.let(::Jdn)

// Ignore offset if it isn't set in less than month ago
val SharedPreferences.isIslamicOffsetExpired
    get() = getJdnOrNull(PREF_ISLAMIC_OFFSET_SET_DATE)?.let { Jdn.today() - it > 30 } != false

val SharedPreferences.athanVolume: Int get() = getInt(PREF_ATHAN_VOLUME, DEFAULT_ATHAN_VOLUME)

fun SharedPreferences.saveCity(city: CityItem) = edit {
    listOf(PREF_GEOCODED_CITYNAME, PREF_LATITUDE, PREF_LONGITUDE, PREF_ALTITUDE).forEach(::remove)
    putString(PREF_SELECTED_LOCATION, city.key)
}

fun SharedPreferences.saveLocation(
    coordinates: Coordinates, cityName: String, countryCode: String = "IR"
) {
    edit {
        putString(PREF_LATITUDE, "%f".format(Locale.ENGLISH, coordinates.latitude))
        putString(PREF_LONGITUDE, "%f".format(Locale.ENGLISH, coordinates.longitude))
        // Don't store elevation on Iranian cities, it degrades the calculations quality
        val elevation = if (countryCode == "IR") .0 else coordinates.elevation
        putString(PREF_ALTITUDE, "%f".format(Locale.ENGLISH, elevation))
        putString(PREF_GEOCODED_CITYNAME, cityName)
        putString(PREF_SELECTED_LOCATION, DEFAULT_CITY)
    }
    overrideCoordinatesGlobalVariable(coordinates)
}

// Preferences changes be applied automatically when user requests a language change
fun SharedPreferences.saveLanguage(language: Language) = edit {
    putString(PREF_APP_LANGUAGE, language.code)
    putBoolean(PREF_LOCAL_DIGITS, language.prefersLocalDigits)

    when {
        language.isAfghanistanExclusive -> {
            val enabledHolidays = EventsRepository(this@saveLanguage, language, emptySet())
            if (enabledHolidays.isEmpty || enabledHolidays.onlyIranHolidaysIsEnabled)
                putStringSet(PREF_HOLIDAY_TYPES, EventsRepository.afghanistanDefault)
        }

        language.isIranExclusive -> {
            val enabledHolidays = EventsRepository(this@saveLanguage, language, emptySet())
            if (enabledHolidays.isEmpty || enabledHolidays.onlyAfghanistanHolidaysIsEnabled)
                putStringSet(PREF_HOLIDAY_TYPES, EventsRepository.iranDefault)
        }

        language.isNepali -> {
            putStringSet(PREF_HOLIDAY_TYPES, EventsRepository.nepalDefault)
        }
    }

    putString(PREF_MAIN_CALENDAR_KEY, language.defaultMainCalendar)
    putString(PREF_OTHER_CALENDARS_KEY, language.defaultOtherCalendars)
    putString(PREF_WEEK_START, language.defaultWeekStart)
    putStringSet(PREF_WEEK_ENDS, language.defaultWeekEnds)

    putString(PREF_PRAY_TIME_METHOD, language.preferredCalculationMethod.name)
    putBoolean(PREF_ASR_HANAFI_JURISTIC, language.isHanafiMajority)
}
