package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SubmitVerificationRequest
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.VerificationRequestView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.repository.VerificationRepository
import kotlinx.coroutines.launch

/**
 * SCR-PR-07 Verification submission. Purpose: let a manager put their team
 * forward for admin review (ADM-03), and see where that review stands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    verificationRepository: VerificationRepository,
    teamRepository: TeamRepository,
    navigator: Navigator,
    teamId: String
) {
    var team by remember { mutableStateOf<TeamView?>(null) }
    var request by remember { mutableStateOf<VerificationRequestView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var affiliationNumber by remember { mutableStateOf("") }
    var contactDetails by remember { mutableStateOf("") }
    var evidenceUrls by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(teamId) {
        when (val teamResult = teamRepository.get(teamId)) {
            is ApiResult.Success -> team = teamResult.value
            is ApiResult.Failure -> errorMessage = teamResult.message
        }
        when (val reqResult = verificationRepository.getForTeam(teamId)) {
            is ApiResult.Success -> request = reqResult.value
            is ApiResult.Failure -> {}
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Verification") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        val current = team
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "Team not found.") }
            return@Column
        }

        val underReview = request?.status == "PENDING" || request?.status == "AWAITING_SECOND_REJECTION"

        Column(Modifier.padding(24.dp).weight(1f).verticalScroll(rememberScrollState())) {
            AssistChip(onClick = {}, label = { Text(current.verification) })
            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
            }

            if (current.verification == "VERIFIED") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "This team is verified. Verified teams get priority visibility in search.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (underReview && request != null) {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Under review", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Submitted ${request!!.submittedAt.take(10)}. We'll let you know once an admin has reviewed it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (request?.status == "REJECTED") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        (request!!.finalRejectionReason ?: "This submission was rejected.") + " You can submit again below.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (!underReview && current.verification != "VERIFIED") {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Put this team forward for verification. An admin will review your details and evidence.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = affiliationNumber,
                    onValueChange = { affiliationNumber = it },
                    label = { Text("League/FA affiliation number (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = contactDetails,
                    onValueChange = { contactDetails = it },
                    label = { Text("Contact details") },
                    supportingText = { Text("How an admin can reach you if they have questions") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = evidenceUrls,
                    onValueChange = { evidenceUrls = it },
                    label = { Text("Evidence links") },
                    supportingText = { Text("One link per line - league registration, club website, social media, etc.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        submitting = true
                        errorMessage = null
                        scope.launch {
                            val urls = evidenceUrls.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            val result = verificationRepository.submit(
                                teamId,
                                SubmitVerificationRequest(affiliationNumber.trim().ifBlank { null }, contactDetails.trim(), urls)
                            )
                            submitting = false
                            when (result) {
                                is ApiResult.Success -> {
                                    request = result.value
                                    team = current.copy(verification = "PENDING")
                                }
                                is ApiResult.Failure -> errorMessage = result.message
                            }
                        }
                    },
                    enabled = !submitting && contactDetails.trim().isNotEmpty() && evidenceUrls.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Submit for verification")
                }
            }
        }
    }
}
