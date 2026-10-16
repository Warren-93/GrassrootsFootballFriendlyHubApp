package com.gffh.mobile.core.push

import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * What a tapped push is about: the data keys the API's NotificationDelivery
 * puts on every push, each absent when the notification has no such id.
 * [teamId] is the user's own team the notification was raised for.
 */
data class NotificationTap(
    val type: String? = null,
    val requestId: String? = null,
    val fixtureId: String? = null,
    val conversationId: String? = null,
    val teamId: String? = null
) {
    /**
     * The screen the push is about, picked in the same order as the API's
     * email links (NotificationDelivery.pathFor): a conversation, then a
     * fixture, then a request, then a team's verification. Null when it names
     * none of them - the app then opens the notification centre, as the
     * emails do.
     */
    fun route(): Route? = when {
        conversationId != null -> Route.ConversationThread(conversationId)
        fixtureId != null -> Route.FixtureDetail(fixtureId)
        requestId != null -> Route.RequestDetail(requestId)
        // VERIFICATION_APPROVED and VERIFICATION_REJECTED - the API's VERIFICATION category.
        type?.startsWith("VERIFICATION_") == true && teamId != null -> Route.Verification(teamId)
        else -> null
    }

    companion object {
        /** The data keys, spelt as the API's FcmSender and ApnsSender send them. */
        val KEYS = listOf("type", "requestId", "fixtureId", "conversationId", "teamId")

        /**
         * Reads a push's data - FCM's intent extras on Android, the APNs
         * payload's custom keys on iOS. Blank values count as absent. Null
         * when none of the keys is there, as for an ordinary app launch.
         */
        fun from(data: Map<String, String?>): NotificationTap? {
            fun value(key: String) = data[key]?.trim()?.ifEmpty { null }
            val tap = NotificationTap(
                type = value("type"),
                requestId = value("requestId"),
                fixtureId = value("fixtureId"),
                conversationId = value("conversationId"),
                teamId = value("teamId")
            )
            return tap.takeIf { it != NotificationTap() }
        }
    }
}

/**
 * Where the platform hands over a tapped push. The tap arrives outside any
 * screen - MainActivity's intent on Android, the AppDelegate on iOS, often
 * before [com.gffh.mobile.App] is even composed - so it waits here until App
 * has someone signed in, with a team, past the splash screen, and opens it.
 */
object NotificationTaps {

    private val _pending = MutableStateFlow<NotificationTap?>(null)

    /** The latest tap not opened yet. A newer tap replaces an older one. */
    val pending: StateFlow<NotificationTap?> = _pending

    /** Called with a launch's data; does nothing unless it came from a Kickabout push. */
    fun handle(data: Map<String, String?>) {
        NotificationTap.from(data)?.let { _pending.value = it }
    }

    /** Called once [tap] has been opened. Keeps a newer tap that arrived meanwhile. */
    fun consume(tap: NotificationTap) {
        _pending.compareAndSet(tap, null)
    }
}

/**
 * The team to act as before opening [tap], from `GET /api/v1/teams/mine`:
 * the one the push was raised for, when that isn't [activeTeamId] already
 * and the user manages it. Null means stay on the active team.
 */
fun teamToSwitchTo(tap: NotificationTap, activeTeamId: String?, myTeams: List<TeamView>): TeamView? {
    val teamId = tap.teamId ?: return null
    if (teamId == activeTeamId) return null
    return myTeams.firstOrNull { it.id == teamId && it.canManage != false }
}
