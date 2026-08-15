package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateVenueRequest
import com.gffh.mobile.model.PitchSurface
import com.gffh.mobile.model.VenueFacility
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.GeoPoint
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.theme.displayHeadlineSmall
import com.gffh.mobile.theme.eyebrowLabel
import com.gffh.mobile.ui.components.PostcodeLocationField
import kotlinx.coroutines.launch

/**
 * SCR-ON-04 Add first venue. Purpose: capture a home venue so that home
 * fixtures can be proposed. Longitude/latitude are resolved from a postcode
 * via postcodes.io rather than a draggable pin - see PostcodeLocationField.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFirstVenueScreen(
    venueRepository: VenueRepository,
    geocodeRepository: GeocodeRepository,
    navigator: Navigator,
    clubId: String,
    teamId: String
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf<GeoPoint?>(null) }
    var surface by remember { mutableStateOf<PitchSurface?>(null) }
    var facilities by remember { mutableStateOf(setOf<VenueFacility>()) }
    var accessNotes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val formValid = name.trim().length in 3..80 && address.isNotBlank() && coordinates != null

    fun proceed() = navigator.push(Route.AddFirstAvailability(teamId, clubId))

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("STEP 2 OF 3 · TEAM & VENUE", style = eyebrowLabel, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text("Add your home venue".uppercase(), style = displayHeadlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Venue name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        PostcodeLocationField(
            geocodeRepository = geocodeRepository,
            postcode = postcode, onPostcodeChange = { postcode = it },
            coordinates = coordinates, onCoordinatesChange = { coordinates = it }
        )

        Spacer(Modifier.height(16.dp))
        Text("Pitch surface", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PitchSurface.entries.forEach { s ->
                FilterChip(
                    selected = surface == s, onClick = { surface = if (surface == s) null else s },
                    label = { Text(s.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Facilities", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VenueFacility.entries.forEach { f ->
                FilterChip(
                    selected = f in facilities,
                    onClick = { facilities = if (f in facilities) facilities - f else facilities + f },
                    label = { Text(f.name.replace('_', ' ')) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = accessNotes,
            onValueChange = { if (it.length <= 280) accessNotes = it },
            label = { Text("Access notes (optional)") },
            supportingText = { Text("e.g. gate code, entrance to use") },
            modifier = Modifier.fillMaxWidth()
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
                    val result = venueRepository.create(
                        CreateVenueRequest(
                            clubId = clubId, name = name.trim(), address = address.trim(),
                            longitude = coordinates!!.longitude, latitude = coordinates!!.latitude,
                            pitchSurface = surface?.name, facilities = facilities.map { it.name },
                            accessNotes = accessNotes.ifBlank { null }
                        )
                    )
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> proceed()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = formValid && !submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Save venue")
        }
        TextButton(onClick = { proceed() }, modifier = Modifier.fillMaxWidth()) {
            Text("Skip")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
        Text(
            "Home fixtures can't be proposed until a venue exists.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}