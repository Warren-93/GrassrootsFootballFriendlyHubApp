package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.theme.eyebrowLabel
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.HeroBand

/**
 * SCR-PR-08 Settings. Purpose: account, notification, privacy and support
 * controls in one predictable place.
 *
 * Units and Appearance aren't shown: neither has anywhere to persist to yet,
 * and a toggle that resets on relaunch would be worse than no toggle.
 */
@Composable
fun SettingsScreen(authRepository: AuthRepository, currentTeamStore: CurrentTeamStore, navigator: Navigator) {
    val session by authRepository.session.collectAsState()
    val activeTeam by currentTeamStore.active.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroBand(
            title = session?.displayName ?: "Settings",
            eyebrow = "Account",
            subtitle = activeTeam?.let { "Managing ${it.teamName}" } ?: "Manage your account and preferences.",
            compact = true,
            modifier = Modifier.padding(16.dp)
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
        SectionLabel("Account")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(session?.displayName ?: "", style = MaterialTheme.typography.bodyMedium)
                Text(session?.email ?: "", style = MaterialTheme.typography.bodySmall)
                if (session?.emailVerified == false) {
                    Spacer(Modifier.height(8.dp))
                    AssistChip(onClick = { navigator.push(Route.EmailVerification) }, label = { Text("Verify your email") })
                }
            }
        }

        if (activeTeam != null) {
            val team = activeTeam!!
            Spacer(Modifier.height(20.dp))
            SectionLabel("Team & club")
            Card(
                onClick = { navigator.push(Route.TeamProfile(team.teamId)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrestAvatar(team.teamName, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(team.teamName, style = MaterialTheme.typography.titleSmall)
                        Text("Team profile", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            SettingsRow("Members") { navigator.push(Route.Members(team.teamId)) }
            SettingsRow("Club") { navigator.push(Route.ClubProfile(team.clubId)) }
            SettingsRow("Venues") { navigator.push(Route.VenuesList(team.clubId)) }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("General")
        SettingsRow("Notifications") { navigator.push(Route.NotificationPreferences) }
        SettingsRow("Privacy and data") { navigator.push(Route.Privacy) }
        SettingsRow("Blocked teams") { navigator.push(Route.BlockedTeams) }
        SettingsRow("Help and support") { navigator.push(Route.Help) }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Danger zone")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Signing out ends your session on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(
                    onClick = { confirmSignOut = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Sign out") }
            }
        }
        Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to publish availability or respond to requests.") },
            confirmButton = {
                TextButton(onClick = {
                    authRepository.signOut()
                    currentTeamStore.clear()
                    navigator.resetTo(Route.Welcome)
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(label, modifier = Modifier.padding(16.dp))
    }
}

/** Small bold uppercase section header, matching the eyebrow treatment used above [HeroBand] titles. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = eyebrowLabel,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
