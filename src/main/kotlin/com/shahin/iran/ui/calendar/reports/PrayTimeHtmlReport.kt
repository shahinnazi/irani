package com.shahin.iran.ui.calendar.reports

import android.content.res.Resources
import androidx.annotation.CheckResult
import com.shahin.iran.R
import com.shahin.iran.entities.Jdn
import com.shahin.iran.entities.PrayTime
import com.shahin.iran.entities.PrayTime.Companion.get
import com.shahin.iran.global.calculationMethod
import com.shahin.iran.global.cityName
import com.shahin.iran.global.coordinates
import com.shahin.iran.global.language
import com.shahin.iran.global.mainCalendar
import com.shahin.iran.global.spacedComma
import com.shahin.iran.ui.utils.isRtl
import com.shahin.iran.utils.calculatePrayTimes
import com.shahin.iran.utils.formatNumber
import com.shahin.iran.utils.monthName
import com.shahin.iran.utils.titleStringId
import io.github.persiancalendar.calendar.AbstractDate
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tfoot
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.html.unsafe

@CheckResult
fun prayTimeHtmlReport(resources: Resources, date: AbstractDate): String {
    return createHTML().html {
        val coordinates = coordinates.value ?: return@html
        attributes["lang"] = language.value.language
        attributes["dir"] = if (resources.isRtl) "rtl" else "ltr"
        head {
            meta(charset = "utf8")
            style {
                unsafe {
                    +"""
                        body { font-family: system-ui }
                        th, td { padding: 0 .5em; text-align: center }
                        td { border-top: 1px solid lightgray; font-size: 95% }
                        h1 { text-align: center; font-size: 110% }
                        table { margin: 0 auto; }
                    """.trimIndent()
                }
            }
        }
        body {
            h1 {
                +listOfNotNull(
                    cityName.value,
                    language.value.my.format(date.monthName, formatNumber(date.year))
                ).joinToString(spacedComma)
            }
            table {
                val prayTimeList = PrayTime.allTimes(calculationMethod.value.isJafari)
                thead {
                    tr {
                        th { +resources.getString(R.string.day) }
                        prayTimeList.forEach { th { +resources.getString(it.stringRes) } }
                    }
                }
                tbody {
                    (0..<mainCalendar.getMonthLength(date.year, date.month)).forEach { day ->
                        tr {
                            val prayTimes = coordinates.calculatePrayTimes(
                                Jdn(
                                    mainCalendar.createDate(date.year, date.month, day)
                                ).toGregorianCalendar()
                            )
                            th { +formatNumber(day + 1) }
                            prayTimeList.forEach {
                                td { +prayTimes[it].toBasicFormatString() }
                            }
                        }
                    }
                }
                if (calculationMethod.value != language.value.preferredCalculationMethod) {
                    tfoot {
                        tr {
                            td {
                                colSpan = "10"
                                +resources.getString(calculationMethod.value.titleStringId)
                            }
                        }
                    }
                }
            }
            script { unsafe { +"print()" } }
        }
    }
}
