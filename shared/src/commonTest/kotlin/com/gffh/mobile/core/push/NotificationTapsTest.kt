package com.gffh.mobile.core.push

import com.gffh.mobile.model.NotificationView
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The data keys and their precedence follow the API's NotificationDelivery.pushMessage and pathFor. */
class NotificationTapsTest {

    @Test
    fun readsEveryKeyThePushCarries() {
        val tap = NotificationTap.from(
            mapOf(
                "type" to "FIXTURE_CONFIRMED",
                "requestId" to "r1",
                "fixtureId" to "f1",
                "teamId" to "t1",
                // FCM's own extras ride along on Android; they're not ours to read.
                "google.message_id" to "0:123",
                "from" to "1234567890"
            )
        )

        assertEquals(NotificationTap(type = "FIXTURE_CONFIRMED", requestId = "r1", fixtureId = "f1", teamId = "t1"), tap)
    }

    @Test
    fun anOrdinaryLaunchIsNotATap() {
        assertNull(NotificationTap.from(emptyMap()))
        assertNull(NotificationTap.from(mapOf("google.message_id" to "0:123")))
    }

    @Test
    fun blankAndMissingValuesCountAsAbsent() {
        val tap = NotificationTap.from(mapOf("type" to "REQUEST_RECEIVED", "requestId" to " r1 ", "fixtureId" to "", "teamId" to null))

        assertEquals(NotificationTap(type = "REQUEST_RECEIVED", requestId = "r1"), tap)
    }

    @Test
    fun aConversationWinsOverEverythingElse() {
        val tap = NotificationTap("MESSAGE_RECEIVED", requestId = "r1", fixtureId = "f1", conversationId = "c1", teamId = "t1")

        assertEquals(Route.ConversationThread("c1"), tap.route())
    }

    @Test
    fun aConfirmedFixtureOpensTheFixtureRatherThanItsRequest() {
        val tap = NotificationTap("FIXTURE_CONFIRMED", requestId = "r1", fixtureId = "f1", teamId = "t1")

        assertEquals(Route.FixtureDetail("f1"), tap.route())
    }

    @Test
    fun aRequestNotificationOpensTheRequest() {
        val tap = NotificationTap("REQUEST_CHANGES_REQUESTED", requestId = "r1", teamId = "t1")

        assertEquals(Route.RequestDetail("r1"), tap.route())
    }

    @Test
    fun aVerificationOutcomeOpensThatTeamsVerification() {
        assertEquals(Route.Verification("t1"), NotificationTap("VERIFICATION_APPROVED", teamId = "t1").route())
        assertEquals(Route.Verification("t1"), NotificationTap("VERIFICATION_REJECTED", teamId = "t1").route())
    }

    @Test
    fun aTeamAloneOnlyMeansVerificationForAVerificationType() {
        assertNull(NotificationTap("REQUEST_RECEIVED", teamId = "t1").route())
        assertNull(NotificationTap("VERIFICATION_APPROVED").route())
    }

    // The notification centre opens an item the same way, from its stored ids.

    @Test
    fun aNotificationInTheCentreIsTheSameTapAsItsPush() {
        val pushed = NotificationTap.from(
            mapOf("type" to "FIXTURE_CONFIRMED", "requestId" to "r1", "fixtureId" to "f1", "teamId" to "t1")
        )
        val listed = NotificationTap.of(
            notification("FIXTURE_CONFIRMED", teamId = "t1", requestId = "r1", fixtureId = "f1")
        )

        assertEquals(pushed, listed)
        assertEquals(Route.FixtureDetail("f1"), listed.route())
    }

    @Test
    fun aVerificationOutcomeInTheCentreOpensThatTeamsVerification() {
        val tap = NotificationTap.of(notification("VERIFICATION_REJECTED", teamId = "t2"))

        assertEquals(Route.Verification("t2"), tap.route())
        assertEquals("t2", tap.teamId)
    }

    @Test
    fun aMessageInTheCentreOpensItsConversation() {
        val tap = NotificationTap.of(notification("MESSAGE_RECEIVED", teamId = "t1", conversationId = "c1"))

        assertEquals(Route.ConversationThread("c1"), tap.route())
    }

    @Test
    fun aNotificationInTheCentreNamingNoScreenOpensNothing() {
        assertNull(NotificationTap.of(notification("VERIFICATION_APPROVED")).route())
    }

    @Test
    fun switchesToTheTeamThePushIsAboutWhenTheUserManagesIt() {
        val tap = NotificationTap("REQUEST_RECEIVED", requestId = "r1", teamId = "t2")
        val teams = listOf(team("t1"), team("t2", canManage = true))

        assertEquals("t2", teamToSwitchTo(tap, activeTeamId = "t1", myTeams = teams)?.id)
    }

    @Test
    fun aTeamFromTeamsMineWithNoCanManageFlagCountsAsManaged() {
        val tap = NotificationTap("REQUEST_RECEIVED", requestId = "r1", teamId = "t2")

        assertEquals("t2", teamToSwitchTo(tap, activeTeamId = "t1", myTeams = listOf(team("t2", canManage = null)))?.id)
    }

    @Test
    fun staysOnTheActiveTeamOtherwise() {
        val tap = NotificationTap("REQUEST_RECEIVED", requestId = "r1", teamId = "t2")

        // Already on it.
        assertNull(teamToSwitchTo(tap, activeTeamId = "t2", myTeams = listOf(team("t2"))))
        // Only a member of it.
        assertNull(teamToSwitchTo(tap, activeTeamId = "t1", myTeams = listOf(team("t1"), team("t2", canManage = false))))
        // Not one of theirs (any more).
        assertNull(teamToSwitchTo(tap, activeTeamId = "t1", myTeams = listOf(team("t1"))))
        // The push named no team.
        assertNull(teamToSwitchTo(tap.copy(teamId = null), activeTeamId = "t1", myTeams = listOf(team("t2"))))
    }

    // NotificationTaps is process-wide (the platform hands taps over outside
    // any screen), so each test leaves it empty again.

    @Test
    fun aTapWaitsUntilItIsConsumed() {
        NotificationTaps.handle(mapOf("type" to "REQUEST_RECEIVED", "requestId" to "r1"))
        val tap = NotificationTaps.pending.value

        assertEquals(NotificationTap(type = "REQUEST_RECEIVED", requestId = "r1"), tap)
        NotificationTaps.consume(tap!!)
        assertNull(NotificationTaps.pending.value)
    }

    @Test
    fun consumingAnOlderTapKeepsANewerOne() {
        NotificationTaps.handle(mapOf("type" to "REQUEST_RECEIVED", "requestId" to "r1"))
        val first = NotificationTaps.pending.value!!
        NotificationTaps.handle(mapOf("type" to "MESSAGE_RECEIVED", "conversationId" to "c1"))

        NotificationTaps.consume(first)

        assertEquals(NotificationTap(type = "MESSAGE_RECEIVED", conversationId = "c1"), NotificationTaps.pending.value)
        NotificationTaps.consume(NotificationTaps.pending.value!!)
    }

    @Test
    fun anOrdinaryLaunchLeavesAWaitingTapAlone() {
        NotificationTaps.handle(mapOf("type" to "FIXTURE_CANCELLED", "requestId" to "r1"))

        NotificationTaps.handle(emptyMap())

        assertEquals(NotificationTap(type = "FIXTURE_CANCELLED", requestId = "r1"), NotificationTaps.pending.value)
        NotificationTaps.consume(NotificationTaps.pending.value!!)
    }

    private fun notification(
        type: String,
        teamId: String? = null,
        requestId: String? = null,
        fixtureId: String? = null,
        conversationId: String? = null
    ) = NotificationView(
        id = "n1",
        type = type,
        title = "Title",
        body = "Body",
        relatedTeamId = teamId,
        relatedRequestId = requestId,
        relatedFixtureId = fixtureId,
        relatedConversationId = conversationId,
        read = false,
        createdAt = "2026-10-16T09:00:00Z"
    )

    private fun team(id: String, canManage: Boolean? = true) = TeamView(
        id = id,
        clubId = "club",
        name = "Team $id",
        clubName = "Club",
        ageGroup = "U11",
        gender = "MIXED",
        format = "SEVEN_A_SIDE",
        abilityLevel = "INTERMEDIATE",
        postcode = "AB1 2CD",
        longitude = -1.0,
        latitude = 52.0,
        travelRadiusMiles = 10,
        homeAwayPreference = "EITHER",
        verification = "VERIFIED",
        completenessPercent = 100,
        createdAt = "2026-10-01T10:00:00Z",
        canManage = canManage
    )
}
