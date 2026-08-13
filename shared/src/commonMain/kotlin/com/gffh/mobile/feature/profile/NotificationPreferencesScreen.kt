package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.NotificationPreferenceView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.NotificationRepository
import kotlinx.coroutines.launch

private data class PrefRow(val label: String, val help: String, val get: (NotificationPreferenceView) -> Boolean,
                            val set: (NotificationPreferenceView, Boolean) -> NotificationPreferenceView)

private val ROWS = listOf(
    PrefRow("Friendly requests", "New requests, declines, suggested changes and withdrawals",
        { it.friendlyRequests }, { p, v -> p.copy(friendlyRequests = v) }),
    PrefRow("Fixtures", "Confirmations and cancellations",
        { it.fixtures }, { p, v -> p.copy(fixtures = v) }),
    PrefRow("Verification", "Approval or rejection of a submission",
        { it.verification }, { p, v -> p.copy(verification = v) }),
    PrefRow("Fixture messages", "New messages from the other team on a confirmed fixture",
        { it.messages }, { p, v -> p.copy(messages = v) }),
)

/** SCR-PR-09. Purpose: let a manager choose which events actually create a notification for them. */
@Composable
fun NotificationPreferencesScreen(notificationRepository: NotificationRepository, navigator: Navigator) {
    var prefs by remember { mutableStateOf<NotificationPreferenceView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (val result = notificationRepository.getPreferences()) {
            is ApiResult.Success -> prefs = result.value
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    fun toggle(row: PrefRow) {
        val current = prefs ?: return
        val updated = row.set(current, !row.get(current))
        prefs = updated
        scope.launch {
            val result = notificationRepository.updatePreferences(updated)
            if (result is ApiResult.Failure) {
                prefs = current
                errorMessage = result.message
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Notifications", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Choose which events show up in your notification centre.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val current = prefs
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            if (current != null) {
                ROWS.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(row.label, style = MaterialTheme.typography.bodyMedium)
                            Text(row.help, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = row.get(current), onCheckedChange = { toggle(row) })
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
