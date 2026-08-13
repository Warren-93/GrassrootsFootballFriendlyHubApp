package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SearchRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.MatchRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch

/**
 * SCR-HM-03 Suggested matches. Purpose: show every opponent worth considering
 * right now, without the manual filter step SCR-FF-01/02 require - runs a
 * zero-filter search and lands on the existing results screen.
 */
@Composable
fun SuggestedMatchesScreen(
    matchRepository: MatchRepository,
    currentTeamStore: CurrentTeamStore,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeTeam?.teamId) {
        val teamId = activeTeam?.teamId ?: return@LaunchedEffect
        scope.launch {
            when (val result = matchRepository.search(SearchRequest(teamId = teamId, limit = 20))) {
                is ApiResult.Success -> {
                    resultsCache.store(result.value)
                    navigator.replace(Route.Results)
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (activeTeam == null) {
            Text("No team selected.")
        } else {
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ?: Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Finding suggested matches…")
                }
        }
    }
}
