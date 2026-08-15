package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.session.CurrentTeamStore

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

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Account", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
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
            Text("Team & club", style = MaterialTheme.typography.titleSmall)
            SettingsRow("Team profile") { navigator.push(Route.TeamProfile(team.teamId)) }
            SettingsRow("Members") { navigator.push(Route.Members(team.teamId)) }
            SettingsRow("Club") { navigator.push(Route.ClubProfile(team.clubId)) }
            SettingsRow("Venues") { navigator.push(Route.VenuesList(team.clubId)) }
        }

        Spacer(Modifier.height(20.dp))
        Text("General", style = MaterialTheme.typography.titleSmall)
        SettingsRow("Notifications") { navigator.push(Route.NotificationPreferences) }
        SettingsRow("Privacy and data") { navigator.push(Route.Privacy) }
        SettingsRow("Help and support") { navigator.push(Route.Help) }

        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { confirmSignOut = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign out") }
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
