package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.repository.MatchRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch
import com.gffh.mobile.ui.components.codeLabel

/** How many ranked opponents one search returns. */
internal const val SEARCH_LIMIT = 50

/**
 * SCR-FF-01 Search entry. Purpose: get the user to a ranked result set with
 * one tap.
 *
 * The search opens with no filters: the API returns every team with the same
 * age group and gender, all divisions and formats, ranked by fit, and
 * optional filters are set from the results (SCR-FF-02). Availability is
 * not required either - a new team sees who is out there before publishing
 * any dates, so the add-availability prompt is a tip, not a gate.
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
            summaryText = "Every ${codeLabel(teamView.ageGroup)} ${codeLabel(teamView.gender).lowercase()} team, all divisions, best fit first"
        }
    }

    fun runSearch() {
        val t = team ?: return
        searching = true
        errorMessage = null
        filterState.reset()
        scope.launch {
            when (val result = matchRepository.search(filterState.toRequest(t.teamId, SEARCH_LIMIT))) {
                is ApiResult.Success -> { resultsCache.store(result.value); navigator.push(Route.Results) }
                is ApiResult.Failure -> errorMessage = result.message
            }
            searching = false
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Find a friendly", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { runSearch() },
            enabled = !searching && team != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (searching) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Find me a friendly")
        }
        if (summaryText.isNotBlank()) {
            Text(summaryText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }

        if (hasAvailability == false) {
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Add your availability", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "You can browse teams now. Add dates so you can send invitations and rank higher for teams free when you are.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { navigator.push(Route.EditAvailabilitySlot()) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Add availability") }
                }
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
