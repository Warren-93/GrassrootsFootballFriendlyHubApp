package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SendFriendlyRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.FriendlyRequestRepository
import com.gffh.mobile.session.InvitationDraftState
import kotlinx.coroutines.launch

/**
 * SCR-IN-02 Invitation review. Purpose: give one last unambiguous read before
 * a commitment is sent to another club.
 */
@Composable
fun InvitationReviewScreen(
    friendlyRequestRepository: FriendlyRequestRepository,
    draft: InvitationDraftState,
    navigator: Navigator
) {
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Review proposal", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SummaryRow("Opponent", draft.opponentTeamName)
                SummaryRow("Date", draft.date)
                SummaryRow("Kick-off", draft.startTime)
                SummaryRow("Duration", "${draft.startTime} - ${draft.endTime}")
                SummaryRow("Home team", if (draft.isSenderHome) "You" else draft.opponentTeamName)
                if (draft.isSenderHome) SummaryRow("Venue", draft.venueName ?: "Not set")
                SummaryRow("Referee", draft.refereeArrangement)
                SummaryRow("Cost share", draft.costShare)
            }
        }

        if (draft.notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleSmall)
                    Text(draft.notes)
                }
            }
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val senderTeamId = draft.senderTeamId ?: return@Button
                val opponentTeamId = draft.opponentTeamId ?: return@Button
                val senderSlotId = draft.senderSlotId ?: return@Button
                val recipientSlotId = draft.recipientSlotId ?: return@Button
                sending = true
                errorMessage = null
                scope.launch {
                    val result = friendlyRequestRepository.send(
                        SendFriendlyRequest(
                            senderTeamId = senderTeamId,
                            recipientTeamId = opponentTeamId,
                            senderSlotId = senderSlotId,
                            recipientSlotId = recipientSlotId,
                            date = draft.date,
                            startTime = draft.startTime.let { if (it.length == 5) "$it:00" else it },
                            endTime = draft.endTime.let { if (it.length == 5) "$it:00" else it },
                            venueId = draft.venueId,
                            homeTeamId = draft.homeTeamId,
                            costShare = draft.costShare,
                            refereeArrangement = draft.refereeArrangement,
                            message = draft.notes.ifBlank { null }
                        )
                    )
                    sending = false
                    when (result) {
                        is ApiResult.Success -> {
                            val requestId = result.value.id
                            draft.clear()
                            // Discards Composer/Review from the stack so back never resubmits.
                            navigator.resetTo(Route.InvitationSent(requestId))
                        }
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = !sending,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Send invitation")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Edit") }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
