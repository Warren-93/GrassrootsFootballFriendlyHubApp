package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FixtureView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.ConversationRepository
import com.gffh.mobile.repository.FixtureRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/**
 * SCR-FX-04 Fixture detail. Purpose: be the single reference for everything
 * about an agreed match. Contact details and the exact venue appear here
 * because a fixture only exists once its request reached CONFIRMED - the
 * server-side disclosure rule that makes this screen safe to build plainly.
 *
 * SCR-FX-06 cancel is a dedicated button here (with a confirm dialog and
 * optional reason) rather than only reachable via the underlying friendly
 * request's generic cancel action. Messaging (SCR-FX-05) now opens the
 * team-to-team conversation thread rather than embedding a fixture-scoped
 * chat here - see ConversationThreadScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixtureDetailScreen(
    fixtureRepository: FixtureRepository,
    conversationRepository: ConversationRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    fixtureId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var fixture by remember { mutableStateOf<FixtureView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var opening by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelling by remember { mutableStateOf(false) }
    var cancelError by remember { mutableStateOf<String?>(null) }
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

    fun openMessages(f: FixtureView) {
        val ours = activeTeam ?: return
        val otherTeamId = if (ours.teamId == f.homeTeam.id) f.awayTeam.id else f.homeTeam.id
        opening = true
        messageError = null
        scope.launch {
            when (val result = conversationRepository.start(ours.teamId, otherTeamId)) {
                is ApiResult.Success -> navigator.push(Route.ConversationThread(result.value.id))
                is ApiResult.Failure -> messageError = result.message
            }
            opening = false
        }
    }

    fun cancelFixture() {
        cancelling = true
        cancelError = null
        scope.launch {
            when (val result = fixtureRepository.cancel(fixtureId, cancelReason.trim().ifBlank { null })) {
                is ApiResult.Success -> {
                    fixture = result.value
                    confirmCancel = false
                }
                is ApiResult.Failure -> cancelError = result.message
            }
            cancelling = false
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

        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
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

            Spacer(Modifier.height(16.dp))
            messageError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (activeTeam?.teamId == f.homeTeam.id || activeTeam?.teamId == f.awayTeam.id) {
                OutlinedButton(
                    onClick = { openMessages(f) },
                    enabled = !opening,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (opening) "Opening…" else "Message the other team")
                }
            }

            cancelError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            val weManage = activeTeam?.teamId == f.homeTeam.id || activeTeam?.teamId == f.awayTeam.id
            if (weManage && f.status == "CONFIRMED") {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel fixture") }
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

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this fixture?") },
            text = {
                Column {
                    Text("The other team will be notified. This can't be undone - you'd need to arrange a new friendly from scratch.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { cancelFixture() }, enabled = !cancelling) {
                    Text(if (cancelling) "Cancelling…" else "Cancel fixture")
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Back") } }
        )
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
