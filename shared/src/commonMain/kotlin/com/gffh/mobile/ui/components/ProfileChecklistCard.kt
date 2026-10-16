package com.gffh.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.model.ProfileChecklistItem
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route

/**
 * Tells a manager what their team still needs. Being verified and having a
 * venue unlock sending and receiving friendly requests; the other items only
 * make the profile more useful to opponents. Draws nothing once there is
 * nothing left to show, or for teams the viewer doesn't manage (the API sends
 * an empty checklist then).
 *
 * [canManageClub] is the viewer's say over the team's club (see ActiveTeam):
 * only a club admin can add the club's venues, so anyone else is told to ask
 * theirs rather than sent to a form they couldn't save.
 */
@Composable
fun ProfileChecklistCard(
    team: TeamView,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    showOptional: Boolean = true,
    canManageClub: Boolean? = null
) {
    val required = team.profileChecklist.filter { it.requiredForRequests }
    val optionalMissing = team.profileChecklist.filter { !it.requiredForRequests && !it.done }
    val requiredMissing = required.any { !it.done }
    if (!requiredMissing && (!showOptional || optionalMissing.isEmpty())) return

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Finish setting up your team", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                if (requiredMissing) {
                    "Your team can't send or receive friendly requests until it's verified and has a venue." +
                        if (team.verification == "PENDING") " Your verification is being reviewed." else ""
                } else {
                    "Your team can send and receive friendly requests."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (requiredMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (requiredMissing) {
                Spacer(Modifier.height(8.dp))
                required.forEach { ChecklistItemRow(it, team, navigator, canManageClub) }
            }
            if (showOptional && optionalMissing.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Help opponents get to know you", style = MaterialTheme.typography.labelMedium)
                optionalMissing.forEach { ChecklistItemRow(it, team, navigator, canManageClub) }
            }
        }
    }
}

@Composable
private fun ChecklistItemRow(item: ProfileChecklistItem, team: TeamView, navigator: Navigator, canManageClub: Boolean?) {
    val route = checklistItemRoute(item, team, canManageClub)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (item.done) "Done" else "To do",
            modifier = Modifier.size(18.dp),
            tint = if (item.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(8.dp))
        Text(item.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (!item.done && route != null) {
            TextButton(onClick = { navigator.push(route) }) {
                Text(if (item.key == "VERIFICATION" && team.verification == "PENDING") "View" else "Do it")
            }
        }
    }
    if (!item.done && route == null) {
        // The venue item, for someone who isn't the club's admin: the club has no venues yet.
        Text(
            "Your club has no venues yet. Ask your club admin to add one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 26.dp, bottom = 4.dp)
        )
    }
}

/**
 * Where an unfinished item's "Do it" goes. Null for the venue item when
 * [canManageClub] is false: the API answers anyone but a club admin's venue
 * changes with "That club could not be found." Unknown (null) keeps the
 * button, as before the API said.
 */
fun checklistItemRoute(item: ProfileChecklistItem, team: TeamView, canManageClub: Boolean?): Route? = when (item.key) {
    "VERIFICATION" -> Route.Verification(team.id)
    "VENUE" -> if (canManageClub == false) null else Route.EditVenue(null, team.clubId)
    else -> Route.EditTeam(team.id)
}
