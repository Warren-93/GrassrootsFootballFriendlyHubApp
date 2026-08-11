package com.gffh.mobile.session

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ActiveTeam(val teamId: String, val clubId: String, val teamName: String)

/**
 * The team every tab-root screen (Dashboard, Calendar) implicitly operates on.
 *
 * <p>The Screen Build Specification's team switcher (SCR-HM-01) assumes a
 * "list every team the user holds a role on" endpoint that doesn't exist yet
 * on the backend - see gffh-mobile/README.md. Until that lands, this holds
 * only the single most-recently-created-or-opened team, set at the end of
 * onboarding and whenever a team profile is opened.
 */
class CurrentTeamStore(private val settings: Settings) {

    private val _active = MutableStateFlow(load())
    val active: StateFlow<ActiveTeam?> = _active

    private fun load(): ActiveTeam? {
        val teamId = settings.get<String>(KEY_TEAM_ID) ?: return null
        val clubId = settings.get<String>(KEY_CLUB_ID) ?: return null
        val name = settings.get<String>(KEY_TEAM_NAME) ?: return null
        return ActiveTeam(teamId, clubId, name)
    }

    fun set(team: ActiveTeam) {
        settings.putString(KEY_TEAM_ID, team.teamId)
        settings.putString(KEY_CLUB_ID, team.clubId)
        settings.putString(KEY_TEAM_NAME, team.teamName)
        _active.value = team
    }

    fun clear() {
        settings.remove(KEY_TEAM_ID)
        settings.remove(KEY_CLUB_ID)
        settings.remove(KEY_TEAM_NAME)
        _active.value = null
    }

    private companion object {
        const val KEY_TEAM_ID = "active_team_id"
        const val KEY_CLUB_ID = "active_club_id"
        const val KEY_TEAM_NAME = "active_team_name"
    }
}
