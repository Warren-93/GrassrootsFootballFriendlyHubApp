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
import com.gffh.mobile.model.FriendlyRequestView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.FriendlyRequestRepository
import kotlinx.coroutines.launch

/**
 * SCR-IN-04 Request detail. Purpose: let the recipient evaluate and resolve a
 * proposal, and let the sender track it.
 *
 * Actions render strictly from `availableActions`, exactly per the spec's
 * rule ("actions the state does not permit are hidden, not disabled") - the
 * backend's `RequestStateMachine` is the single source of truth for this,
 * the client never re-derives it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(friendlyRequestRepository: FriendlyRequestRepository, navigator: Navigator, requestId: String) {
    var request by remember { mutableStateOf<FriendlyRequestView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            when (val result = friendlyRequestRepository.get(requestId)) {
                is ApiResult.Success -> { request = result.value; loading = false }
                is ApiResult.Failure -> { errorMessage = result.message; loading = false }
            }
        }
    }

    LaunchedEffect(requestId) { reload() }

    fun perform(action: String) {
        scope.launch {
            when (val result = friendlyRequestRepository.act(requestId, action)) {
                is ApiResult.Success -> request = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Friendly request") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        val r = request
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "Not found") }
            return
        }

        Column(Modifier.padding(24.dp).weight(1f)) {
            StatusPill(r.status)
            Spacer(Modifier.height(16.dp))
            InfoRow("Date", r.date)
            InfoRow("Kick-off", r.startTime)
            InfoRow("Cost share", r.costShare)
            InfoRow("Referee", r.refereeArrangement)
            r.message?.let {
                Spacer(Modifier.height(12.dp))
                Text("Message", style = MaterialTheme.typography.titleSmall)
                Text(it)
            }

            val contact = r.senderContact ?: r.recipientContact
            if (contact != null) {
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Contact", style = MaterialTheme.typography.titleSmall)
                        contact.managerName?.let { Text(it) }
                        contact.contactPhone?.let { Text(it) }
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(Modifier.padding(16.dp)) {
            r.availableActions.forEach { action ->
                when (action) {
                    "accept" -> Button(
                        onClick = { confirmAction = action },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) { Text("Accept") }
                    "suggestChanges" -> OutlinedButton(
                        onClick = { navigator.push(Route.SuggestChanges(requestId)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) { Text("Suggest changes") }
                    "decline" -> TextButton(
                        onClick = { navigator.push(Route.DeclineRequest(requestId)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Decline", color = MaterialTheme.colorScheme.error) }
                    "withdraw" -> TextButton(
                        onClick = { confirmAction = action },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Withdraw", color = MaterialTheme.colorScheme.error) }
                    "cancel" -> TextButton(
                        onClick = { confirmAction = action },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel fixture", color = MaterialTheme.colorScheme.error) }
                    "acceptChanges", "counter" -> Button(
                        onClick = { confirmAction = action },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) { Text(if (action == "acceptChanges") "Accept changes" else "Send again") }
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Confirm") },
            text = { Text(consequenceText(action)) },
            confirmButton = {
                TextButton(onClick = { perform(action); confirmAction = null }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
    }
}

private fun consequenceText(action: String): String = when (action) {
    "accept" -> "A fixture will be created and the opponent notified."
    "withdraw" -> "This request will be withdrawn."
    "cancel" -> "The opponent will be notified immediately."
    "acceptChanges" -> "The updated terms will be sent back for their final answer."
    "counter" -> "This sends the request back to them unchanged, for a fresh answer."
    else -> "Are you sure?"
}

@Composable
private fun StatusPill(status: String) {
    Card { Text(status, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
