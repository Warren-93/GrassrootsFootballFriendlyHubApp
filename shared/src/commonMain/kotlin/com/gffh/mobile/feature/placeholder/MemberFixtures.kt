package com.gffh.mobile.feature.placeholder

import com.gffh.mobile.model.FixtureTeamView
import com.gffh.mobile.model.FixtureView
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

/** A fixture from one team's point of view: who they're playing, and whether they're the hosts. */
data class FixtureSide(val opponent: FixtureTeamView, val isHome: Boolean)

/** [teamId]'s side of this fixture - the home team is the one hosting it - or null when it isn't playing in it. */
fun FixtureView.sideOf(teamId: String): FixtureSide? = when (teamId) {
    homeTeam.id -> FixtureSide(awayTeam, isHome = true)
    awayTeam.id -> FixtureSide(homeTeam, isHome = false)
    else -> null
}

/**
 * The confirmed fixtures still to come, from [today] on, soonest first.
 * The API's dates are ISO (2026-10-18) and its times 24-hour, so both
 * compare correctly as text.
 */
fun upcomingFixtures(fixtures: List<FixtureView>, today: LocalDate): List<FixtureView> {
    val from = today.toString()
    return fixtures
        .filter { it.status == "CONFIRMED" && it.date >= from }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
}

/**
 * "Saturday 18 October", with the year only when it isn't [today]'s. The
 * API's text unchanged if it isn't a date.
 */
fun fixtureDateLabel(date: String, today: LocalDate): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    val label = "${DAY_NAMES[parsed.dayOfWeek.isoDayNumber - 1]} ${parsed.day} ${MONTH_NAMES[parsed.month.number - 1]}"
    return if (parsed.year == today.year) label else "$label ${parsed.year}"
}

/** "10:00" from the API's "10:00:00". */
fun kickOffLabel(time: String): String = time.take(5)

private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
