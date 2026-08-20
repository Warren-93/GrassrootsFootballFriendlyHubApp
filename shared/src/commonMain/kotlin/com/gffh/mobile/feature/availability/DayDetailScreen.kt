package com.gffh.mobile.feature.availability

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.model.FriendlyRequestView
import com.gffh.mobile.model.SlotView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.repository.FriendlyRequestRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

private val PENDING_STATUSES = setOf("SENT", "CHANGES_REQUESTED", "UPDATED")

/**
 * SCR-AV-02 Day detail. Purpose: manage everything happening on a single date -
 * published availability, any confirmed fixture, and any friendly request still
 * awaiting a response.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    availabilityRepository: AvailabilityRepository,
    fixtureRepository: FixtureRepository,
    friendlyRequestRepository: FriendlyRequestRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    date: String
) {
    val team = currentTeamStore.active.collectAsState().value
    var slots by remember { mutableStateOf<List<SlotView>>(emptyList()) }
    var fixtures by remember { mutableStateOf<List<FixtureView>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendlyRequestView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<SlotView?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        val t = team ?: return
        loading = true
        scope.launch {
            val result = availabilityRepository.list(t.teamId, date, date)
            when (result) {
                is ApiResult.Success -> { slots = result.value; errorMessage = null }
                is ApiResult.Failure -> errorMessage = result.message
            }

            (fixtureRepository.list(t.teamId) as? ApiResult.Success)?.let { r ->
                fixtures = r.value.filter { it.date == date && it.status == "CONFIRMED" }
            }
            (friendlyRequestRepository.list(t.teamId) as? ApiResult.Success)?.let { r ->
                pendingRequests = r.value.filter { it.date == date && it.status in PENDING_STATUSES }
            }

            loading = false
        }
    }

    LaunchedEffect(team?.teamId, date) { reload() }

    val isEmpty = slots.isEmpty() && fixtures.isEmpty() && pendingRequests.isEmpty()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(date) },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (isEmpty) {
            Column(Modifier.padding(24.dp)) {
                Text("Nothing on this date.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(Modifier.padding(16.dp).weight(1f, fill = false)) {
                if (fixtures.isNotEmpty()) {
                    Text("Confirmed fixtures", style = MaterialTheme.typography.titleSmall)
                    fixtures.forEach { fixture ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            onClick = { navigator.push(Route.FixtureDetail(fixture.id)) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("${fixture.homeTeam.name} vs ${fixture.awayTeam.name}", style = MaterialTheme.typography.bodyMedium)
                                Text("${fixture.startTime} - ${fixture.endTime}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (pendingRequests.isNotEmpty()) {
                    Text("Pending requests", style = MaterialTheme.typography.titleSmall)
                    pendingRequests.forEach { request ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            onClick = { navigator.push(Route.RequestDetail(request.id)) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("${request.startTime} - ${request.endTime}", style = MaterialTheme.typography.bodyMedium)
                                Text(request.status, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (slots.isNotEmpty()) {
                    Text("Availability", style = MaterialTheme.typography.titleSmall)
                    slots.forEach { slot ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${slot.startTime} - ${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                                    Text(slot.homeAwayPreference, style = MaterialTheme.typography.bodySmall)
                                    slot.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                                Row {
                                    IconButton(onClick = {
                                        navigator.push(Route.EditAvailabilitySlot(slotId = slot.id, date = slot.date))
                                    }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                    IconButton(onClick = { pendingDelete = slot }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Withdraw")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = isEmpty))
        Button(
            onClick = { navigator.push(Route.EditAvailabilitySlot(date = date)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("Add slot") }
    }

    pendingDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Withdraw this slot?") },
            text = { Text("${slot.date} ${slot.startTime}-${slot.endTime} will no longer be visible to other teams.") },
            confirmButton = {
                TextButton(onClick = {
                    val t = team
                    pendingDelete = null
                    if (t != null) {
                        scope.launch {
                            availabilityRepository.withdraw(t.teamId, slot.id)
                            reload()
                        }
                    }
                }) { Text("Withdraw") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep editing") } }
        )
    }
}
