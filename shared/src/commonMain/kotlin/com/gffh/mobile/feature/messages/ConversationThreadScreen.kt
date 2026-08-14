package com.gffh.mobile.feature.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ConversationView
import com.gffh.mobile.model.MessageView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.ConversationRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POLL_MS = 4000L

/**
 * An instant-message-style chat with one other team. Mirrors gffh-web's
 * ConversationThreadPage, including the 4s poll for new messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationThreadScreen(
    conversationRepository: ConversationRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    conversationId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var conversation by remember { mutableStateOf<ConversationView?>(null) }
    var messages by remember { mutableStateOf<List<MessageView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    suspend fun loadMessages() {
        when (val result = conversationRepository.messages(conversationId)) {
            is ApiResult.Success -> messages = result.value
            is ApiResult.Failure -> {}
        }
    }

    LaunchedEffect(conversationId) {
        when (val result = conversationRepository.get(conversationId)) {
            is ApiResult.Success -> conversation = result.value
            is ApiResult.Failure -> errorMessage = result.message
        }
        loadMessages()
        loading = false
        while (true) {
            delay(POLL_MS)
            loadMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        if (draft.isBlank()) return
        sending = true
        errorMessage = null
        scope.launch {
            when (val result = conversationRepository.send(conversationId, draft.trim())) {
                is ApiResult.Success -> {
                    draft = ""
                    loadMessages()
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            sending = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(conversation?.otherTeam?.name ?: "Conversation") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }

        val c = conversation
        if (c == null) {
            Box(Modifier.fillMaxSize().padding(24.dp)) { Text(errorMessage ?: "That conversation could not be found.") }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No messages yet - say hello.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            itemsIndexed(messages) { _, m ->
                val isMine = m.senderTeamId == activeTeam?.teamId
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                m.body,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                m.createdAt.replace("T", " ").take(16),
                                color = (if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    .copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { send() }, enabled = !sending && draft.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
