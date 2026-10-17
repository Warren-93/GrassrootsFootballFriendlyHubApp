package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AbilityLevel
import com.gffh.mobile.model.Format
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.MatchRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch
import com.gffh.mobile.ui.components.codeLabel

/**
 * SCR-FF-02 Filters. Optional narrowing of the results: nothing selected
 * means every team in the same age group and gender. Age group and gender
 * aren't offered because they always match the searching team. Apply re-runs
 * the search and returns to the results.
 */
@Composable
fun FiltersScreen(
    matchRepository: MatchRepository,
    currentTeamStore: CurrentTeamStore,
    filterState: SearchFilterState,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    var distance by remember { mutableStateOf(filterState.maxDistanceMiles?.toFloat() ?: 25f) }
    var distanceEnabled by remember { mutableStateOf(filterState.maxDistanceMiles != null) }
    var formats by remember { mutableStateOf(filterState.formats) }
    var abilities by remember { mutableStateOf(filterState.abilityLevels) }
    var verifiedOnly by remember { mutableStateOf(filterState.verifiedOnly) }
    var applying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Filters", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Leave everything unselected to see every team in your age group and gender.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))

        Text("Division", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityLevel.entries.forEach { a ->
                FilterChip(
                    selected = a.name in abilities,
                    onClick = { abilities = if (a.name in abilities) abilities - a.name else abilities + a.name },
                    label = { Text(a.label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Format", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Format.entries.forEach { f ->
                FilterChip(
                    selected = f.name in formats,
                    onClick = { formats = if (f.name in formats) formats - f.name else formats + f.name },
                    label = { Text(codeLabel(f.name)) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = distanceEnabled, onCheckedChange = { distanceEnabled = it })
            Text(if (distanceEnabled) "Within ${distance.toInt()} miles" else "Any distance")
        }
        if (distanceEnabled) {
            Slider(value = distance, onValueChange = { distance = it }, valueRange = 5f..50f, steps = 8)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = verifiedOnly, onCheckedChange = { verifiedOnly = it })
            Text("Verified teams only")
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Row {
            OutlinedButton(
                onClick = { distance = 25f; distanceEnabled = false; formats = emptySet(); abilities = emptySet(); verifiedOnly = false },
                enabled = !applying,
                modifier = Modifier.weight(1f)
            ) { Text("Clear") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = {
                    val team = currentTeamStore.active.value ?: return@Button
                    filterState.maxDistanceMiles = if (distanceEnabled) distance.toInt() else null
                    filterState.formats = formats
                    filterState.abilityLevels = abilities
                    filterState.verifiedOnly = verifiedOnly
                    applying = true
                    errorMessage = null
                    scope.launch {
                        when (val result = matchRepository.search(filterState.toRequest(team.teamId, SEARCH_LIMIT))) {
                            is ApiResult.Success -> { resultsCache.store(result.value); navigator.pop() }
                            is ApiResult.Failure -> errorMessage = result.message
                        }
                        applying = false
                    }
                },
                enabled = !applying,
                modifier = Modifier.weight(1f)
            ) {
                if (applying) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Apply")
            }
        }
    }
}
