package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.MatchSummary
import com.gffh.mobile.model.SearchRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.MatchRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch

/**
 * SCR-FF-03 Results list. Purpose: present ranked opponents so fit can be
 * judged without opening each profile. Empty results render SCR-FF-07's
 * next-action content inline, since the spec frames it as this screen's
 * Empty state rather than a separate destination.
 *
 * The map view toggle (SCR-FF-04) is omitted - it needs a maps SDK
 * integration that's out of scope.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsListScreen(
    matchRepository: MatchRepository,
    currentTeamStore: CurrentTeamStore,
    filterState: SearchFilterState,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    val response by resultsCache.lastResponse.collectAsState()
    var relaxing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun relaxAndResearch(mutate: SearchFilterState.() -> Unit) {
        val team = currentTeamStore.active.value ?: return
        filterState.mutate()
        relaxing = true
        scope.launch {
            val request = SearchRequest(
                teamId = team.teamId,
                maxDistanceMiles = filterState.maxDistanceMiles,
                formats = filterState.formats.toList().ifEmpty { null },
                abilityLevels = filterState.abilityLevels.toList().ifEmpty { null },
                verifiedOnly = filterState.verifiedOnly.takeIf { it }
            )
            (matchRepository.search(request) as? ApiResult.Success)?.let { resultsCache.store(it.value) }
            relaxing = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${response?.totalResults ?: 0} team${if (response?.totalResults == 1) "" else "s"} match") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        val results = response?.results.orEmpty()
        if (relaxing) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (results.isEmpty()) {
            NoResultsContent(
                onRelaxDistance = { relaxAndResearch { maxDistanceMiles = (maxDistanceMiles ?: 15) + 25 } },
                onRelaxAbility = { relaxAndResearch { abilityLevels = emptySet() } },
                onOpenFilters = { navigator.push(Route.Filters) }
            )
        } else {
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(results) { match ->
                    TeamCard(
                        match,
                        onCardClick = { navigator.push(Route.OpponentProfile(match.team.id)) },
                        onScoreClick = { navigator.push(Route.MatchExplanation(match.team.id)) },
                        onInviteClick = { navigator.push(Route.InvitationComposer(match.team.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamCard(match: MatchSummary, onCardClick: () -> Unit, onScoreClick: () -> Unit, onInviteClick: () -> Unit) {
    Card(onClick = onCardClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(match.team.name, style = MaterialTheme.typography.titleMedium)
                        if (match.team.verified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        "${match.team.clubName} - ${match.team.ageGroup} - ${match.team.format}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ScoreChip(match.score, match.band, onClick = onScoreClick)
            }
            Spacer(Modifier.height(8.dp))
            Text("${match.milesApart} miles away", style = MaterialTheme.typography.bodySmall)
            match.earliestOverlap?.let {
                Text("First free: ${it.date} ${it.startTime}", style = MaterialTheme.typography.bodySmall)
            }
            if (match.reasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    match.reasons.take(3).forEach { reason -> AssistChip(onClick = onScoreClick, label = { Text(reason) }) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onInviteClick, modifier = Modifier.fillMaxWidth()) { Text("Invite") }
        }
    }
}

@Composable
private fun ScoreChip(score: Int, band: String, onClick: () -> Unit) {
    val color = when (band) {
        "HIGH" -> MaterialTheme.colorScheme.primary
        "MID" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    AssistChip(onClick = onClick, label = { Text("$score%") }, colors = AssistChipDefaults.assistChipColors(labelColor = color))
}

@Composable
private fun NoResultsContent(onRelaxDistance: () -> Unit, onRelaxAbility: () -> Unit, onOpenFilters: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("No teams match right now", style = MaterialTheme.typography.titleMedium)
        Text(
            "Try widening your search.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        ActionRow("Search further away", onRelaxDistance)
        ActionRow("Include other ability levels", onRelaxAbility)
        ActionRow("Adjust filters", onOpenFilters)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.padding(16.dp))
    }
}
