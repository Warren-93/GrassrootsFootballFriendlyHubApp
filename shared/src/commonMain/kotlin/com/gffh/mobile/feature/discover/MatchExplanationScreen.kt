package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.model.FactorView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.session.SearchResultsCache

/**
 * SCR-FF-06 Match explanation. Purpose: make the compatibility score
 * auditable, so the recommendation can be trusted.
 *
 * All reason and outcome strings are server-generated (see [FactorView]); this
 * screen renders them and never composes its own, matching the spec's rule.
 * No separate API call - reads the already-fetched search response, exactly
 * as the spec specifies for this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchExplanationScreen(resultsCache: SearchResultsCache, navigator: Navigator, teamId: String) {
    val match = resultsCache.find(teamId)

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Match explanation") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (match == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text("This match is no longer available. Search again.") }
            return
        }

        Column(Modifier.padding(24.dp)) {
            Text("${match.score}%", style = MaterialTheme.typography.displayMedium)
            Text("match with ${match.team.name}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))
            match.factors.forEach { factor -> FactorRow(factor) }

            Spacer(Modifier.height(16.dp))
            Text(
                "This score is a guide, not a guarantee - weights are set centrally and can change without an app update.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navigator.push(Route.InvitationComposer(match.team.id)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Invite to friendly") }
        }
    }
}

@Composable
private fun FactorRow(factor: FactorView) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${factor.label} (${factor.weight}%)", style = MaterialTheme.typography.titleSmall)
            Text("${factor.points} pts", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(progress = { factor.ratio.toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("You: ${factor.ourValue}", style = MaterialTheme.typography.bodySmall)
            Text("Them: ${factor.theirValue}", style = MaterialTheme.typography.bodySmall)
        }
        Text(factor.reason, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
