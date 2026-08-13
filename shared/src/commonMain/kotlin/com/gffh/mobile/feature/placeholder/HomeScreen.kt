package com.gffh.mobile.feature.placeholder

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.model.SlotView
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.repository.NotificationRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.*

/**
 * SCR-HM-01 Dashboard. Purpose: answer "what needs my attention?" within two
 * seconds of opening the app.
 *
 * The team switcher and suggested-opponents carousel are the two pieces still
 * not real: the switcher needs a "list every team I manage" endpoint (see
 * [CurrentTeamStore]'s doc comment), and the carousel needs a standing
 * matches/search call the user hasn't triggered yet - both card through to
 * the real screens (Fixtures, Find a friendly) instead of showing fake data.
 */
@Composable
fun HomeScreen(
    authRepository: AuthRepository,
    teamRepository: TeamRepository,
    availabilityRepository: AvailabilityRepository,
    fixtureRepository: FixtureRepository,
    notificationRepository: NotificationRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val session by authRepository.session.collectAsState()
    val activeTeam by currentTeamStore.active.collectAsState()

    var team by remember { mutableStateOf<TeamView?>(null) }
    var futureSlots by remember { mutableStateOf<List<SlotView>>(emptyList()) }
    var fixtures by remember { mutableStateOf<List<FixtureView>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0L) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = notificationRepository.unreadCount()
        if (result is ApiResult.Success) unreadCount = result.value.count
    }

    LaunchedEffect(activeTeam?.teamId) {
        val t = activeTeam ?: return@LaunchedEffect
        val teamResult = teamRepository.get(t.teamId)
        if (teamResult is ApiResult.Success) team = teamResult.value

        val today = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
        val horizon = today.plus(56, DateTimeUnit.DAY)
        val slotResult = availabilityRepository.list(t.teamId, today.toString(), horizon.toString())
        if (slotResult is ApiResult.Success) futureSlots = slotResult.value

        val fixtureResult = fixtureRepository.list(t.teamId)
        if (fixtureResult is ApiResult.Success) fixtures = fixtureResult.value
        loaded = true
    }

    val nextFixture = fixtures.filter { it.status == "CONFIRMED" }.minByOrNull { it.date }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text("Welcome, ${session?.displayName ?: ""}", style = MaterialTheme.typography.headlineSmall)
                activeTeam?.let {
                    Text(it.teamName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            BadgedBox(badge = {
                if (unreadCount > 0) Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) }
            }) {
                IconButton(onClick = { navigator.push(Route.Notifications) }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            }
        }

        if (session?.emailVerified == false) {
            Spacer(Modifier.height(12.dp))
            AssistChip(onClick = { navigator.push(Route.EmailVerification) }, label = { Text("Verify your email") })
        }

        Spacer(Modifier.height(24.dp))

        if (activeTeam == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("No team yet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Create or join a team to publish availability and find friendlies.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { navigator.push(Route.RoleSelection) }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Create or join a team")
                    }
                }
            }
        } else {
            if (nextFixture != null) {
                Card(onClick = { navigator.push(Route.FixtureDetail(nextFixture.id)) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Next fixture", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${nextFixture.homeTeam.name} vs ${nextFixture.awayTeam.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("${nextFixture.date}, kick-off ${nextFixture.startTime}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Action required.
            val completeness = team?.completenessPercent
            val actionItems = buildList {
                if (completeness != null && completeness < 80) add("Team profile is $completeness% complete - invitations need 80%.")
                if (loaded && futureSlots.isEmpty()) add("No availability published yet.")
                if (session?.emailVerified == false) add("Email not verified - invitations and messages are blocked.")
            }
            if (actionItems.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Needs your attention", style = MaterialTheme.typography.titleSmall)
                        actionItems.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(onClick = { navigator.push(Route.SearchEntry) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Find a friendly", style = MaterialTheme.typography.titleSmall)
                    Text("Search", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(onClick = { navigator.push(Route.Fixtures) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Requests and fixtures", style = MaterialTheme.typography.titleSmall)
                    Text("Open", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(onClick = { navigator.push(Route.Calendar) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Availability", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (loaded) "${futureSlots.map { it.date }.toSet().size} date(s) published"
                            else "Loading...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Open calendar", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (futureSlots.isEmpty()) navigator.push(Route.EditAvailabilitySlot())
                    else navigator.push(Route.SearchEntry)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (futureSlots.isEmpty()) "Add availability" else "Find me a friendly") }

            OutlinedButton(
                onClick = { navigator.push(Route.TeamProfile(activeTeam!!.teamId)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Team profile") }
        }

        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = {
                authRepository.signOut()
                currentTeamStore.clear()
                navigator.resetTo(Route.Welcome)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign out") }
    }
}
