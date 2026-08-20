package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FriendlyRequestView
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.FriendlyRequestRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.ui.components.TimeField
import com.gffh.mobile.ui.components.formatHourMinute
import com.gffh.mobile.ui.components.parseHourMinute
import kotlinx.coroutines.launch

/**
 * SCR-IN-05 Suggest changes. Purpose: counter-propose without discarding what
 * both parties have already agreed. Date isn't proposable here - see the
 * backend's FriendlyRequest javadoc on why - only the kick-off time and venue.
 */
@Composable
fun SuggestChangesScreen(
    friendlyRequestRepository: FriendlyRequestRepository,
    venueRepository: VenueRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    requestId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var request by remember { mutableStateOf<FriendlyRequestView?>(null) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var venueId by remember { mutableStateOf<String?>(null) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var reason by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        when (val result = friendlyRequestRepository.get(requestId)) {
            is ApiResult.Success -> {
                request = result.value
                startTime = result.value.startTime.take(5)
                endTime = result.value.endTime.take(5)
                venueId = result.value.venueId
            }
            is ApiResult.Failure -> {}
        }
    }

    LaunchedEffect(activeTeam?.clubId) {
        val clubId = activeTeam?.clubId ?: return@LaunchedEffect
        (venueRepository.list(clubId) as? ApiResult.Success)?.let { venues = it.value }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Suggest changes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Propose a different kick-off time or venue. The date itself can't change here - decline and start a " +
                "new request if a different day works better.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        val current = request
        if (current != null) {
            val (startHour, startMinute) = parseHourMinute(startTime)
            val (endHour, endMinute) = parseHourMinute(endTime)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("Start", startHour, startMinute, { h, m -> startTime = formatHourMinute(h, m) })
                TimeField("End", endHour, endMinute, { h, m -> endTime = formatHourMinute(h, m) })
            }

            if (venues.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Venue", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    venues.forEach { v ->
                        FilterChip(selected = venueId == v.id, onClick = { venueId = v.id }, label = { Text(v.name) })
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = reason, onValueChange = { if (it.length <= 500) reason = it },
            label = { Text("Reason (optional)") },
            supportingText = { Text("${reason.length}/500") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val current2 = request ?: return@Button
                sending = true
                errorMessage = null
                scope.launch {
                    val result = friendlyRequestRepository.act(
                        requestId, "suggestChanges",
                        reason = reason.ifBlank { null },
                        proposedStartTime = if (startTime != current2.startTime.take(5)) "$startTime:00" else null,
                        proposedEndTime = if (endTime != current2.endTime.take(5)) "$endTime:00" else null,
                        proposedVenueId = if (venueId != current2.venueId) venueId else null
                    )
                    when (result) {
                        is ApiResult.Success -> navigator.pop()
                        is ApiResult.Failure -> { errorMessage = "Could not send changes."; sending = false }
                    }
                }
            },
            enabled = !sending && request != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Send changes") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
