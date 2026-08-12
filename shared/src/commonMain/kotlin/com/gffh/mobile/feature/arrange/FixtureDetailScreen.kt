package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/**
 * SCR-FX-04 Fixture detail. Purpose: be the single reference for everything
 * about an agreed match. Contact details and the exact venue appear here
 * because a fixture only exists once its request reached CONFIRMED - the
 * server-side disclosure rule that makes this screen safe to build plainly.
 *
 * Cancel is omitted here - it belongs on the underlying friendly request
 * (SCR-IN-04's "cancel" action, already built), and messaging (SCR-FX-05)
 * isn't part of this pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixtureDetailScreen(
    fixtureRepository: FixtureRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    fixtureId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var fixture by remember { mutableStateOf<FixtureView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(fixtureId) {
        scope.launch {
            when (val result = fixtureRepository.get(fixtureId)) {
                is ApiResult.Success -> fixture = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Fixture") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        val f = fixture
        if (f == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "Not found") }
            return
        }

        Column(Modifier.padding(24.dp)) {
            Text("${f.homeTeam.name} vs ${f.awayTeam.name}", style = MaterialTheme.typography.headlineSmall)
            Text("${f.date}, kick-off ${f.startTime}", style = MaterialTheme.typography.bodyMedium)
            Text(f.status, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))
            SectionCard("Home") {
                Text(f.homeTeam.name)
                f.homeTeam.managerName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                f.homeTeam.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(Modifier.height(12.dp))
            SectionCard("Away") {
                Text(f.awayTeam.name)
                f.awayTeam.managerName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                f.awayTeam.contactPhone?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }

            Spacer(Modifier.height(12.dp))
            SectionCard("Arrangements") {
                Text("Cost share: ${f.costShare}", style = MaterialTheme.typography.bodySmall)
                Text("Referee: ${f.refereeArrangement}", style = MaterialTheme.typography.bodySmall)
                f.venueId?.let { Text("Venue: $it", style = MaterialTheme.typography.bodySmall) }
            }

            activeTeam?.let { ours ->
                val other = if (ours.teamId == f.homeTeam.id) f.awayTeam else f.homeTeam
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { navigator.push(Route.ReportBlock(other.id, other.name, f.id)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Report or block", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
