package com.gffh.mobile.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDateFormattingTest {
    @Test
    fun formatsIsoDatesAsDayMonthYear() {
        assertEquals("22/08/2026", formatMatchDate("2026-08-22"))
    }

    @Test
    fun keepsUnexpectedDateStringsReadable() {
        assertEquals("date unavailable", formatMatchDate("date unavailable"))
    }
}

