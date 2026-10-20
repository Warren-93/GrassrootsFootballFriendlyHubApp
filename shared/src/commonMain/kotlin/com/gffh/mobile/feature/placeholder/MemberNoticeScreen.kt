package com.gffh.mobile.feature.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.feature.availability.todayDate
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.session.ActiveTeam
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.VenueCard
import kotlinx.datetime.LocalDate

/**
 * Shown in place of manager screens to someone who joined a team with a join
 * code: they're a member (role USER), and the API won't let them publish
 * availability, search, arrange or message on the team's behalf. What they
 * can see is the team's confirmed fixtures still to come - when, who against,
 * home or away, and where - each opening a read-only fixture detail
 * (MemberFixtureDetailScreen).
 */
@Composable
fun MemberNoticeScreen(
    team: ActiveTeam,
    fixtureRepository: FixtureRepository,
    availabilityRepository: AvailabilityRepository,
    navigator: Navigator
) {
    var fixtures by remember(team.teamId) { mutableStateOf<List<FixtureView>?>(null) }
    var publishedDates by remember(team.teamId) { mutableStateOf<Set<String>?>(null) }
    var errorMessage by remember(team.teamId) { mutableStateOf<String?>(null) }
    var attempt by remember(team.teamId) { mutableStateOf(0) }
    val today = remember { todayDate() }

    LaunchedEffect(team.teamId, attempt) {
        errorMessage = null
        publishedDates = null
        when (val result = fixtureRepository.list(team.teamId)) {
            is ApiResult.Success -> fixtures = result.value
            is ApiResult.Failure -> errorMessage = result.message
        }
        when (val result = availabilityRepository.list(team.teamId)) {
            is ApiResult.Success -> publishedDates = result.value.map { it.date }.toSet()
            is ApiResult.Failure -> errorMessage = result.message
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("You're a member of ${team.teamName}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Publishing availability, finding opponents, arranging friendlies and messaging other teams are for " +
                "the team's managers. Ask a manager to change your role in their club's members list if you need " +
                "to do these.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))
        Text("Upcoming fixtures", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val loaded = fixtures
        val error = errorMessage
        when {
            error != null -> {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { attempt++ }) { Text("Try again") }
            }
            loaded == null || publishedDates == null -> CircularProgressIndicator(Modifier.padding(vertical = 8.dp))
            else -> {
                val upcoming = fixturesMatchingPublishedAvailability(
                    upcomingFixtures(loaded, today), publishedDates.orEmpty()
                )
                if (upcoming.isEmpty()) {
                    Text(
                        "No upcoming fixtures currently match the team's published availability dates.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                upcoming.forEach { fixture ->
                    UpcomingFixture(fixture, team.teamId, today, onOpen = { navigator.push(Route.FixtureDetail(fixture.id)) })
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

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

/** When, who against and home or away - tap for the full fixture - with the venue card under it. */
@Composable
private fun UpcomingFixture(fixture: FixtureView, teamId: String, today: LocalDate, onOpen: () -> Unit) {
    // Every fixture listed for the team is one of its own.
    val side = fixture.sideOf(teamId) ?: return
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(fixtureDateLabel(fixture.date, today), style = MaterialTheme.typography.titleSmall)
            Text("Kick-off ${kickOffLabel(fixture.startTime)}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CrestAvatar(side.opponent.name, size = 30.dp, badgeUrl = side.opponent.badgeUrl)
                Column {
                    Text("vs ${side.opponent.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(if (side.isHome) "Home" else "Away", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    val venue = fixture.venue
    if (venue != null) {
        VenueCard(venue)
    } else {
        Text("Venue to be confirmed.", style = MaterialTheme.typography.bodySmall)
    }
}
