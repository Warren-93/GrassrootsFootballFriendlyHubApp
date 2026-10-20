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
import com.gffh.mobile.model.MatchSummary
import com.gffh.mobile.model.formatMatchDate
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.session.SearchFilterState
import com.gffh.mobile.session.SearchResultsCache
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.MatchScoreChip
import com.gffh.mobile.ui.components.codeLabel

/**
 * SCR-FF-03 Results list. Purpose: present ranked opponents so fit can be
 * judged without opening each profile. Empty results render SCR-FF-07's
 * next-action content inline, since the spec frames it as this screen's
 * Empty state rather than a separate destination.
 *
 * The map view (SCR-FF-04) is its own screen ([ResultsMapScreen]) rather
 * than an inline toggle here, reachable via the app bar action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsListScreen(
    filterState: SearchFilterState,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    val response by resultsCache.lastResponse.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("${response?.totalResults ?: 0} team${if (response?.totalResults == 1) "" else "s"} match") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                val active = filterState.activeCount()
                TextButton(onClick = { navigator.push(Route.Filters) }) { Text(if (active > 0) "Filters ($active)" else "Filters") }
                TextButton(onClick = { navigator.push(Route.ResultsMap) }) { Text("Map") }
            }
        )

        val results = response?.results.orEmpty()
        if (results.isEmpty()) {
            NoResultsContent(filtered = filterState.activeCount() > 0, onOpenFilters = { navigator.push(Route.Filters) })
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
                CrestAvatar(match.team.name, badgeUrl = match.team.badgeUrl)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(match.team.name, style = MaterialTheme.typography.titleMedium)
                        if (match.team.verified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        "${match.team.clubName} - ${codeLabel(match.team.ageGroup)} - ${codeLabel(match.team.format)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                MatchScoreChip(match.score, onClick = onScoreClick)
            }
            Spacer(Modifier.height(8.dp))
            Text("${match.milesApart} miles away", style = MaterialTheme.typography.bodySmall)
            val overlapDates = match.overlappingDates.ifEmpty { listOfNotNull(match.earliestOverlap?.date) }
            if (overlapDates.isNotEmpty()) {
                Text(
                    "Shared published dates: ${overlapDates.joinToString { formatMatchDate(it) }}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (match.reasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    match.reasons.take(3).forEach { reason -> AssistChip(onClick = onScoreClick, label = { Text(reason) }) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onInviteClick,
                enabled = match.canReceiveRequests != false,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (match.canReceiveRequests == false) "Can't receive requests yet" else "Invite") }
        }
    }
}

@Composable
private fun NoResultsContent(filtered: Boolean, onOpenFilters: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("No teams to show yet", style = MaterialTheme.typography.titleMedium)
        Text(
            if (filtered) "No teams match these filters. Clear them to see every team in your age group and gender."
            else "No other teams in your age group and gender have joined yet. Check back soon.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (filtered) {
            TextButton(onClick = onOpenFilters, modifier = Modifier.padding(top = 8.dp)) { Text("Change filters") }
        }
    }
}
