package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateVenueRequest
import com.gffh.mobile.model.PitchSurface
import com.gffh.mobile.model.VenueFacility
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.VenueRepository
import kotlinx.coroutines.launch

/**
 * SCR-ON-04 Add first venue. Purpose: capture a home venue so that home
 * fixtures can be proposed. No map/geocoding integration (see
 * gffh-mobile/README.md) - latitude/longitude are entered directly rather
 * than a draggable pin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFirstVenueScreen(venueRepository: VenueRepository, navigator: Navigator, clubId: String, teamId: String) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var surface by remember { mutableStateOf<PitchSurface?>(null) }
    var facilities by remember { mutableStateOf(setOf<VenueFacility>()) }
    var accessNotes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val lon = longitude.toDoubleOrNull()
    val lat = latitude.toDoubleOrNull()
    val formValid = name.trim().length in 3..80 && address.isNotBlank() &&
        lon != null && lon in -180.0..180.0 && lat != null && lat in -90.0..90.0

    fun proceed() = navigator.push(Route.AddFirstAvailability(teamId, clubId))

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Add your home venue", style = MaterialTheme.typography.headlineSmall)
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
        Row {
            OutlinedTextField(
                value = latitude, onValueChange = { latitude = it },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = longitude, onValueChange = { longitude = it },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Pitch surface", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PitchSurface.entries.forEach { s ->
                FilterChip(selected = surface == s, onClick = { surface = if (surface == s) null else s },
                    label = { Text(s.name) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Facilities", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VenueFacility.entries.forEach { f ->
                FilterChip(
                    selected = f in facilities,
                    onClick = { facilities = if (f in facilities) facilities - f else facilities + f },
                    label = { Text(f.name.replace('_', ' ')) }
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
                            longitude = lon!!, latitude = lat!!,
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
        Text(
            "Home fixtures can't be proposed until a venue exists.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
