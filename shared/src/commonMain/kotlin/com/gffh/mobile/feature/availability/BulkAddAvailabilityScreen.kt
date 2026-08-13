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
import com.gffh.mobile.model.BulkCreateSlotRequest
import com.gffh.mobile.model.HomeAwayPreference
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val WEEKDAYS = listOf(
    DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue", DayOfWeek.WEDNESDAY to "Wed",
    DayOfWeek.THURSDAY to "Thu", DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat", DayOfWeek.SUNDAY to "Sun"
)

private fun datesMatching(from: LocalDate, to: LocalDate, days: Set<DayOfWeek>): List<LocalDate> {
    val result = mutableListOf<LocalDate>()
    var cursor = from
    while (cursor <= to) {
        if (cursor.dayOfWeek in days) result.add(cursor)
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return result
}

/**
 * SCR-AV-04 Bulk add availability. Purpose: publish a recurring weekly slot
 * (e.g. every Saturday) across several weeks in one go.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkAddAvailabilityScreen(
    availabilityRepository: AvailabilityRepository,
    venueRepository: VenueRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val team = currentTeamStore.active.collectAsState().value
    val today = todayDate()

    var fromDate by remember { mutableStateOf(today) }
    var toDate by remember { mutableStateOf(today.plus(56, DateTimeUnit.DAY)) }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.SATURDAY)) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("12:00") }
    var homeAway by remember { mutableStateOf(HomeAwayPreference.EITHER) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var selectedVenueId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(team?.clubId) {
        val clubId = team?.clubId ?: return@LaunchedEffect
        (venueRepository.list(clubId) as? ApiResult.Success)?.let { res ->
            venues = res.value
            selectedVenueId = res.value.firstOrNull { it.isDefault }?.id
        }
    }

    val previewDates = if (fromDate <= toDate) datesMatching(fromDate, toDate, selectedDays) else emptyList()

    if (team == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("No active team.") }
        return
    }

    result?.let { (createdCount, skipped) ->
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Published", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text("$createdCount date${if (createdCount == 1) "" else "s"} published.")
            if (skipped.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${skipped.size} skipped (already in the past): ${skipped.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back to calendar") }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Bulk add availability", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Publish the same time window on every matching weekday between two dates.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = fromDate.toString(), onValueChange = { runCatching { fromDate = LocalDate.parse(it) } },
            label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = toDate.toString(), onValueChange = { runCatching { toDate = LocalDate.parse(it) } },
            label = { Text("To (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Repeat on", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WEEKDAYS.forEach { (day, label) ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = { selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
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
            label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "${previewDates.size} date${if (previewDates.size == 1) "" else "s"} will be published.",
            style = MaterialTheme.typography.bodySmall
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                submitting = true
                errorMessage = null
                scope.launch {
                    val request = BulkCreateSlotRequest(
                        dates = previewDates.map { it.toString() },
                        startTime = "$startTime:00", endTime = "$endTime:00",
                        homeAwayPreference = homeAway.name,
                        venueId = if (homeAway != HomeAwayPreference.AWAY) selectedVenueId else null,
                        notes = notes.ifBlank { null }
                    )
                    when (val res = availabilityRepository.createBulk(team.teamId, request)) {
                        is ApiResult.Success -> result = res.value.created.size to res.value.skippedPastDates
                        is ApiResult.Failure -> errorMessage = res.message
                    }
                    submitting = false
                }
            },
            enabled = !submitting && previewDates.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Publish ${previewDates.size} date${if (previewDates.size == 1) "" else "s"}")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
