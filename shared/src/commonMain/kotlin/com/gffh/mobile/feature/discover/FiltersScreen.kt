package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.model.AbilityLevel
import com.gffh.mobile.model.Format
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.session.SearchFilterState

/**
 * SCR-FF-02 Filters. Purpose: allow precise control over the result set
 * without losing the user in options.
 *
 * Age group is omitted entirely: the spec says it's "disabled while exact
 * age matching is mandatory" (open question 3), and the backend's
 * `findCandidates` only ever filters by the searching team's own age group
 * regardless - see the backend README's "Outstanding" section. The live
 * result-count preview is also omitted; it needs a count-only endpoint that
 * doesn't exist, and the spec explicitly allows it to "degrade silently".
 */
@Composable
fun FiltersScreen(filterState: SearchFilterState, navigator: Navigator) {
    var distance by remember { mutableStateOf(filterState.maxDistanceMiles?.toFloat() ?: 25f) }
    var distanceEnabled by remember { mutableStateOf(filterState.maxDistanceMiles != null) }
    var formats by remember { mutableStateOf(filterState.formats) }
    var abilities by remember { mutableStateOf(filterState.abilityLevels) }
    var verifiedOnly by remember { mutableStateOf(filterState.verifiedOnly) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Filters", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = distanceEnabled, onCheckedChange = { distanceEnabled = it })
            Text("Distance: ${distance.toInt()} miles")
        }
        if (distanceEnabled) {
            Slider(value = distance, onValueChange = { distance = it }, valueRange = 5f..50f, steps = 8)
        }

        Spacer(Modifier.height(16.dp))
        Text("Format", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Format.entries.forEach { f ->
                FilterChip(
                    selected = f.name in formats,
                    onClick = { formats = if (f.name in formats) formats - f.name else formats + f.name },
                    label = { Text(f.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Ability", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityLevel.entries.forEach { a ->
                FilterChip(
                    selected = a.name in abilities,
                    onClick = { abilities = if (a.name in abilities) abilities - a.name else abilities + a.name },
                    label = { Text(a.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
            Text("Verified teams only")
        }

        Spacer(Modifier.weight(1f))
        Row {
            OutlinedButton(
                onClick = {
                    filterState.reset()
                    distance = 25f; distanceEnabled = false; formats = emptySet(); abilities = emptySet(); verifiedOnly = false
                },
                modifier = Modifier.weight(1f)
            ) { Text("Reset") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    filterState.maxDistanceMiles = if (distanceEnabled) distance.toInt() else null
                    filterState.formats = formats
                    filterState.abilityLevels = abilities
                    filterState.verifiedOnly = verifiedOnly
                    navigator.pop()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Apply") }
        }
    }
}
