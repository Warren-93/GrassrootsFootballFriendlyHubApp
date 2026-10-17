package com.gffh.mobile.feature.placeholder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.feature.availability.todayDate
import com.gffh.mobile.model.FixtureTeamView
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.session.ActiveTeam
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.VenueCard
import com.gffh.mobile.ui.components.refereeLabel
import com.gffh.mobile.ui.components.costShareLabel

/**
 * SCR-FX-04 for a member who joined with a code: the same fixture, read-only.
 * There's nothing to act on - cancelling, messaging the other team and
 * reporting are for the managers (FixtureDetailScreen) - and the API leaves
 * the managers' names and phone numbers out for someone who manages neither
 * team, so each is shown only when it's there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberFixtureDetailScreen(
    fixtureRepository: FixtureRepository,
    team: ActiveTeam,
    navigator: Navigator,
    fixtureId: String
) {
    var fixture by remember { mutableStateOf<FixtureView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val today = remember { todayDate() }

    LaunchedEffect(fixtureId) {
        when (val result = fixtureRepository.get(fixtureId)) {
            is ApiResult.Success -> fixture = result.value
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Fixture") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        val f = fixture
        if (f == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "Not found") }
            return
        }
        val side = f.sideOf(team.teamId)

        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            Text("${f.homeTeam.name} vs ${f.awayTeam.name}", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${fixtureDateLabel(f.date, today)}, kick-off ${kickOffLabel(f.startTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
            // Null for a fixture this team isn't playing in - one of the user's other
            // teams', say, opened from the notification centre.
            if (side != null) {
                Text(
                    if (side.isHome) "${team.teamName} are at home" else "${team.teamName} are away",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            when (f.status) {
                "CANCELLED" -> Text(
                    "This fixture has been cancelled.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                "COMPLETED" -> Text("This fixture has been played.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            val venue = f.venue
            if (venue != null) {
                VenueCard(venue)
            } else {
                Text(
                    "Venue to be confirmed - the team's managers will let you know.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(16.dp))
            TeamCard("Home", f.homeTeam)
            Spacer(Modifier.height(12.dp))
            TeamCard("Away", f.awayTeam)

            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Arrangements", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Cost share: ${costShareLabel(f.costShare)}", style = MaterialTheme.typography.bodySmall)
                    Text("Referee: ${refereeLabel(f.refereeArrangement)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** The team, and its manager's name and phone number when the API included them. */
@Composable
private fun TeamCard(title: String, team: FixtureTeamView) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CrestAvatar(team.name, size = 32.dp, badgeUrl = team.badgeUrl)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(team.name)
                    Text(team.clubName, style = MaterialTheme.typography.bodySmall)
                    team.managerName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    team.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
