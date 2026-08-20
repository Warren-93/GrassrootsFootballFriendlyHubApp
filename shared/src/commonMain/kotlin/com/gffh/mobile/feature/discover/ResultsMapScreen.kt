package com.gffh.mobile.feature.discover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.MatchSummary
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.SearchResultsCache
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.launch

private data class PlottedMatch(val match: MatchSummary, val latitude: Double, val longitude: Double)

/**
 * SCR-FF-04 Results map. Purpose: see where match candidates sit relative to
 * your own team on a real map. Renders Leaflet/OpenStreetMap inside a WebView
 * - the same tile source gffh-web's own map uses - rather than a native map
 * SDK, so neither platform needs an API key or billing account wired up.
 * Opponent positions come from geocoding their general area, never their
 * exact ground (see GeocodeRepository).
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
    var homeLat by remember { mutableStateOf<Double?>(null) }
    var homeLon by remember { mutableStateOf<Double?>(null) }
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
            homeLat = home.latitude
            homeLon = home.longitude

            plotted = results.mapNotNull { match ->
                val outcode = match.team.generalArea ?: return@mapNotNull null
                val point = geocodeRepository.geocodeOutcode(outcode) ?: return@mapNotNull null
                PlottedMatch(match, point.latitude, point.longitude)
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

        val lat = homeLat
        val lon = homeLon
        if (lat != null && lon != null) {
            LeafletMap(lat, lon, plotted, modifier = Modifier.fillMaxWidth().height(280.dp))
        }

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
private fun LeafletMap(homeLat: Double, homeLon: Double, plotted: List<PlottedMatch>, modifier: Modifier = Modifier) {
    val html = remember(homeLat, homeLon, plotted) { buildMapHtml(homeLat, homeLon, plotted) }
    val state = rememberWebViewStateWithHTMLData(data = html)
    WebView(state = state, modifier = modifier)
}

private fun buildMapHtml(homeLat: Double, homeLon: Double, plotted: List<PlottedMatch>): String {
    val markers = plotted.joinToString("\n") { p ->
        val name = p.match.team.name.replace("'", "\\'").replace("\"", "&quot;")
        """L.circleMarker([${p.latitude}, ${p.longitude}], {radius: 7, color: '#D64545', fillColor: '#D64545', fillOpacity: 0.9})
            .addTo(map).bindPopup('$name &middot; ${p.match.milesApart} mi');"""
    }
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>
        </head>
        <body>
            <div id="map"></div>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script>
                var map = L.map('map').setView([$homeLat, $homeLon], 10);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '&copy; OpenStreetMap contributors'
                }).addTo(map);
                L.circleMarker([$homeLat, $homeLon], {radius: 8, color: '#1976d2', fillColor: '#1976d2', fillOpacity: 1})
                    .addTo(map).bindPopup('Your team');
                $markers
            </script>
        </body>
        </html>
    """.trimIndent()
}
