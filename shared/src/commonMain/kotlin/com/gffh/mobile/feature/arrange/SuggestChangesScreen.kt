package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.FriendlyRequestRepository
import kotlinx.coroutines.launch

/**
 * SCR-IN-05 Suggest changes. Purpose: counter-propose without discarding what
 * both parties have already agreed.
 *
 * The reason is sent to and shown to the other team once they open the
 * request. Editing the terms themselves (date, venue, cost) isn't supported
 * yet - only a free-text counter-proposal.
 */
@Composable
fun SuggestChangesScreen(friendlyRequestRepository: FriendlyRequestRepository, navigator: Navigator, requestId: String) {
    var reason by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Suggest changes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Describe what you'd like to change - the other team will see this when they open the request.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = reason, onValueChange = { if (it.length <= 500) reason = it },
            label = { Text("Reason (optional)") },
            supportingText = { Text("${reason.length}/500") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                sending = true
                errorMessage = null
                scope.launch {
                    when (friendlyRequestRepository.act(requestId, "suggestChanges", reason.ifBlank { null })) {
                        is ApiResult.Success -> navigator.pop()
                        is ApiResult.Failure -> { errorMessage = "Could not send changes."; sending = false }
                    }
                }
            },
            enabled = !sending,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Send changes") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
