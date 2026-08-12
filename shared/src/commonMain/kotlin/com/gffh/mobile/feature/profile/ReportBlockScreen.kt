package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.BlockRequest
import com.gffh.mobile.model.SubmitReportRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.ReportRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

private data class ReasonOption(val value: String, val label: String, val severity: String, val blockByDefault: Boolean)

private val REASONS = listOf(
    ReasonOption("SAFEGUARDING", "Safeguarding concern", "SAFEGUARDING", true),
    ReasonOption("ABUSIVE_BEHAVIOUR", "Abusive behaviour", "HIGH", true),
    ReasonOption("FAKE_OR_MISLEADING_TEAM", "Fake or misleading team", "MEDIUM", false),
    ReasonOption("SPAM", "Spam", "LOW", false),
    ReasonOption("OTHER", "Other", "LOW", false),
)

/**
 * SCR-PR-11 Report / block. Purpose: an immediate, low-friction safeguarding
 * action from any surface where a team appears.
 */
@Composable
fun ReportBlockScreen(
    reportRepository: ReportRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    reportedTeamId: String,
    reportedTeamName: String,
    fixtureId: String?
) {
    val active by currentTeamStore.active.collectAsState()
    var reason by remember { mutableStateOf(REASONS[0]) }
    var details by remember { mutableStateOf("") }
    var alsoBlock by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val ourTeamId = active?.teamId
    if (ourTeamId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Nothing to report.") }
        return
    }

    if (done) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Report received", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                "Our team will review this." + if (alsoBlock) " $reportedTeamName has been blocked." else "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Report $reportedTeamName", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Text(
                "If there's an immediate risk to a child, report it to the police and your safeguarding officer " +
                    "directly - not only through this form.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Reason", style = MaterialTheme.typography.titleSmall)
        REASONS.forEach { option ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = reason == option,
                    onClick = { reason = option; alsoBlock = option.blockByDefault }
                )
                Text(option.label)
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = details,
            onValueChange = { if (it.length <= 1000) details = it },
            label = { Text(if (reason.value == "OTHER") "Detail (required)" else "Detail (optional)") },
            supportingText = { Text("${details.length}/1000") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Also block this team", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(checked = alsoBlock, onCheckedChange = { alsoBlock = it })
        }

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (reason.value == "OTHER" && details.isBlank()) {
                    errorMessage = "Please add detail - it is required when the reason is \"Other\"."
                    return@Button
                }
                submitting = true
                errorMessage = null
                scope.launch {
                    val reportResult = reportRepository.submit(
                        ourTeamId,
                        SubmitReportRequest(reportedTeamId, fixtureId, reason.value, reason.severity, details.trim())
                    )
                    if (reportResult is ApiResult.Failure) {
                        submitting = false
                        errorMessage = reportResult.message
                        return@launch
                    }
                    if (alsoBlock) {
                        val blockResult = reportRepository.block(ourTeamId, BlockRequest(reportedTeamId))
                        if (blockResult is ApiResult.Failure) {
                            submitting = false
                            errorMessage = "Report sent, but blocking failed: ${blockResult.message}"
                            return@launch
                        }
                    }
                    submitting = false
                    done = true
                }
            },
            enabled = !submitting,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (submitting) "Submitting…" else "Submit report") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
