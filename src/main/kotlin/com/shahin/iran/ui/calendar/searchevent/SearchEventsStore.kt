package com.shahin.iran.ui.calendar.searchevent

import com.shahin.iran.ZWNJ
import com.shahin.iran.entities.CalendarEvent

class SearchEventsStore(val events: List<CalendarEvent<*>>) {
    private val delimiters = arrayOf(" ", "(", ")", "-", ZWNJ)
    private val itemsWords = events.map { it to it.formattedTitle.split(*delimiters) }

    fun query(constraint: CharSequence?): List<CalendarEvent<*>> {
        return if (constraint == null) events
        else itemsWords.mapNotNull { (event: CalendarEvent<*>, words: List<String>) ->
            event.takeIf { words.any { word -> word.startsWith(constraint) } }
        }
    }
}
