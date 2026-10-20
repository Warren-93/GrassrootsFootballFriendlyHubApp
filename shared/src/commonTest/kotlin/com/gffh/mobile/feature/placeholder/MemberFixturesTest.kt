package com.gffh.mobile.feature.placeholder

import com.gffh.mobile.model.FixtureTeamView
import com.gffh.mobile.model.FixtureView
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberFixturesTest {

    private val today = LocalDate(2026, 10, 16)

    @Test
    fun upcomingIsConfirmedFixturesFromTodaySoonestFirst() {
        val fixtures = listOf(
            fixture("later", date = "2026-11-07"),
            fixture("yesterday", date = "2026-10-15"),
            fixture("cancelled", date = "2026-10-24", status = "CANCELLED"),
            fixture("todayAfternoon", date = "2026-10-16", startTime = "14:00:00"),
            fixture("todayMorning", date = "2026-10-16", startTime = "09:30:00"),
            fixture("played", date = "2026-10-01", status = "COMPLETED")
        )

        assertEquals(
            listOf("todayMorning", "todayAfternoon", "later"),
            upcomingFixtures(fixtures, today).map { it.id }
        )
    }

    @Test
    fun upcomingIsEmptyWhenEveryConfirmedFixtureIsInThePast() {
        assertTrue(upcomingFixtures(listOf(fixture("past", date = "2026-10-15")), today).isEmpty())
    }

    @Test
    fun onlyFixturesOnPublishedAvailabilityDatesAreShown() {
        val fixtures = listOf(
            fixture("published", date = "2026-10-18"),
            fixture("unpublished", date = "2026-10-25"),
            fixture("sameDateDifferentTime", date = "2026-10-18", startTime = "14:00:00")
        )

        assertEquals(
            listOf("published", "sameDateDifferentTime"),
            fixturesMatchingPublishedAvailability(fixtures, setOf("2026-10-18")).map { it.id }
        )
    }

    @Test
    fun theHomeTeamIsTheOneHosting() {
        val f = fixture("f1", date = "2026-10-18")

        val home = f.sideOf("ours")!!
        assertTrue(home.isHome)
        assertEquals("theirs", home.opponent.id)

        val away = f.copy(homeTeam = f.awayTeam, awayTeam = f.homeTeam).sideOf("ours")!!
        assertFalse(away.isHome)
        assertEquals("theirs", away.opponent.id)
    }

    @Test
    fun aTeamNotPlayingHasNoSide() {
        assertNull(fixture("f1", date = "2026-10-18").sideOf("someoneElse"))
    }

    @Test
    fun datesReadAsWordsWithTheYearOnlyWhenItIsNotThisOne() {
        assertEquals("Saturday 17 October", fixtureDateLabel("2026-10-17", today))
        assertEquals("Sunday 3 January 2027", fixtureDateLabel("2027-01-03", today))
    }

    @Test
    fun aDateThatIsNotOneIsShownAsSent() {
        assertEquals("soon", fixtureDateLabel("soon", today))
    }

    @Test
    fun kickOffDropsTheSeconds() {
        assertEquals("10:00", kickOffLabel("10:00:00"))
        assertEquals("09:30", kickOffLabel("09:30"))
    }

    private fun fixture(
        id: String,
        date: String,
        startTime: String = "10:00:00",
        status: String = "CONFIRMED"
    ) = FixtureView(
        id = id,
        friendlyRequestId = "r-$id",
        homeTeam = FixtureTeamView(id = "ours", name = "United U11", clubName = "United"),
        awayTeam = FixtureTeamView(id = "theirs", name = "Rovers U11", clubName = "Rovers"),
        date = date,
        startTime = startTime,
        endTime = "11:30:00",
        status = status,
        costShare = "SPLIT",
        refereeArrangement = "NONE"
    )
}
