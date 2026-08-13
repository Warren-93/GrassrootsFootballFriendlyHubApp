package com.gffh.mobile.feature.availability

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateSlotRequest
import com.gffh.mobile.model.HomeAwayPreference
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock as DateTimeClock
import kotlinx.datetime.*

/**
 * SCR-AV-03 Add / edit availability slot. Purpose: publish or amend a window
 * in which the team can play.
 *
 * Simplifications from spec: no platform time-picker dialog is wired up yet
 * (times are fixed at the spec's own defaults, 10:00-13:00). Repeating a slot
 * across several dates is its own screen (SCR-AV-04, reachable from Calendar)
 * rather than an option here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAvailabilitySlotScreen(
    availabilityRepository: AvailabilityRepository,
    venueRepository: VenueRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    slotId: String?,
    initialDate: String?
) {
    val team = currentTeamStore.active.collectAsState().value
    val isEdit = slotId != null

    var date by remember { mutableStateOf(initialDate ?: todayDate().toString()) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("13:00") }
    var homeAway by remember { mutableStateOf(HomeAwayPreference.EITHER) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var selectedVenueId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(isEdit) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(team?.clubId) {
        val clubId = team?.clubId ?: return@LaunchedEffect
        (venueRepository.list(clubId) as? ApiResult.Success)?.let { result ->
            venues = result.value
            if (selectedVenueId == null) selectedVenueId = result.value.firstOrNull { it.isDefault }?.id
        }
    }

    LaunchedEffect(slotId) {
        val t = team
        if (slotId != null && t != null) {
            when (val result = availabilityRepository.get(t.teamId, slotId)) {
                is ApiResult.Success -> {
                    val s = result.value
                    date = s.date; startTime = s.startTime.take(5); endTime = s.endTime.take(5)
                    homeAway = HomeAwayPreference.valueOf(s.homeAwayPreference)
                    selectedVenueId = s.venueId
                    notes = s.notes ?: ""
                    loading = false
                }
                is ApiResult.Failure -> { errorMessage = result.message; loading = false }
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(if (isEdit) "Edit availability" else "Add availability", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = date, onValueChange = { date = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Time window: $startTime - $endTime", style = MaterialTheme.typography.labelLarge)

        Spacer(Modifier.height(16.dp))
        Text("Home / Away", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeAwayPreference.entries.forEach { pref ->
                FilterChip(selected = homeAway == pref, onClick = { homeAway = pref }, label = { Text(pref.name) })
            }
        }

        if (homeAway != HomeAwayPreference.AWAY && venues.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Venue", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                venues.forEach { v ->
                    FilterChip(selected = selectedVenueId == v.id, onClick = { selectedVenueId = v.id }, label = { Text(v.name) })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = notes, onValueChange = { if (it.length <= 280) notes = it },
            label = { Text("Notes (optional)") },
            supportingText = { Text("${notes.length}/280") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val t = team ?: return@Button
                submitting = true
                errorMessage = null
                scope.launch {
                    val request = CreateSlotRequest(
                        date = date, startTime = "$startTime:00", endTime = "$endTime:00",
                        homeAwayPreference = homeAway.name,
                        venueId = if (homeAway != HomeAwayPreference.AWAY) selectedVenueId else null,
                        notes = notes.ifBlank { null }
                    )
                    val result = if (isEdit) availabilityRepository.update(t.teamId, slotId!!, request)
                    else availabilityRepository.create(t.teamId, request)
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> navigator.pop()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = !submitting && team != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (isEdit) "Save" else "Publish")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

fun todayDate(): LocalDate = DateTimeClock.System.todayIn(TimeZone.currentSystemDefault())
