package com.gffh.mobile.feature.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SearchRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.MatchRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch

/**
 * SCR-FF-01 Search entry. Purpose: get the user to a ranked result set with
 * one tap, while allowing manual control.
 *
 * Quick match and manual search hit the same endpoint - the only difference
 * is which filters are supplied, exactly as the spec requires.
 */
@Composable
fun SearchEntryScreen(
    matchRepository: MatchRepository,
    teamRepository: TeamRepository,
    availabilityRepository: AvailabilityRepository,
    currentTeamStore: CurrentTeamStore,
    filterState: SearchFilterState,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    val team = currentTeamStore.active.collectAsState().value
    var hasAvailability by remember { mutableStateOf<Boolean?>(null) }
    var summaryText by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(team?.teamId) {
        val t = team ?: return@LaunchedEffect
        val teamView = (teamRepository.get(t.teamId) as? ApiResult.Success)?.value
        val slots = (availabilityRepository.list(t.teamId) as? ApiResult.Success)?.value.orEmpty()
        hasAvailability = slots.isNotEmpty()
        if (teamView != null) {
            val dateCount = slots.map { it.date }.toSet().size
            summaryText = "Your $dateCount published date${if (dateCount == 1) "" else "s"}, " +
                "${teamView.ageGroup}, ${teamView.format}, within ${teamView.travelRadiusMiles} miles"
        }
    }

    fun runSearch(useFilters: Boolean) {
        val t = team ?: return
        searching = true
        errorMessage = null
        scope.launch {
            val request = if (useFilters) {
                SearchRequest(
                    teamId = t.teamId,
                    fromDate = filterState.fromDate,
                    toDate = filterState.toDate,
                    formats = filterState.formats.toList().ifEmpty { null },
                    abilityLevels = filterState.abilityLevels.toList().ifEmpty { null },
                    maxDistanceMiles = filterState.maxDistanceMiles,
                    verifiedOnly = filterState.verifiedOnly.takeIf { it }
                )
            } else {
                SearchRequest(teamId = t.teamId)
            }
            when (val result = matchRepository.search(request)) {
                is ApiResult.Success -> { resultsCache.store(result.value); navigator.push(Route.Results) }
                is ApiResult.Failure -> errorMessage = result.message
            }
            searching = false
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Find a friendly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        if (hasAvailability == false) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Publish availability first", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Quick match uses your published dates as a filter - add one to search.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { navigator.push(Route.EditAvailabilitySlot()) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Add availability") }
                }
            }
        } else {
            Button(
                onClick = { runSearch(useFilters = false) },
                enabled = !searching && team != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (searching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Find me a friendly")
            }
            if (summaryText.isNotBlank()) {
                Text(
                    summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp).clickable { navigator.push(Route.Filters) }
                )
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { navigator.push(Route.Filters) }) { Text("More filters") }

            val filterCount = filterState.activeCount()
            if (filterCount > 0) {
                Button(
                    onClick = { runSearch(useFilters = true) },
                    enabled = !searching,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Search with $filterCount filter${if (filterCount == 1) "" else "s"}") }
            }
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
