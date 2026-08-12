package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ClubView
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.ClubRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.ActiveTeam
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/** SCR-PR-03 Club profile. Purpose: manage club identity and the squads beneath it. */
@Composable
fun ClubProfileScreen(
    clubRepository: ClubRepository,
    teamRepository: TeamRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    clubId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var club by remember { mutableStateOf<ClubView?>(null) }
    var teams by remember { mutableStateOf<List<TeamView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clubId) {
        scope.launch {
            when (val result = clubRepository.get(clubId)) {
                is ApiResult.Success -> club = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
            when (val result = teamRepository.listByClub(clubId)) {
                is ApiResult.Success -> teams = result.value
                is ApiResult.Failure -> {}
            }
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val c = club
    if (c == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "Club not found.") }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(c.name, style = MaterialTheme.typography.headlineSmall)
        Text(c.postcode ?: "", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(20.dp))
        Text("Teams", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        teams.forEach { t ->
            Card(
                onClick = { currentTeamStore.set(ActiveTeam(t.id, t.clubId, t.name)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(t.name, style = MaterialTheme.typography.titleSmall)
                        Text("${t.ageGroup} · ${t.format} · ${t.completenessPercent}% complete", style = MaterialTheme.typography.bodySmall)
                    }
                    if (t.id == activeTeam?.teamId) AssistChip(onClick = {}, label = { Text("Active") })
                }
            }
        }
        OutlinedButton(
            onClick = { navigator.push(Route.CreateTeam(c.id)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add team") }

        Spacer(Modifier.height(16.dp))
        Card(onClick = { navigator.push(Route.Placeholder("Team members and permissions")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Officials", modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(onClick = { navigator.push(Route.VenuesList(c.id)) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Venues")
                Text("Shared across all squads in this club", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = { navigator.push(Route.EditClub(c.id)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Edit club")
        }
    }
}
