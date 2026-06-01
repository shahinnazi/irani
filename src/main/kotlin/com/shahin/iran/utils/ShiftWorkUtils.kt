package com.shahin.iran.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shahin.iran.R
import com.shahin.iran.entities.Jdn
import com.shahin.iran.global.shiftWorkPeriod
import com.shahin.iran.global.shiftWorkRecurs
import com.shahin.iran.global.shiftWorkStartingJdn
import com.shahin.iran.global.shiftWorkTitles
import com.shahin.iran.global.shiftWorks
import com.shahin.iran.global.spacedColon
import com.shahin.iran.global.spacedComma

fun getShiftWorkTitle(jdn: Jdn, abbreviated: Boolean = false): String? {
    val shiftWorkStartingJdn = shiftWorkStartingJdn ?: return null
    if (jdn < shiftWorkStartingJdn || shiftWorkPeriod == 0) return null

    val passedDays = jdn - shiftWorkStartingJdn
    if (!shiftWorkRecurs && passedDays >= shiftWorkPeriod) return null

    val dayInPeriod = passedDays % shiftWorkPeriod

    var accumulation = 0
    val type = shiftWorks.firstOrNull {
        accumulation += it.length
        accumulation > dayInPeriod
    }?.type ?: return null

    // Skip rests on abbreviated mode
    if (shiftWorkRecurs && abbreviated && (type == "r" || type == shiftWorkTitles["r"])) return null

    val title = shiftWorkTitles[type] ?: type
    return if (abbreviated && title.isNotEmpty() && title.length > 2) {
        title.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            .joinToString("/") { it.substring(0, 1) }
    } else title
}

@Composable
fun getShiftWorksInDaysDistance(jdn: Jdn): String? {
    if (shiftWorks.isEmpty()) return null
    val today = Jdn.today()
    if ((jdn - today) !in 1..365) return null
    val shiftWorksInDaysDistance = (today + 1..jdn).groupBy(::getShiftWorkTitle)
    if (shiftWorksInDaysDistance.size < 2 || null in shiftWorksInDaysDistance) return null
    return stringResource(R.string.days_distance) + spacedColon + shiftWorksInDaysDistance.entries.map { (title, days) ->
        pluralStringResource(
            R.plurals.days, days.size, formatNumber(days.size)
        ) + " " + title
    }.joinToString(spacedComma)
}
