package com.gffh.mobile.feature.profile

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
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.session.canManageClubOf
import com.gffh.mobile.ui.components.HeroBand
import com.gffh.mobile.ui.components.VenueCard
import kotlinx.coroutines.launch

/**
 * SCR-PR-05 Venues list. Purpose: see and manage every pitch the club can host at.
 *
 * Only the club's admin can add, edit, make default or delete its venues.
 * Anyone else (a team manager) sees each venue's details read-only instead,
 * without the actions the API would refuse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuesListScreen(venueRepository: VenueRepository, currentTeamStore: CurrentTeamStore, navigator: Navigator, clubId: String) {
    val activeTeam by currentTeamStore.active.collectAsState()
    // Unknown (an older API) counts as yes, as before the API said.
    val canManageVenues = activeTeam.canManageClubOf(clubId) != false
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        scope.launch {
            when (val result = venueRepository.list(clubId)) {
                is ApiResult.Success -> venues = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
            loading = false
        }
    }

    LaunchedEffect(clubId) { load() }

    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = { navigator.pop() }, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
        }
        HeroBand(
            title = "Venues",
            eyebrow = "Club",
            subtitle = "Every pitch this club can host at.",
            compact = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Column(Modifier.padding(16.dp).weight(1f)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (venues.isEmpty()) {
                // A team manager is only sent to their club admin once the list
                // has loaded and really is empty, not when it failed to load.
                if (canManageVenues || errorMessage == null) {
                    Text(
                        if (canManageVenues) "No venues yet." else "Your club has no venues yet. Ask your club admin to add one.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (!canManageVenues) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    venues.forEach { v ->
                        VenueCard(
                            v.toSummary(),
                            Modifier.padding(bottom = 8.dp),
                            title = if (v.isDefault) "Default venue" else "Venue"
                        )
                    }
                }
            } else {
                venues.forEach { v ->
                    Card(
                        onClick = { navigator.push(Route.EditVenue(v.id, clubId)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(v.name, style = MaterialTheme.typography.titleSmall)
                                Text(v.address, style = MaterialTheme.typography.bodySmall)
                            }
                            if (v.isDefault) {
                                AssistChip(onClick = {}, label = { Text("Default") })
                            } else {
                                TextButton(onClick = {
                                    scope.launch {
                                        venueRepository.setDefault(v.id)
                                        load()
                                    }
                                }) { Text("Make default") }
                            }
                        }
                    }
                }
            }
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (canManageVenues) {
            Button(
                onClick = { navigator.push(Route.EditVenue(null, clubId)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("Add venue") }
        } else if (venues.isNotEmpty()) {
            Text(
                "Only your club admin can add or change venues.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}
