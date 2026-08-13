package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateVenueRequest
import com.gffh.mobile.model.PitchSurface
import com.gffh.mobile.model.UpdateVenueRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.GeoPoint
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.ui.components.PostcodeLocationField
import kotlinx.coroutines.launch

/**
 * SCR-PR-06 Add / edit venue, as its own reachable screen (not just inline
 * during onboarding) - opened from the Venues list for both "new" (venueId
 * null) and editing an existing one.
 *
 * Editing seeds `coordinates` from the venue's existing lat/lon so leaving
 * the postcode field untouched keeps the current location rather than
 * forcing a re-lookup for an edit that has nothing to do with location.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditVenueScreen(
    venueRepository: VenueRepository,
    geocodeRepository: GeocodeRepository,
    navigator: Navigator,
    venueId: String?,
    clubId: String
) {
    val isNew = venueId == null
    var loading by remember { mutableStateOf(!isNew) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf<GeoPoint?>(null) }
    var surface by remember { mutableStateOf<PitchSurface?>(null) }
    var accessNotes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(venueId) {
        if (venueId == null) return@LaunchedEffect
        when (val result = venueRepository.list(clubId)) {
            is ApiResult.Success -> {
                val venue = result.value.find { it.id == venueId }
                if (venue != null) {
                    name = venue.name
                    address = venue.address
                    coordinates = GeoPoint(venue.latitude, venue.longitude)
                    surface = venue.pitchSurface?.let { s -> PitchSurface.entries.find { it.name == s } }
                    accessNotes = venue.accessNotes ?: ""
                } else {
                    errorMessage = "Venue not found."
                }
            }
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    val formValid = name.trim().length in 3..80 && address.isNotBlank() && coordinates != null

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(if (isNew) "Add venue" else "Edit venue", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Venue name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        PostcodeLocationField(
            geocodeRepository = geocodeRepository,
            postcode = postcode, onPostcodeChange = { postcode = it },
            coordinates = coordinates, onCoordinatesChange = { coordinates = it }
        )
        if (!isNew && postcode.isBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Keeping the existing location. Enter a postcode above to change it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Pitch surface", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PitchSurface.entries.forEach { s ->
                FilterChip(selected = surface == s, onClick = { surface = if (surface == s) null else s }, label = { Text(s.name) })
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = accessNotes,
            onValueChange = { if (it.length <= 280) accessNotes = it },
            label = { Text("Access notes (optional)") },
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
                    val result = if (isNew) {
                        venueRepository.create(
                            CreateVenueRequest(
                                clubId = clubId, name = name.trim(), address = address.trim(),
                                longitude = coordinates!!.longitude, latitude = coordinates!!.latitude,
                                pitchSurface = surface?.name,
                                accessNotes = accessNotes.ifBlank { null }
                            )
                        )
                    } else {
                        venueRepository.update(
                            venueId!!,
                            UpdateVenueRequest(
                                name = name.trim(), address = address.trim(),
                                longitude = coordinates?.longitude, latitude = coordinates?.latitude,
                                pitchSurface = surface?.name, accessNotes = accessNotes.ifBlank { null }
                            )
                        )
                    }
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> navigator.pop()
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
        if (!isNew) {
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete venue", color = MaterialTheme.colorScheme.error) }
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }

    if (confirmDelete && venueId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this venue?") },
            text = { Text("This can't be undone. It won't be possible if a fixture is confirmed there.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        when (val result = venueRepository.delete(venueId)) {
                            is ApiResult.Success -> navigator.pop()
                            is ApiResult.Failure -> errorMessage = result.message
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}