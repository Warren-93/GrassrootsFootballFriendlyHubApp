package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateSlotRequest
import com.gffh.mobile.model.HomeAwayPreference
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.VenueRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.*
// Explicit import: kotlin.time.Clock (stdlib, since Kotlin 2.1) shadows the
// wildcard-imported kotlinx.datetime.Clock, which is hidden on Kotlin/Native
// in this kotlinx-datetime version ("Unresolved reference 'System'" when
// compiling for iOS otherwise).
import kotlin.time.Clock

private fun nextSaturday(today: LocalDate): LocalDate {
    var d = today
    while (d.dayOfWeek != DayOfWeek.SATURDAY) d = d.plus(1, DateTimeUnit.DAY)
    return d
}

/**
 * SCR-ON-05 Add first availability. Purpose: publish one date so the team
 * becomes discoverable immediately. The "compatible teams already available"
 * result the spec shows on success needs the matches/search endpoint, which
 * is built in the Discover slice, not here - this shows a plain confirmation
 * instead.
 */
@Composable
fun AddFirstAvailabilityScreen(
    availabilityRepository: AvailabilityRepository,
    venueRepository: VenueRepository,
    navigator: Navigator,
    teamId: String,
    clubId: String
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var date by remember { mutableStateOf(nextSaturday(today)) }
    var startTime by remember { mutableStateOf(LocalTime(10, 0)) }
    var endTime by remember { mutableStateOf(LocalTime(13, 0)) }
    var homeAway by remember { mutableStateOf(HomeAwayPreference.EITHER) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var selectedVenueId by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clubId) {
        val result = venueRepository.list(clubId)
        if (result is ApiResult.Success) {
            venues = result.value
            selectedVenueId = result.value.firstOrNull { it.isDefault }?.id ?: result.value.firstOrNull()?.id
        }
    }

    val suggestedDates = remember(today) {
        val saturdays = generateSequence(nextSaturday(today)) { it.plus(7, DateTimeUnit.DAY) }.take(4)
        val sundays = generateSequence(nextSaturday(today).plus(1, DateTimeUnit.DAY)) { it.plus(7, DateTimeUnit.DAY) }.take(2)
        (saturdays + sundays).sorted().toList()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Publish your first availability", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        Text("Suggested dates", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestedDates.take(3).forEach { d ->
                FilterChip(selected = d == date, onClick = { date = d }, label = { Text("${d.dayOfMonth}/${d.monthNumber}") })
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Selected: $date", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))
        Text("Time window: ${startTime} - ${endTime}", style = MaterialTheme.typography.labelLarge)
        Text("Default 10:00-13:00 - edit in a future release; time pickers need a platform dialog not wired up yet.",
            style = MaterialTheme.typography.bodySmall)

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

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                submitting = true
                errorMessage = null
                scope.launch {
                    val result = availabilityRepository.create(
                        teamId,
                        CreateSlotRequest(
                            date = date.toString(), startTime = "$startTime:00", endTime = "$endTime:00",
                            homeAwayPreference = homeAway.name,
                            venueId = if (homeAway != HomeAwayPreference.AWAY) selectedVenueId else null
                        )
                    )
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> navigator.push(Route.OnboardingComplete(teamId))
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Publish")
        }
        TextButton(onClick = { navigator.push(Route.OnboardingComplete(teamId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
    }
}
