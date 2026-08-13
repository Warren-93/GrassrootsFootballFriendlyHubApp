package com.gffh.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.gffh.mobile.repository.GeoPoint
import com.gffh.mobile.repository.GeocodeRepository
import kotlinx.coroutines.launch

private enum class LookupStatus { IDLE, LOADING, FOUND, NOT_FOUND }

/**
 * Replaces manual longitude/latitude entry with postcode-driven lookup via
 * postcodes.io - mirrors gffh-web's PostcodeLocationField.tsx. Looks up on
 * focus loss and invalidates any resolved coordinates as soon as the
 * postcode text changes again, so a stale location can never be submitted
 * silently alongside an edited postcode.
 */
@Composable
fun PostcodeLocationField(
    geocodeRepository: GeocodeRepository,
    postcode: String,
    onPostcodeChange: (String) -> Unit,
    coordinates: GeoPoint?,
    onCoordinatesChange: (GeoPoint?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Postcode",
) {
    var status by remember { mutableStateOf(LookupStatus.IDLE) }
    var wasFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun lookup() {
        val trimmed = postcode.trim()
        if (trimmed.isEmpty()) return
        status = LookupStatus.LOADING
        scope.launch {
            val result = geocodeRepository.geocodePostcode(trimmed)
            if (result != null) {
                onCoordinatesChange(result)
                status = LookupStatus.FOUND
            } else {
                onCoordinatesChange(null)
                status = LookupStatus.NOT_FOUND
            }
        }
    }

    Column(modifier) {
        OutlinedTextField(
            value = postcode,
            onValueChange = {
                onPostcodeChange(it)
                if (coordinates != null) onCoordinatesChange(null)
                if (status != LookupStatus.IDLE) status = LookupStatus.IDLE
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (wasFocused && !focusState.isFocused) lookup()
                wasFocused = focusState.isFocused
            }
        )
        when (status) {
            LookupStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("Looking up postcode…", style = MaterialTheme.typography.bodySmall)
            }
            LookupStatus.FOUND -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Location found (${displayOutcode(postcode)}).", style = MaterialTheme.typography.bodySmall)
            }
            LookupStatus.NOT_FOUND -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Couldn't find that postcode - check it and try again.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                )
            }
            LookupStatus.IDLE -> {}
        }
    }
}

/** Outward code = the postcode minus its last 3 chars (a digit and two letters), e.g. "SW1A 1AA" -> "SW1A". */
private fun displayOutcode(postcode: String): String {
    val trimmed = postcode.trim().uppercase()
    return if (trimmed.length > 3) trimmed.dropLast(3).trim() else trimmed
}