package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.image.BadgePickResult
import com.gffh.mobile.core.image.rememberBadgeImagePicker
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.UpdateTeamRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.ui.components.CrestAvatar
import kotlinx.coroutines.launch

/**
 * SCR-PR-02 Edit team. Purpose: amend team attributes, with guards on the
 * fields matching depends upon.
 *
 * The client doesn't know locally whether age group/format are locked
 * (`TeamView` doesn't expose `firstFixtureConfirmedAt`) - editing is always
 * offered, and a lock is surfaced as the server's own rejection
 * (`MATCHING_FIELDS_LOCKED`) rather than pre-emptively disabled here.
 */
@Composable
fun EditTeamScreen(
    teamRepository: TeamRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    teamId: String
) {
    var original by remember { mutableStateOf<TeamView?>(null) }
    var name by remember { mutableStateOf("") }
    var badge by remember { mutableStateOf("") }
    var badgeError by remember { mutableStateOf<String?>(null) }
    var league by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var travelRadius by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    var archiving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pickBadge = rememberBadgeImagePicker { result ->
        when (result) {
            is BadgePickResult.Picked -> {
                badge = result.dataUrl
                badgeError = null
            }
            BadgePickResult.Failed -> badgeError = "That file couldn't be read as an image."
            BadgePickResult.Cancelled -> {}
        }
    }

    LaunchedEffect(teamId) {
        when (val result = teamRepository.get(teamId)) {
            is ApiResult.Success -> {
                val t = result.value
                original = t
                name = t.name
                badge = t.badgeUrl ?: ""
                league = t.league ?: ""
                managerName = t.managerName ?: ""
                contactPhone = t.contactPhone ?: ""
                description = t.description ?: ""
                travelRadius = t.travelRadiusMiles.toString()
            }
            is ApiResult.Failure -> errorMessage = result.message
        }
    }

    val current = original
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (errorMessage != null) Text(errorMessage!!) else CircularProgressIndicator()
        }
        return
    }

    val badgeChanged = badge != (current.badgeUrl ?: "")
    val dirty = name != current.name || badgeChanged || league != (current.league ?: "") ||
        managerName != (current.managerName ?: "") || contactPhone != (current.contactPhone ?: "") ||
        description != (current.description ?: "") || travelRadius != current.travelRadiusMiles.toString()

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Edit team", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Badge", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrestAvatar(name.ifBlank { current.name }, size = 56.dp, badgeUrl = badge)
            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = pickBadge) { Text(if (badge.isBlank()) "Upload" else "Change") }
            if (badge.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { badge = ""; badgeError = null }) { Text("Remove") }
            }
        }
        Text(
            "A square image works best - we'll crop it to a square and shrink it to 256px.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        badgeError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Team name") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = league, onValueChange = { league = it }, label = { Text("League") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = travelRadius, onValueChange = { travelRadius = it.filter(Char::isDigit) },
            label = { Text("Travel radius (miles)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text("Manager name") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = contactPhone, onValueChange = { contactPhone = it }, label = { Text("Contact phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description, onValueChange = { if (it.length <= 280) description = it },
            label = { Text("Description") },
            supportingText = { Text("${description.length}/280") },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            "Age group and format lock once a fixture has been confirmed - edit them from onboarding otherwise.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (saved) {
            Spacer(Modifier.height(8.dp))
            Text("Saved.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                submitting = true
                errorMessage = null
                saved = false
                scope.launch {
                    val result = teamRepository.update(
                        teamId,
                        UpdateTeamRequest(
                            name = name.trim(),
                            // Only when it changed: the API keeps the current badge for null,
                            // and clears it for "" - and an unchanged badge is a wasted 30KB.
                            badgeUrl = if (badgeChanged) badge else null,
                            league = league.ifBlank { null },
                            managerName = managerName.ifBlank { null }, contactPhone = contactPhone.ifBlank { null },
                            description = description.ifBlank { null },
                            travelRadiusMiles = travelRadius.toIntOrNull()
                        )
                    )
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> { original = result.value; saved = true }
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = dirty && !submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Save")
        }

        TextButton(
            onClick = { confirmArchive = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Archive team") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { if (!archiving) confirmArchive = false },
            title = { Text("Archive this team?") },
            text = {
                Text(
                    "Archived teams stop appearing in search and can't publish availability. This can't be done " +
                        "while there's an upcoming confirmed fixture or an open friendly request."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !archiving,
                    onClick = {
                        archiving = true
                        errorMessage = null
                        scope.launch {
                            val result = teamRepository.archive(teamId)
                            archiving = false
                            when (result) {
                                is ApiResult.Success -> {
                                    confirmArchive = false
                                    if (currentTeamStore.active.value?.teamId == teamId) currentTeamStore.clear()
                                    navigator.resetTo(Route.Home)
                                }
                                is ApiResult.Failure -> {
                                    confirmArchive = false
                                    errorMessage = result.message
                                }
                            }
                        }
                    }
                ) { Text("Archive") }
            },
            dismissButton = {
                TextButton(enabled = !archiving, onClick = { confirmArchive = false }) { Text("Cancel") }
            }
        )
    }
}
