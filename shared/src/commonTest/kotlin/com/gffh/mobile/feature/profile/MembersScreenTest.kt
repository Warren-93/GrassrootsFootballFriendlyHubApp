package com.gffh.mobile.feature.profile

import com.gffh.mobile.model.MemberRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Which roles and member controls a club admin and a team manager get - see
 * assignableRoles and canChangeMember - and how the join code is spelled out
 * for a screen reader.
 */
class MembersScreenTest {

    @Test
    fun aClubAdminCanGiveEveryRole() {
        assertEquals(listOf(MemberRole.USER, MemberRole.TEAM_MANAGER, MemberRole.CLUB_ADMIN), assignableRoles(canManageClub = true))
    }

    @Test
    fun aTeamManagerIsNotOfferedClubAdmin() {
        assertEquals(listOf(MemberRole.USER, MemberRole.TEAM_MANAGER), assignableRoles(canManageClub = false))
    }

    @Test
    fun anOlderApiThatDoesntSayStillOffersEveryRole() {
        assertEquals(MemberRole.entries, assignableRoles(canManageClub = null))
    }

    @Test
    fun aTeamManagerCantChangeOrRemoveAClubAdmin() {
        assertFalse(canChangeMember(MemberRole.CLUB_ADMIN, canManageClub = false))
    }

    @Test
    fun aTeamManagerCanStillChangeOrRemovePlayersAndTeamManagers() {
        assertTrue(canChangeMember(MemberRole.USER, canManageClub = false))
        assertTrue(canChangeMember(MemberRole.TEAM_MANAGER, canManageClub = false))
    }

    @Test
    fun aClubAdminOrAnOlderApiCanChangeEveryone() {
        listOf(true, null).forEach { canManageClub ->
            MemberRole.entries.forEach { role -> assertTrue(canChangeMember(role, canManageClub)) }
        }
    }

    @Test
    fun aJoinCodeIsSpelledOutOneCharacterAtATime() {
        assertEquals("K X 7 P 3 Q", spelledOut("KX7P3Q"))
    }

    @Test
    fun anEmptyCodeSpellsOutAsNothing() {
        assertEquals("", spelledOut(""))
    }
}
