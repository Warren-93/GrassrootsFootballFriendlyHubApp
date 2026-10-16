package com.gffh.mobile.session

import com.gffh.mobile.model.TeamView
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [canManage] is false for a member who joined with a code (role USER): the API
 * won't let them publish availability, search, arrange or message, so the app
 * shows them a read-only view instead of manager screens that come back empty.
 *
 * [canManageClub] is false for anyone but the club's admin (a team manager,
 * say): they can see the club's venues but not add, edit, make default or
 * delete them. Null until `GET /api/v1/teams/mine` has said, or when an older
 * API doesn't - the app then shows venues as it always has.
 */
data class ActiveTeam(
    val teamId: String,
    val clubId: String,
    val teamName: String,
    val canManage: Boolean = true,
    val canManageClub: Boolean? = null
)

/** The active team as `GET /api/v1/teams/mine` describes it. */
fun TeamView.toActiveTeam(): ActiveTeam = ActiveTeam(id, clubId, name, canManage ?: true, canManageClub)

/**
 * Whether the user can change [clubId]'s venues, as far as the active team
 * says: null when it doesn't know, or the club isn't the active team's.
 */
fun ActiveTeam?.canManageClubOf(clubId: String): Boolean? = this?.takeIf { it.clubId == clubId }?.canManageClub

/**
 * The team every tab-root screen (Dashboard, Calendar) implicitly operates on.
 *
 * <p>Set at the end of onboarding and whenever a team profile is opened, and
 * reconciled against `GET /api/v1/teams/mine` on the dashboard - see
 * HomeScreen's team-switcher dropdown, shown whenever that call returns more
 * than one team.
 */
class CurrentTeamStore(private val settings: Settings) {

    private val _active = MutableStateFlow(load())
    val active: StateFlow<ActiveTeam?> = _active

    private fun load(): ActiveTeam? {
        val teamId = settings.get<String>(KEY_TEAM_ID) ?: return null
        val clubId = settings.get<String>(KEY_CLUB_ID) ?: return null
        val name = settings.get<String>(KEY_TEAM_NAME) ?: return null
        return ActiveTeam(
            teamId, clubId, name,
            canManage = settings.getBoolean(KEY_CAN_MANAGE, true),
            canManageClub = settings.getBooleanOrNull(KEY_CAN_MANAGE_CLUB)
        )
    }

    fun set(team: ActiveTeam) {
        settings.putString(KEY_TEAM_ID, team.teamId)
        settings.putString(KEY_CLUB_ID, team.clubId)
        settings.putString(KEY_TEAM_NAME, team.teamName)
        settings.putBoolean(KEY_CAN_MANAGE, team.canManage)
        // Not knowing is kept as not knowing, rather than read back as a no.
        val canManageClub = team.canManageClub
        if (canManageClub != null) settings.putBoolean(KEY_CAN_MANAGE_CLUB, canManageClub) else settings.remove(KEY_CAN_MANAGE_CLUB)
        _active.value = team
    }

    fun clear() {
        settings.remove(KEY_TEAM_ID)
        settings.remove(KEY_CLUB_ID)
        settings.remove(KEY_TEAM_NAME)
        settings.remove(KEY_CAN_MANAGE)
        settings.remove(KEY_CAN_MANAGE_CLUB)
        _active.value = null
    }

    private companion object {
        const val KEY_TEAM_ID = "active_team_id"
        const val KEY_CLUB_ID = "active_club_id"
        const val KEY_TEAM_NAME = "active_team_name"
        const val KEY_CAN_MANAGE = "active_team_can_manage"
        const val KEY_CAN_MANAGE_CLUB = "active_team_can_manage_club"
    }
}
