package com.gffh.mobile.feature.discover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.MatchSummary
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchResultsCache
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max

private const val KM_PER_DEGREE_LAT = 111.0

private data class PlottedMatch(val match: MatchSummary, val dx: Double, val dy: Double)

/**
 * SCR-FF-04 Results map. Purpose: see roughly where match candidates sit
 * relative to your own team. No tile-based map library is wired up on
 * mobile (web uses Leaflet), so this draws a simple relative-position plot
 * instead - dots placed by real approximate distance/bearing, not a fake
 * illustration, just not a street map. Opponent positions come from
 * geocoding their general area, never their exact ground (see
 * GeocodeRepository).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsMapScreen(
    teamRepository: TeamRepository,
    geocodeRepository: GeocodeRepository,
    currentTeamStore: CurrentTeamStore,
    resultsCache: SearchResultsCache,
    navigator: Navigator
) {
    val activeTeam by currentTeamStore.active.collectAsState()
    val response by resultsCache.lastResponse.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var plotted by remember { mutableStateOf<List<PlottedMatch>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeTeam?.teamId, response) {
        val teamId = activeTeam?.teamId
        val results = response?.results
        if (teamId == null || results == null) {
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            val homeResult = teamRepository.get(teamId)
            val home = (homeResult as? ApiResult.Success)?.value
            if (home == null) {
                loading = false
                return@launch
            }
            val cosLat = cos(Math.toRadians(home.latitude))
            val kmPerDegreeLon = KM_PER_DEGREE_LAT * cosLat

            plotted = results.mapNotNull { match ->
                val outcode = match.team.generalArea ?: return@mapNotNull null
                val point = geocodeRepository.geocodeOutcode(outcode) ?: return@mapNotNull null
                val dx = (point.longitude - home.longitude) * kmPerDegreeLon
                val dy = (point.latitude - home.latitude) * KM_PER_DEGREE_LAT
                PlottedMatch(match, dx, dy)
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Results map") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }

        Text(
            "Opponent positions are approximate - placed at their general area, not their exact ground.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        RelativeMapCanvas(plotted, modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 16.dp))

        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(plotted) { p ->
                Card(
                    onClick = { navigator.push(Route.OpponentProfile(p.match.team.id)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.match.team.name, style = MaterialTheme.typography.titleSmall)
                            Text("${p.match.team.generalArea ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${p.match.milesApart} mi", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RelativeMapCanvas(plotted: List<PlottedMatch>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error

    Canvas(modifier) {
        val maxKm = max(1.0, plotted.maxOfOrNull { max(kotlin.math.abs(it.dx), kotlin.math.abs(it.dy)) } ?: 1.0) * 1.2
        val radius = kotlin.math.min(size.width, size.height) / 2f - 24f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Distance rings.
        listOf(0.33f, 0.66f, 1f).forEach { fraction ->
            drawCircle(color = outline.copy(alpha = 0.3f), radius = radius * fraction, center = center, style = Stroke(width = 1f))
        }

        // Home marker.
        drawCircle(color = primary, radius = 8f, center = center)
        drawText(textMeasurer, "You", topLeft = center + Offset(10f, -8f), style = TextStyle(fontSize = 11.sp, color = onSurface))

        plotted.forEach { p ->
            val scale = (radius / maxKm).toFloat()
            val point = center + Offset((p.dx * scale).toFloat(), -(p.dy * scale).toFloat())
            drawCircle(color = error, radius = 6f, center = point)
            drawText(
                textMeasurer,
                p.match.team.name.take(14),
                topLeft = point + Offset(8f, -8f),
                style = TextStyle(fontSize = 10.sp, color = onSurface)
            )
        }
    }
}
