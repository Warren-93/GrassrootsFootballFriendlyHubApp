package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route

/**
 * SCR-ON-01 Role selection. Purpose: route the new user into the correct
 * creation path in one decision.
 *
 * "Join with a code" is rendered per spec but disabled with an explanatory
 * note: club invitation codes are part of team-member management (SCR-PR-04),
 * which has no backend support yet - see gffh-mobile/README.md.
 */
@Composable
fun RoleSelectionScreen(navigator: Navigator) {
    var showJoinField by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("How are you setting up?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OptionCard(
            title = "Create a club",
            subtitle = "For a club official setting up several squads",
            onClick = { navigator.push(Route.CreateClub) }
        )
        Spacer(Modifier.height(12.dp))
        OptionCard(
            title = "Create a team",
            subtitle = "For a single-team manager",
            onClick = { navigator.push(Route.CreateTeam()) }
        )
        Spacer(Modifier.height(12.dp))
        OptionCard(
            title = "Join with a code",
            subtitle = "For an official invited by an existing club",
            onClick = { showJoinField = !showJoinField }
        )

        if (showJoinField) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it.uppercase().take(8) },
                label = { Text("8-character code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                supportingText = { Text("Club invitations aren't available yet in this build.") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = { navigator.resetTo(Route.Home) }, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
        }
        Text(
            "You'll have read-only access until you create or join a team.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
