package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.ActiveTeam
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.ui.components.HeroBand

/**
 * SCR-ON-06 Onboarding complete. Purpose: confirm setup and direct the user
 * to the highest-value next action. The pre-warmed "Find me a friendly"
 * result set the spec describes needs the Discover slice's search endpoint
 * wiring, so this routes to the placeholder for now.
 */
@Composable
fun OnboardingCompleteScreen(
    teamRepository: TeamRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    teamId: String
) {
    // The team just created becomes the app's active team - see CurrentTeamStore.
    LaunchedEffect(teamId) {
        val result = teamRepository.get(teamId)
        if (result is ApiResult.Success) {
            currentTeamStore.set(ActiveTeam(result.value.id, result.value.clubId, result.value.name))
        }
    }

    val activeTeam by currentTeamStore.active.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroBand(
            title = "You're set up",
            eyebrow = activeTeam?.teamName ?: "All done",
            subtitle = "Your team profile is ready - here's what's next.",
            modifier = Modifier.padding(16.dp)
        )
        Spacer(Modifier.height(8.dp))

        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                ChecklistRow("Team profile created")
                ChecklistRow("Venue and availability - see your team profile for what's outstanding")
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Get verified", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Verification unlocks the verified badge and helps other clubs trust your invitations.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { navigator.push(Route.SearchEntry) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Find me a friendly") }
        OutlinedButton(
            onClick = { navigator.push(Route.TeamProfile(teamId)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("View team profile") }
        TextButton(
            onClick = { navigator.resetTo(Route.Home) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Explore the app") }
        Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChecklistRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
