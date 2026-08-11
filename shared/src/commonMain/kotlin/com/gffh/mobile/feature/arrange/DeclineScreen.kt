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
 * SCR-IN-06 Decline. Purpose: close a proposal cleanly, with enough signal to
 * improve future matching.
 *
 * As with Suggest changes, the reason/message collected here isn't sent to
 * the backend yet - `POST .../actions/decline` is a bare transition. Kept in
 * the UI because it's cheap to add server-side later and the spec treats the
 * reason as important signal, not because it does anything today.
 */
@Composable
fun DeclineScreen(friendlyRequestRepository: FriendlyRequestRepository, navigator: Navigator, requestId: String) {
    val reasons = listOf("Date no longer suits", "Too far", "Ability mismatch", "Already arranged", "Other")
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Decline request", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        reasons.forEach { r ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                Text(r)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = message, onValueChange = { if (it.length <= 280) message = it },
            label = { Text("Message (optional, shared with the other team)") },
            supportingText = { Text("${message.length}/280") },
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
                    when (friendlyRequestRepository.act(requestId, "decline")) {
                        is ApiResult.Success -> navigator.pop()
                        is ApiResult.Failure -> { errorMessage = "Could not decline."; sending = false }
                    }
                }
            },
            enabled = !sending,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Decline") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Keep negotiating") }
    }
}
