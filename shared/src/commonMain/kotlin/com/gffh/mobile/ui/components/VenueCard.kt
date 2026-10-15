package com.gffh.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.platform.openUrl
import com.gffh.mobile.model.PitchSurface
import com.gffh.mobile.model.VenueSummary
import io.ktor.http.encodeURLParameter
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Where a friendly is being played - name, address and postcode, pitch
 * surface, access and parking notes - plus a Directions button that opens
 * Google Maps. Mirrors gffh-web's VenueCard.tsx.
 */
@Composable
fun VenueCard(venue: VenueSummary, modifier: Modifier = Modifier, title: String = "Venue") {
    val url = directionsUrl(venue)
    var openFailed by remember(url) { mutableStateOf(false) }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(venue.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            venueAddressLine(venue)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            venue.pitchSurface?.let {
                Text("${pitchSurfaceLabel(it)} pitch", style = MaterialTheme.typography.bodySmall)
            }

            venue.accessNotes?.takeIf { it.isNotBlank() }?.let { VenueNote("Access", it) }
            venue.parkingNotes?.takeIf { it.isNotBlank() }?.let { VenueNote("Parking", it) }

            if (url != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { openFailed = !openUrl(url) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Directions")
                }
                if (openFailed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "There's no maps app or browser to open directions in.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun VenueNote(label: String, text: String) {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider()
    Spacer(Modifier.height(10.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/**
 * "12 Park Road, AB1 2CD" - whichever of the address and postcode are known, or null if neither.
 * The postcode isn't repeated when the address already has it in: with none entered for the
 * venue, the API sends the one it found in the address.
 */
fun venueAddressLine(venue: VenueSummary): String? {
    val address = venue.address?.trim().orEmpty()
    val postcode = venue.postcode?.trim().orEmpty()
    val postcodeInAddress = postcode.isNotEmpty() &&
        address.replace(" ", "").contains(postcode.replace(" ", ""), ignoreCase = true)
    return listOf(address, if (postcodeInAddress) "" else postcode)
        .filter { it.isNotEmpty() }
        .joinToString(", ")
        .ifEmpty { null }
}

/** The API's PitchSurface name as people say it - "3G", not "THREE_G". Unknown values pass through. */
fun pitchSurfaceLabel(value: String): String = when (PitchSurface.entries.find { it.name == value }) {
    PitchSurface.GRASS -> "Grass"
    PitchSurface.THREE_G -> "3G"
    PitchSurface.FOUR_G -> "4G"
    PitchSurface.ASTRO -> "Astro"
    PitchSurface.INDOOR -> "Indoor"
    null -> value
}

/**
 * A Google Maps directions link to [venue] - the same format the API's fixture
 * confirmation emails and gffh-web use: the exact coordinates when known, else
 * the postcode, else the address. Null when there's nothing to navigate to.
 */
fun directionsUrl(venue: VenueSummary): String? {
    val base = "https://www.google.com/maps/dir/?api=1&destination="
    val lat = venue.latitude
    val lng = venue.longitude
    if (lat != null && lng != null) return "$base${coordinate(lat)},${coordinate(lng)}"
    val place = venue.postcode?.trim()?.ifEmpty { null } ?: venue.address?.trim()?.ifEmpty { null } ?: return null
    return base + place.encodeURLParameter()
}

/**
 * Plain decimal to 6 places (about 10cm), trailing zeros dropped. Double's own
 * toString switches to "5.0E-4" for small values, which Maps wouldn't read -
 * and a pitch near the Greenwich meridian has a longitude that small.
 */
private fun coordinate(value: Double): String {
    val micro = (value * 1_000_000).roundToLong()
    val sign = if (micro < 0) "-" else ""
    val whole = abs(micro) / 1_000_000
    val fraction = (abs(micro) % 1_000_000).toString().padStart(6, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}
