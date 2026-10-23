package com.gffh.mobile.feature.availability

import com.gffh.mobile.model.formatMatchDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The API accepts at most this many dates in one bulk publish. */
const val MAX_BULK_DATES = 52

internal fun parseDateOrNull(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim()) }.getOrNull()

internal fun datesMatching(from: LocalDate, to: LocalDate, days: Set<DayOfWeek>): List<LocalDate> {
    val result = mutableListOf<LocalDate>()
    var cursor = from
    while (cursor <= to) {
        if (cursor.dayOfWeek in days) result.add(cursor)
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return result
}

sealed interface AddDateOutcome {
    data class Added(val dates: List<LocalDate>) : AddDateOutcome
    data class Refused(val reason: String) : AddDateOutcome
}

/**
 * Adds one hand-picked date to the list, keeping it sorted and free of
 * duplicates. Past dates are refused here rather than left for the API to
 * skip, so the count on the Publish button is what actually gets published.
 */
fun addPickedDate(dates: List<LocalDate>, text: String, today: LocalDate): AddDateOutcome {
    val date = parseDateOrNull(text) ?: return AddDateOutcome.Refused("Enter the date as YYYY-MM-DD, e.g. $today.")
    return when {
        date < today -> AddDateOutcome.Refused("That date has already passed.")
        date in dates -> AddDateOutcome.Refused("That date is already in the list.")
        dates.size >= MAX_BULK_DATES -> AddDateOutcome.Refused("You can publish up to $MAX_BULK_DATES dates at once.")
        else -> AddDateOutcome.Added((dates + date).sorted())
    }
}

/** "Sat 31/10/2026" */
fun pickedDateLabel(date: LocalDate): String {
    val day = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$day ${formatMatchDate(date.toString())}"
}
