package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.model.FriendlyRequestView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.repository.FriendlyRequestRepository
import com.gffh.mobile.session.CurrentTeamStore

/**
 * SCR-FX-01/02/03 Fixtures - Pending, Confirmed, Completed. Purpose: hold
 * every request and fixture in one place, segmented by what the user can do
 * about it.
 */
@Composable
fun FixturesScreen(
    friendlyRequestRepository: FriendlyRequestRepository,
    fixtureRepository: FixtureRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val team = currentTeamStore.active.collectAsState().value
    var requests by remember { mutableStateOf<List<FriendlyRequestView>>(emptyList()) }
    var fixtures by remember { mutableStateOf<List<FixtureView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(team?.teamId) {
        val t = team ?: return@LaunchedEffect
        (friendlyRequestRepository.list(t.teamId) as? ApiResult.Success)?.let { requests = it.value }
        (fixtureRepository.list(t.teamId) as? ApiResult.Success)?.let { fixtures = it.value }
        loading = false
    }

    val openStatuses = setOf("SENT", "CHANGES_REQUESTED", "UPDATED")
    val pending = requests.filter { it.status in openStatuses }
    val waitingOnYou = pending.filter { !(it.availableActions.size == 1 && it.availableActions.first() == "withdraw") }
    val waitingOnThem = pending.filter { it.availableActions.size == 1 && it.availableActions.first() == "withdraw" }
    val confirmed = fixtures.filter { it.status == "CONFIRMED" }
    val completed = fixtures.filter { it.status == "COMPLETED" }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Pending (${pending.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Confirmed") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Completed") })
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }

        when (tab) {
            0 -> if (pending.isEmpty()) {
                EmptyState("No pending requests. Find a friendly to send an invitation.") { navigator.push(Route.SearchEntry) }
            } else {
                LazyColumnList {
                    if (waitingOnYou.isNotEmpty()) {
                        item { SectionHeader("Waiting on you") }
                        items(waitingOnYou) { RequestRow(it, navigator) }
                    }
                    if (waitingOnThem.isNotEmpty()) {
                        item { SectionHeader("Waiting on them") }
                        items(waitingOnThem) { RequestRow(it, navigator) }
                    }
                }
            }
            1 -> if (confirmed.isEmpty()) {
                EmptyState("No confirmed fixtures yet.") { navigator.push(Route.SearchEntry) }
            } else {
                LazyColumnList { items(confirmed) { FixtureRow(it, navigator) } }
            }
            2 -> if (completed.isEmpty()) {
                EmptyState("Nothing completed yet.", null)
            } else {
                LazyColumnList { items(completed) { FixtureRow(it, navigator) } }
            }
        }
    }
}

@Composable
private fun LazyColumnList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), content = content)
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun RequestRow(request: FriendlyRequestView, navigator: Navigator) {
    Card(
        onClick = { navigator.push(Route.RequestDetail(request.id)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(request.date, style = MaterialTheme.typography.bodyMedium)
                Text(request.status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FixtureRow(fixture: FixtureView, navigator: Navigator) {
    Card(
        onClick = { navigator.push(Route.FixtureDetail(fixture.id)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${fixture.homeTeam.name} vs ${fixture.awayTeam.name}", style = MaterialTheme.typography.bodyMedium)
                Text("${fixture.date} ${fixture.startTime}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState(text: String, onAction: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        onAction?.let {
            Button(onClick = it, modifier = Modifier.padding(top = 16.dp)) { Text("Find a friendly") }
        }
    }
}
