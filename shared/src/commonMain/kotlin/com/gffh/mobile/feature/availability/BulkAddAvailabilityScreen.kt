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
import com.gffh.mobile.ui.components.TimeField
import com.gffh.mobile.ui.components.formatHourMinute
import com.gffh.mobile.ui.components.parseHourMinute
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import com.gffh.mobile.ui.components.codeLabel

private val WEEKDAYS = listOf(
    DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue", DayOfWeek.WEDNESDAY to "Wed",
    DayOfWeek.THURSDAY to "Thu", DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat", DayOfWeek.SUNDAY to "Sun"
)

/**
 * SCR-AV-04 Bulk add availability. Purpose: publish one time window across
 * several dates in one go. The dates come either from a weekly pattern
 * between two dates, or from a list of individually picked dates; both
 * publish through the same bulk endpoint.
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

    var pickMode by remember { mutableStateOf(false) }
    var pickedDates by remember { mutableStateOf(emptyList<LocalDate>()) }
    var dateToAddText by remember { mutableStateOf("") }
    var pickError by remember { mutableStateOf<String?>(null) }
    // Kept as typed, so a date can be edited through states that don't parse yet.
    var fromText by remember { mutableStateOf(today.toString()) }
    var toText by remember { mutableStateOf(today.plus(56, DateTimeUnit.DAY).toString()) }
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

    val fromDate = parseDateOrNull(fromText)
    val toDate = parseDateOrNull(toText)
    val dateProblem = when {
        pickMode -> null
        fromDate == null || toDate == null -> "Enter both dates as YYYY-MM-DD, e.g. ${today}."
        toDate != null && fromDate != null && toDate < fromDate -> "The To date can't be before the From date."
        else -> null
    }
    val previewDates = when {
        pickMode -> pickedDates
        fromDate != null && toDate != null && fromDate <= toDate -> datesMatching(fromDate, toDate, selectedDays)
        else -> emptyList()
    }
    val tooMany = previewDates.size > MAX_BULK_DATES

    fun addDate() {
        when (val outcome = addPickedDate(pickedDates, dateToAddText, today)) {
            is AddDateOutcome.Added -> {
                pickedDates = outcome.dates
                dateToAddText = ""
                pickError = null
            }
            is AddDateOutcome.Refused -> pickError = outcome.reason
        }
    }

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
            if (pickMode) "Publish the same time window on each date you pick."
            else "Publish the same time window on every matching weekday between two dates.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !pickMode, onClick = { pickMode = false }, label = { Text("Repeat weekly") })
            FilterChip(selected = pickMode, onClick = { pickMode = true }, label = { Text("Pick dates") })
        }
        Spacer(Modifier.height(16.dp))

        if (pickMode) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dateToAddText, onValueChange = { dateToAddText = it; pickError = null },
                    label = { Text("Date (YYYY-MM-DD)") }, isError = pickError != null, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { addDate() }, enabled = dateToAddText.isNotBlank()) { Text("Add date") }
            }
            pickError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            if (pickedDates.isEmpty()) {
                Text(
                    "No dates added yet. Type a date and tap Add date for each one you want to publish.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Tap a date to remove it", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pickedDates.forEach { date ->
                        FilterChip(
                            selected = true,
                            onClick = { pickedDates = pickedDates - date },
                            label = { Text(pickedDateLabel(date)) },
                            trailingIcon = { Text("✕") }
                        )
                    }
                }
                TextButton(onClick = { pickedDates = emptyList() }) { Text("Clear all") }
            }
        } else {
            OutlinedTextField(
                value = fromText, onValueChange = { fromText = it },
                label = { Text("From (YYYY-MM-DD)") }, isError = fromDate == null, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = toText, onValueChange = { toText = it },
                label = { Text("To (YYYY-MM-DD)") }, isError = toDate == null, singleLine = true,
                modifier = Modifier.fillMaxWidth()
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
        }

        Spacer(Modifier.height(16.dp))
        Text("Time window", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val (startHour, startMinute) = parseHourMinute(startTime)
            val (endHour, endMinute) = parseHourMinute(endTime)
            TimeField("Start", startHour, startMinute, { h, m -> startTime = formatHourMinute(h, m) })
            TimeField("End", endHour, endMinute, { h, m -> endTime = formatHourMinute(h, m) })
        }

        Spacer(Modifier.height(16.dp))
        Text("Home / Away", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeAwayPreference.entries.forEach { pref ->
                FilterChip(selected = homeAway == pref, onClick = { homeAway = pref }, label = { Text(codeLabel(pref.name)) })
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
        if (dateProblem != null) {
            Text(dateProblem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        } else if (tooMany) {
            Text(
                "You can publish up to $MAX_BULK_DATES dates at once. Shorten the range or untick a day.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "${previewDates.size} date${if (previewDates.size == 1) "" else "s"} will be published.",
                style = MaterialTheme.typography.bodySmall
            )
        }

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
            enabled = !submitting && previewDates.isNotEmpty() && !tooMany,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Publish ${previewDates.size} date${if (previewDates.size == 1) "" else "s"}")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
