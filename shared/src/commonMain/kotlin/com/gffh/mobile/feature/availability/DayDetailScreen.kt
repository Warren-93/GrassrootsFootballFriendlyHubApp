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
import com.gffh.mobile.model.SlotView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/**
 * SCR-AV-02 Day detail. Purpose: manage everything happening on a single date.
 *
 * The fixtures and pending-requests lists the spec calls for need the Arrange
 * slice's client repositories, which don't exist yet - this shows availability
 * only for now, with a note where those sections belong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    availabilityRepository: AvailabilityRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    date: String
) {
    val team = currentTeamStore.active.collectAsState().value
    var slots by remember { mutableStateOf<List<SlotView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<SlotView?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        val t = team ?: return
        loading = true
        scope.launch {
            val result = availabilityRepository.list(t.teamId, date, date)
            loading = false
            when (result) {
                is ApiResult.Success -> slots = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
        }
    }

    LaunchedEffect(team?.teamId, date) { reload() }

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
        } else if (slots.isEmpty()) {
            Column(Modifier.padding(24.dp)) {
                Text("Nothing on this date.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(Modifier.padding(16.dp).weight(1f, fill = false)) {
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

        Spacer(Modifier.weight(1f, fill = slots.isEmpty()))
        Text(
            "Fixtures and pending requests for this date will appear here once the Arrange slice is built.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
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
