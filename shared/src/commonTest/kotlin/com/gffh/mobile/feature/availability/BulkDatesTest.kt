package com.gffh.mobile.feature.availability

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class BulkDatesTest {
    private val today = LocalDate(2026, 10, 23)

    private fun added(outcome: AddDateOutcome): List<LocalDate> =
        (outcome as? AddDateOutcome.Added)?.dates ?: error("Expected Added but was $outcome")

    @Test
    fun keepsPickedDatesInDateOrder() {
        var dates = emptyList<LocalDate>()
        for (text in listOf("2026-11-14", "2026-10-31", " 2026-11-07 ")) dates = added(addPickedDate(dates, text, today))
        assertEquals(listOf(LocalDate(2026, 10, 31), LocalDate(2026, 11, 7), LocalDate(2026, 11, 14)), dates)
    }

    @Test
    fun allowsTodayButRefusesThePast() {
        assertEquals(listOf(today), added(addPickedDate(emptyList(), "2026-10-23", today)))
        assertEquals(AddDateOutcome.Refused("That date has already passed."), addPickedDate(emptyList(), "2026-10-22", today))
    }

    @Test
    fun refusesDuplicates() {
        assertEquals(
            AddDateOutcome.Refused("That date is already in the list."),
            addPickedDate(listOf(LocalDate(2026, 10, 31)), "2026-10-31", today)
        )
    }

    @Test
    fun refusesUnparseableText() {
        assertEquals(
            AddDateOutcome.Refused("Enter the date as YYYY-MM-DD, e.g. 2026-10-23."),
            addPickedDate(emptyList(), "31/10/2026", today)
        )
    }

    @Test
    fun stopsAtTheApiLimit() {
        val full = (0 until MAX_BULK_DATES).map { today.plus(it, DateTimeUnit.DAY) }
        assertEquals(
            AddDateOutcome.Refused("You can publish up to 52 dates at once."),
            addPickedDate(full, "2028-01-01", today)
        )
    }

    @Test
    fun labelsIncludeTheWeekday() {
        assertEquals("Sat 31/10/2026", pickedDateLabel(LocalDate(2026, 10, 31)))
    }
}
