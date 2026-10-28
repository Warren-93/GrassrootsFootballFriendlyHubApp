package com.gffh.mobile.feature.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.image.GifImage
import com.gffh.mobile.core.lifecycle.AppVisibility
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ConversationView
import com.gffh.mobile.model.GifView
import com.gffh.mobile.model.MessageView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.ConfigRepository
import com.gffh.mobile.repository.ConversationRepository
import com.gffh.mobile.repository.GifRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.ui.components.CrestAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * An instant-message-style chat with one other team. Mirrors gffh-web's
 * ConversationThreadPage, including the poll for new messages (see
 * ChatPolling.kt: only what's new, slower once quiet, none in the background), the emoji
 * menu and the GIF picker (shown when the API has GIFs switched on). Bad
 * language is masked by the API before messages arrive here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationThreadScreen(
    conversationRepository: ConversationRepository,
    gifRepository: GifRepository,
    configRepository: ConfigRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator,
    conversationId: String
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var conversation by remember { mutableStateOf<ConversationView?>(null) }
    var messages by remember { mutableStateOf<List<MessageView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var draft by remember { mutableStateOf(TextFieldValue("")) }
    var showEmojis by remember { mutableStateOf(false) }
    var showGifs by remember { mutableStateOf(false) }
    val config by configRepository.config.collectAsState()
    val focusManager = LocalFocusManager.current
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // When the chat last saw a new message, to slow polling once it's quiet.
    var lastActivity by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }

    fun addMessages(incoming: List<MessageView>) {
        val merged = mergeMessages(messages, incoming)
        if (merged === messages) return
        messages = merged
        lastActivity = TimeSource.Monotonic.markNow()
    }

    /** Fetches only messages since the newest one held (all of them the first time). */
    suspend fun loadMessages() {
        when (val result = conversationRepository.messages(conversationId, since = pollSince(messages))) {
            is ApiResult.Success -> addMessages(result.value.filter { it.conversationId == conversationId })
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
        configRepository.get()
        while (true) {
            delay(nextPollDelayMs(lastActivity.elapsedNow().inWholeMilliseconds))
            if (!AppVisibility.inForeground.value) {
                // Nothing is fetched in the background; back on screen, check straight away.
                AppVisibility.inForeground.first { it }
                lastActivity = TimeSource.Monotonic.markNow()
            }
            loadMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    /** Sends what's typed, or [gif] on its own (whatever is typed then stays in the box). */
    fun send(gif: GifView? = null) {
        val body = if (gif != null) "" else draft.text.trim()
        if (body.isEmpty() && gif == null) return
        sending = true
        errorMessage = null
        scope.launch {
            when (val result = conversationRepository.send(conversationId, body, gif?.id)) {
                is ApiResult.Success -> {
                    if (gif == null) draft = TextFieldValue("")
                    addMessages(listOf(result.value))
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            sending = false
        }
    }

    /** Puts the emoji where the cursor is (or over the selection), with the cursor after it. */
    fun insertEmoji(emoji: String) {
        val start = draft.selection.min
        val end = draft.selection.max
        val text = draft.text.substring(0, start) + emoji + draft.text.substring(end)
        draft = TextFieldValue(text, TextRange(start + emoji.length))
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                val otherTeam = conversation?.otherTeam
                if (otherTeam != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CrestAvatar(otherTeam.name, size = 28.dp, badgeUrl = otherTeam.badgeUrl)
                        Spacer(Modifier.width(8.dp))
                        Text(otherTeam.name)
                    }
                } else {
                    Text("Conversation")
                }
            },
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

        c.relatedFixtureId?.let { fixtureId ->
            AssistChip(
                onClick = { navigator.push(Route.FixtureDetail(fixtureId)) },
                label = { Text("About a fixture - view it") },
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
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
                            m.gif?.let { gif ->
                                GifImage(
                                    url = gif.url,
                                    contentDescription = gif.title.ifBlank { "GIF" },
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(gifAspectRatio(gif))
                                        .clip(MaterialTheme.shapes.small)
                                )
                                if (m.body.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            }
                            if (m.body.isNotEmpty()) {
                                Text(
                                    m.body,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
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
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Swaps between the emoji menu and the keyboard, like other chat apps.
            IconButton(onClick = {
                if (!showEmojis) focusManager.clearFocus()
                showEmojis = !showEmojis
            }) {
                Icon(
                    if (showEmojis) Icons.Filled.Keyboard else Icons.Filled.EmojiEmotions,
                    contentDescription = if (showEmojis) "Keyboard" else "Emoji"
                )
            }
            if (config?.gifsEnabled == true) {
                IconButton(onClick = { showGifs = true }, enabled = !sending) {
                    Icon(Icons.Filled.Gif, contentDescription = "GIF")
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                // Typing again brings the keyboard back in place of the emoji menu.
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) showEmojis = false }
            )
            IconButton(onClick = { send() }, enabled = !sending && draft.text.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (showEmojis) {
            EmojiPanel(onPick = { insertEmoji(it) })
        }
    }

    if (showGifs) {
        GifPickerSheet(
            gifRepository = gifRepository,
            onPick = { gif ->
                showGifs = false
                send(gif)
            },
            onDismiss = { showGifs = false }
        )
    }
}
