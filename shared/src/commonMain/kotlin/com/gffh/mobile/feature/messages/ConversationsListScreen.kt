package com.gffh.mobile.feature.messages

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
import com.gffh.mobile.model.ConversationView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.ConversationRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.ui.components.CrestAvatar
import kotlinx.coroutines.launch

/**
 * The inbox - every team the active team is chatting with, most recently
 * active first. Mirrors gffh-web's ConversationsListPage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsListScreen(
    conversationRepository: ConversationRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var conversations by remember { mutableStateOf<List<ConversationView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeTeam?.teamId) {
        val teamId = activeTeam?.teamId
        if (teamId == null) {
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            when (val result = conversationRepository.list(teamId)) {
                is ApiResult.Success -> conversations = result.value
                is ApiResult.Failure -> {}
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Messages") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }

        if (activeTeam == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text("No active team. Create or select a team first.") }
            return
        }

        Column(Modifier.padding(16.dp).weight(1f).verticalScroll(rememberScrollState())) {
            if (conversations.isEmpty()) {
                Text(
                    "No conversations yet. Message a team from their profile once they've published availability.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                conversations.forEach { c ->
                    val mine = c.lastMessageSenderTeamId == activeTeam?.teamId
                    Card(
                        onClick = { navigator.push(Route.ConversationThread(c.id)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CrestAvatar(c.otherTeam.name)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.otherTeam.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    c.lastMessageBody?.let { if (mine) "You: $it" else it } ?: "No messages yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            c.lastMessageAt?.let {
                                Text(it.take(10), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
