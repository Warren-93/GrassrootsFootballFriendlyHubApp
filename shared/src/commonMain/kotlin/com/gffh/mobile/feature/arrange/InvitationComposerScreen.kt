package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.InvitationDraftState
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.datetime.LocalTime

/** Mirrors the backend's AgeGroup.defaultMatchMinutes() table (SCR-IN-01's "Match duration" default). */
private fun defaultMatchMinutes(ageGroup: String?): Int = when (ageGroup) {
    "U7", "U8", "U9" -> 40
    "U10", "U11", "U12" -> 50
    "U13", "U14" -> 60
    "U15", "U16" -> 70
    "U17", "U18" -> 80
    else -> 90
}

/**
 * SCR-IN-01 Invitation composer. Purpose: turn a match into a concrete,
 * unambiguous proposal.
 *
 * Format isn't editable here - the backend's `SendFriendlyRequest` doesn't
 * carry one (a friendly is always played at whichever format both teams'
 * profiles already agree on, scored by the matching engine, not renegotiated
 * per-invitation). Referee cost is likewise not modelled server-side yet, so
 * it's omitted rather than collected and silently dropped.
 */
@Composable
fun InvitationComposerScreen(
    teamRepository: TeamRepository,
    venueRepository: VenueRepository,
    currentTeamStore: CurrentTeamStore,
    resultsCache: SearchResultsCache,
    draft: InvitationDraftState,
    navigator: Navigator,
    opponentTeamId: String
) {
    val activeTeam = currentTeamStore.active.collectAsState().value
    val match = resultsCache.find(opponentTeamId)

    var ourTeam by remember { mutableStateOf<TeamView?>(null) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var selectedVenueId by remember { mutableStateOf<String?>(null) }
    var isSenderHome by remember { mutableStateOf(true) }
    var costShare by remember { mutableStateOf("SPLIT") }
    var refereeArrangement by remember { mutableStateOf("NONE") }
    var notes by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(activeTeam?.teamId) {
        val t = activeTeam ?: return@LaunchedEffect
        (teamRepository.get(t.teamId) as? ApiResult.Success)?.let { ourTeam = it.value }
        (venueRepository.list(t.clubId) as? ApiResult.Success)?.let { result ->
            venues = result.value
            selectedVenueId = result.value.firstOrNull { it.isDefault }?.id
        }
        loading = false
    }

    if (loading || match == null || activeTeam == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (match == null && !loading) Text("This match is no longer available. Search again.")
            else CircularProgressIndicator()
        }
        return
    }

    val overlap = match.earliestOverlap
    if (overlap == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { Text("No overlapping availability with this team.") }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Invite ${match.team.name}", style = MaterialTheme.typography.headlineSmall)
        Text(match.team.clubName, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(20.dp))
        Text("Date: ${overlap.date}", style = MaterialTheme.typography.bodyMedium)
        Text("Kick-off: ${overlap.startTime}", style = MaterialTheme.typography.bodyMedium)
        Text("Overlap window: ${overlap.overlapMinutes} minutes", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(16.dp))
        Text("Home team", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isSenderHome, onClick = { isSenderHome = true }, label = { Text(ourTeam?.name ?: "Us") })
            FilterChip(selected = !isSenderHome, onClick = { isSenderHome = false }, label = { Text(match.team.name) })
        }

        if (isSenderHome && venues.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Venue", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                venues.forEach { v ->
                    FilterChip(selected = selectedVenueId == v.id, onClick = { selectedVenueId = v.id }, label = { Text(v.name) })
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Referee", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("NONE", "CLUB_SUPPLIED", "APPOINTED").forEach { opt ->
                FilterChip(selected = refereeArrangement == opt, onClick = { refereeArrangement = opt }, label = { Text(opt) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Cost share", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("NONE", "SPLIT", "HOST_PAYS", "VISITOR_PAYS").forEach { opt ->
                FilterChip(selected = costShare == opt, onClick = { costShare = opt }, label = { Text(opt) })
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = notes, onValueChange = { if (it.length <= 500) notes = it },
            label = { Text("Notes to opponent (optional)") },
            supportingText = { Text("${notes.length}/500") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                draft.opponentTeamId = match.team.id
                draft.opponentTeamName = match.team.name
                draft.senderTeamId = activeTeam.teamId
                draft.senderSlotId = overlap.ourSlotId
                draft.recipientSlotId = overlap.theirSlotId
                draft.date = overlap.date
                draft.startTime = overlap.startTime
                val durationMinutes = minOf(defaultMatchMinutes(ourTeam?.ageGroup), overlap.overlapMinutes)
                val start = LocalTime.parse(overlap.startTime.let { if (it.length == 5) "$it:00" else it })
                val endSeconds = (start.toSecondOfDay() + durationMinutes * 60).coerceAtMost(24 * 3600 - 1)
                draft.endTime = LocalTime.fromSecondOfDay(endSeconds).toString()
                draft.venueId = if (isSenderHome) selectedVenueId else null
                draft.venueName = if (isSenderHome) venues.firstOrNull { it.id == selectedVenueId }?.name else null
                draft.homeTeamId = if (isSenderHome) activeTeam.teamId else match.team.id
                draft.isSenderHome = isSenderHome
                draft.costShare = costShare
                draft.refereeArrangement = refereeArrangement
                draft.notes = notes
                navigator.push(Route.InvitationReview)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Review proposal") }
    }
}
