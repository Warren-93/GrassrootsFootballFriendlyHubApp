package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.platform.saveTextFile
import com.gffh.mobile.model.AccountExport
import com.gffh.mobile.model.PrivacyPreferences
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.repository.PrivacyRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val PRETTY_JSON = Json { prettyPrint = true }

/**
 * SCR-PR-10 Privacy and data. Purpose: let a user see and export what the
 * platform holds on them, plus control the active team's search visibility
 * and contact-sharing.
 */
@Composable
fun PrivacyScreen(
    privacyRepository: PrivacyRepository,
    authRepository: AuthRepository,
    teamRepository: TeamRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var data by remember { mutableStateOf<AccountExport?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var prefs by remember { mutableStateOf<PrivacyPreferences?>(null) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        when (val result = privacyRepository.export()) {
            is ApiResult.Success -> data = result.value
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    LaunchedEffect(activeTeam?.teamId) {
        val teamId = activeTeam?.teamId ?: return@LaunchedEffect
        when (val result = teamRepository.getPrivacy(teamId)) {
            is ApiResult.Success -> prefs = result.value
            is ApiResult.Failure -> {}
        }
    }

    fun togglePref(update: (PrivacyPreferences) -> PrivacyPreferences) {
        val teamId = activeTeam?.teamId ?: return
        val current = prefs ?: return
        val next = update(current)
        prefs = next
        scope.launch {
            when (val result = teamRepository.updatePrivacy(teamId, next)) {
                is ApiResult.Success -> prefs = result.value
                is ApiResult.Failure -> {
                    prefs = current
                    errorMessage = result.message
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Privacy and data", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                }

                data?.let { d ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("What we hold on you", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            Text("${d.displayName} · ${d.email}", style = MaterialTheme.typography.bodyMedium)
                            Text("Account created ${d.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            if (d.memberships.isEmpty()) {
                                Text("No team or club roles.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                d.memberships.forEach { m ->
                                    Text(
                                        "${m.role} - ${m.teamName ?: m.clubName ?: m.clubId}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    val json = PRETTY_JSON.encodeToString(d)
                                    downloadMessage = if (saveTextFile("my-gffh-data.json", json)) {
                                        "Saved to Downloads."
                                    } else {
                                        "Couldn't save a file on this device/OS version."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Download my data") }
                            downloadMessage?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                activeTeam?.let { team ->
                    prefs?.let { p ->
                        Spacer(Modifier.height(16.dp))
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Profile visibility", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Let other teams find ${team.teamName} in search",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = p.searchVisible,
                                        onCheckedChange = { checked ->
                                            togglePref { it.copy(searchVisible = checked) }
                                        }
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Share contact details", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Show your manager name and phone on a confirmed fixture",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Switch(
                                        checked = p.shareContactDetails,
                                        onCheckedChange = { checked ->
                                            togglePref { it.copy(shareContactDetails = checked) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Delete account", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Permanently deletes your account and every team/club role you hold. If you're the only " +
                        "admin of a club, you'll need to promote someone else there first.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete my account") }
            }
        }

        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("This can't be undone. Your account, and every team/club role tied to it, will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = true
                    confirmDelete = false
                    scope.launch {
                        when (val result = privacyRepository.deleteAccount()) {
                            is ApiResult.Success -> {
                                authRepository.signOut()
                                currentTeamStore.clear()
                                navigator.resetTo(Route.Welcome)
                            }
                            is ApiResult.Failure -> {
                                deleting = false
                                errorMessage = result.message
                            }
                        }
                    }
                }) { Text("Delete account") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}
