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
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.HeroBand
import kotlinx.coroutines.launch
import com.gffh.mobile.ui.components.codeLabel

/**
 * SCR-PR-03 Club profile. Purpose: manage club identity and the squads beneath it.
 *
 * Only club admins can read the club and its squads (anyone else gets a 404),
 * so a team manager - the role a member invitation gives by default - sees
 * just the team's officials and the club's venues, without the club, its
 * teams or "Edit club".
 */
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
                // Not a club admin: they still get the team's officials and the club's venues below.
                is ApiResult.Failure -> if (result.httpStatus != 404) errorMessage = result.message
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
    if (c == null && errorMessage != null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage!!) }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroBand(
            title = c?.name ?: "Your club",
            eyebrow = "Club profile",
            subtitle = if (c == null) {
                "Only a club admin can edit the club or add squads."
            } else {
                c.postcode?.let { "Based in $it" } ?: "Manage your club and its squads."
            },
            compact = true,
            modifier = Modifier.padding(16.dp)
        )

        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        if (c != null) {
            Text("Teams", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            teams.forEach { t ->
                Card(
                    // Only the club's admins get its teams list.
                    onClick = { currentTeamStore.set(ActiveTeam(t.id, t.clubId, t.name, canManageClub = true)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CrestAvatar(t.name, size = 32.dp, badgeUrl = t.badgeUrl)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(t.name, style = MaterialTheme.typography.titleSmall)
                                Text("${codeLabel(t.ageGroup)} · ${codeLabel(t.format)} · ${t.completenessPercent}% complete", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (t.id == activeTeam?.teamId) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Active") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { navigator.push(Route.CreateTeam(c.id)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add team") }
            Spacer(Modifier.height(16.dp))
        }

        Card(
            onClick = { activeTeam?.let { navigator.push(Route.Members(it.teamId)) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Officials", modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(onClick = { navigator.push(Route.VenuesList(clubId)) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Venues")
                Text("Shared across all squads in this club", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (c != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = { navigator.push(Route.EditClub(c.id)) }, modifier = Modifier.fillMaxWidth()) {
                Text("Edit club")
            }
        }
        Spacer(Modifier.height(16.dp))
        }
    }
}
