package com.gffh.mobile.feature.home

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
import com.gffh.mobile.model.NotificationView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.NotificationRepository
import kotlinx.coroutines.launch

/**
 * SCR-HM-02 Notification centre. Purpose: one place to see everything that
 * happened across requests, fixtures, verification and messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(notificationRepository: NotificationRepository, navigator: Navigator) {
    var notifications by remember { mutableStateOf<List<NotificationView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            when (val result = notificationRepository.list()) {
                is ApiResult.Success -> notifications = result.value
                is ApiResult.Failure -> {}
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun open(n: NotificationView) {
        scope.launch {
            if (!n.read) notificationRepository.markRead(n.id)
            when {
                n.relatedConversationId != null -> navigator.push(Route.ConversationThread(n.relatedConversationId))
                n.relatedFixtureId != null -> navigator.push(Route.FixtureDetail(n.relatedFixtureId))
                n.relatedRequestId != null -> navigator.push(Route.RequestDetail(n.relatedRequestId))
                else -> load()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                if (notifications.any { !it.read }) {
                    TextButton(onClick = {
                        scope.launch {
                            notificationRepository.markAllRead()
                            load()
                        }
                    }) { Text("Mark all read") }
                }
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(Modifier.padding(16.dp).weight(1f).verticalScroll(rememberScrollState())) {
            if (notifications.isEmpty()) {
                Text("No notifications yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                notifications.forEach { n ->
                    Card(
                        onClick = { open(n) },
                        colors = if (n.read) CardDefaults.cardColors()
                        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                n.title,
                                style = if (n.read) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall
                            )
                            Text(n.body, style = MaterialTheme.typography.bodySmall)
                            Text(n.createdAt.replace("T", " ").take(16), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Remove every notification?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        notificationRepository.clearAll()
                        load()
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
