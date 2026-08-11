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
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.UpdateTeamRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.TeamRepository
import kotlinx.coroutines.launch

/**
 * SCR-PR-02 Edit team. Purpose: amend team attributes, with guards on the
 * fields matching depends upon.
 *
 * The client doesn't know locally whether age group/format are locked
 * (`TeamView` doesn't expose `firstFixtureConfirmedAt`) - editing is always
 * offered, and a lock is surfaced as the server's own rejection
 * (`MATCHING_FIELDS_LOCKED`) rather than pre-emptively disabled here.
 * "Archive team" is shown per spec but disabled - there is no archive
 * endpoint on the backend yet.
 */
@Composable
fun EditTeamScreen(teamRepository: TeamRepository, navigator: Navigator, teamId: String) {
    var original by remember { mutableStateOf<TeamView?>(null) }
    var name by remember { mutableStateOf("") }
    var league by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var travelRadius by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(teamId) {
        when (val result = teamRepository.get(teamId)) {
            is ApiResult.Success -> {
                val t = result.value
                original = t
                name = t.name
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

    val dirty = name != current.name || league != (current.league ?: "") ||
        managerName != (current.managerName ?: "") || contactPhone != (current.contactPhone ?: "") ||
        description != (current.description ?: "") || travelRadius != current.travelRadiusMiles.toString()

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Edit team", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

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
                            name = name.trim(), league = league.ifBlank { null },
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

        TextButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Archive team (not available yet)")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
