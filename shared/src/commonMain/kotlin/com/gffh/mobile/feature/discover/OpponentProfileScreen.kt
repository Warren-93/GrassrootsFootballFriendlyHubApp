package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.ConversationRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch

/**
 * SCR-FF-05 Opponent profile. Purpose: give a manager everything needed to
 * decide whether to invite this team.
 *
 * Reads the candidate from [SearchResultsCache] rather than fetching
 * `GET /api/v1/teams/{id}` - see the cache's doc comment for why: that
 * endpoint requires managing the team, which a searching manager never does
 * for an opponent. Contact details and exact venue are correctly never
 * shown here, matching the privacy rule. Messaging is available here too,
 * as soon as the other team has published availability - it does not wait
 * for a friendly request to exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpponentProfileScreen(
    conversationRepository: ConversationRepository,
    currentTeamStore: CurrentTeamStore,
    resultsCache: SearchResultsCache,
    navigator: Navigator,
    teamId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    val match = resultsCache.find(teamId)
    var opening by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun openMessages() {
        val ours = activeTeam ?: return
        opening = true
        messageError = null
        scope.launch {
            when (val result = conversationRepository.start(ours.teamId, teamId)) {
                is ApiResult.Success -> navigator.push(Route.ConversationThread(result.value.id))
                is ApiResult.Failure -> messageError = result.message
            }
            opening = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(match?.team?.name ?: "Team") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (match == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text("This team is no longer in your search results. Search again to see them.")
            }
            return
        }

        Column(Modifier.padding(24.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(match.team.name, style = MaterialTheme.typography.headlineSmall)
                if (match.team.verified) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Verified, contentDescription = "Verified")
                }
            }
            Text(match.team.clubName, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))
            AssistChip(onClick = { navigator.push(Route.MatchExplanation(teamId)) }, label = { Text("${match.score}% match") })

            Spacer(Modifier.height(16.dp))
            InfoRow("Age group", match.team.ageGroup)
            InfoRow("Format", match.team.format)
            InfoRow("Ability", match.team.abilityLevel)
            match.team.league?.let { InfoRow("League", it) }
            InfoRow("Distance", "${match.milesApart} miles")
            match.team.generalArea?.let { InfoRow("Area", it) }

            if (match.reasons.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Why this match", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    match.reasons.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                }
            }

            match.earliestOverlap?.let { overlap ->
                Spacer(Modifier.height(16.dp))
                Text("Overlapping availability", style = MaterialTheme.typography.titleSmall)
                Text("${overlap.date}, from ${overlap.startTime} (${overlap.overlapMinutes} min overlap)")
            }

            if (!match.team.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Description", style = MaterialTheme.typography.titleSmall)
                Text(match.team.description)
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            messageError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            Button(
                onClick = { navigator.push(Route.InvitationComposer(match.team.id)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Invite to friendly") }
            OutlinedButton(
                onClick = { openMessages() },
                enabled = !opening && activeTeam != null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (opening) "Opening…" else "Message this team")
            }
            TextButton(
                onClick = { navigator.push(Route.ReportBlock(match.team.id, match.team.name)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Report or block", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
