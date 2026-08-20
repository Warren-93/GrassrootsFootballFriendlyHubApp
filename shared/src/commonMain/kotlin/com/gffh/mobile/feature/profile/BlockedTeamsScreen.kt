package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.BlockView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.ReportRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch

/**
 * SCR-PR-11's list/unblock side. Reporting and blocking a team happens from
 * that team's own profile (see ReportBlockScreen); this screen is where a
 * block gets undone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedTeamsScreen(
    reportRepository: ReportRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val team = currentTeamStore.active.collectAsState().value
    var blocks by remember { mutableStateOf<List<BlockView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var unblockingId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(team?.teamId) {
        val t = team ?: return@LaunchedEffect
        loading = true
        when (val result = reportRepository.listBlocks(t.teamId)) {
            is ApiResult.Success -> { blocks = result.value; errorMessage = null }
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Blocked teams") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (blocks.isEmpty()) {
            Text(
                "You haven't blocked any teams.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            Column(Modifier.padding(16.dp)) {
                blocks.forEach { block ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(block.blockedTeamName, style = MaterialTheme.typography.bodyMedium)
                                block.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                            OutlinedButton(
                                enabled = unblockingId != block.id,
                                onClick = {
                                    val t = team ?: return@OutlinedButton
                                    unblockingId = block.id
                                    scope.launch {
                                        val result = reportRepository.unblock(t.teamId, block.id)
                                        unblockingId = null
                                        when (result) {
                                            is ApiResult.Success -> blocks = blocks.filter { it.id != block.id }
                                            is ApiResult.Failure -> errorMessage = result.message
                                        }
                                    }
                                }
                            ) { Text("Unblock") }
                        }
                    }
                }
            }
        }
    }
}
