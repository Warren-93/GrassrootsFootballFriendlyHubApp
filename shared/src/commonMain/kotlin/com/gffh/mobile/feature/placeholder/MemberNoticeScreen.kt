package com.gffh.mobile.feature.placeholder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.session.ActiveTeam

/**
 * Shown in place of manager screens to someone who joined a team with a join
 * code: they're a member (role USER), and the API won't let them publish
 * availability, search, arrange or message on the team's behalf.
 */
@Composable
fun MemberNoticeScreen(team: ActiveTeam, navigator: Navigator) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("You're a member of ${team.teamName}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Publishing availability, finding opponents, arranging friendlies and messaging other teams are for " +
                "the team's managers. Ask a manager to change your role in their club's members list if you need " +
                "to do these.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navigator.push(Route.TeamProfile(team.teamId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("View team profile")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { navigator.push(Route.RoleSelection) }, modifier = Modifier.fillMaxWidth()) {
            Text("Create your own team")
        }
    }
}
