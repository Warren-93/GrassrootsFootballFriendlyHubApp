package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.MemberRepository
import com.gffh.mobile.session.ActiveTeam
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/**
 * SCR-ON-01 Role selection. Purpose: route the new user into the correct
 * creation path in one decision.
 */
@Composable
fun RoleSelectionScreen(memberRepository: MemberRepository, currentTeamStore: CurrentTeamStore, navigator: Navigator) {
    var showJoinField by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
                onValueChange = { joinCode = it.uppercase().take(6); joinError = null },
                label = { Text("6-character code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                supportingText = { joinError?.let { Text(it) } },
                isError = joinError != null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    joining = true
                    joinError = null
                    scope.launch {
                        when (val result = memberRepository.join(joinCode)) {
                            is ApiResult.Success -> {
                                val team = result.value
                                currentTeamStore.set(ActiveTeam(team.teamId, team.clubId, team.teamName))
                                navigator.resetTo(Route.Home)
                            }
                            is ApiResult.Failure -> {
                                joining = false
                                joinError = result.message
                            }
                        }
                    }
                },
                enabled = !joining && joinCode.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (joining) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Join team")
            }
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
