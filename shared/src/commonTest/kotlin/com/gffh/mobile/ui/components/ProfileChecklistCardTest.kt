package com.gffh.mobile.ui.components

import com.gffh.mobile.model.ProfileChecklistItem
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Where each unfinished item's "Do it" goes - see checklistItemRoute. */
class ProfileChecklistCardTest {

    private val team = TeamView(
        id = "t1",
        clubId = "c1",
        name = "Denny Warriors U12",
        clubName = "Denny FC",
        ageGroup = "U12",
        gender = "MIXED",
        format = "NINE_A_SIDE",
        abilityLevel = "INTERMEDIATE",
        postcode = "FK6 5AA",
        longitude = -3.9,
        latitude = 56.0,
        travelRadiusMiles = 15,
        homeAwayPreference = "EITHER",
        verification = "UNVERIFIED",
        completenessPercent = 40,
        createdAt = "2026-10-01T10:00:00Z"
    )

    private val venue = ProfileChecklistItem("VENUE", "Add a venue", done = false, requiredForRequests = true)

    @Test
    fun aClubAdminIsSentToAddAVenue() {
        assertEquals(Route.EditVenue(null, "c1"), checklistItemRoute(venue, team, canManageClub = true))
    }

    @Test
    fun anOlderApiThatDoesntSayStillSendsThemToAddAVenue() {
        assertEquals(Route.EditVenue(null, "c1"), checklistItemRoute(venue, team, canManageClub = null))
    }

    @Test
    fun aTeamManagerIsNotSentToAFormTheyCouldntSave() {
        assertNull(checklistItemRoute(venue, team, canManageClub = false))
    }

    @Test
    fun theOtherItemsGoWhereTheyAlwaysHaveForAnyManager() {
        val verification = ProfileChecklistItem("VERIFICATION", "Get your team verified", done = false, requiredForRequests = true)
        val badge = ProfileChecklistItem("BADGE", "Upload a team badge", done = false, requiredForRequests = false)

        listOf(true, false, null).forEach { canManageClub ->
            assertEquals(Route.Verification("t1"), checklistItemRoute(verification, team, canManageClub))
            assertEquals(Route.EditTeam("t1"), checklistItemRoute(badge, team, canManageClub))
        }
    }
}
