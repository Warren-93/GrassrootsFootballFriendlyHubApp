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
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.VenueRepository
import kotlinx.coroutines.launch

/** SCR-PR-05 Venues list. Purpose: see and manage every pitch the club can host at. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuesListScreen(venueRepository: VenueRepository, navigator: Navigator, clubId: String) {
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
        TopAppBar(
            title = { Text("Venues") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        Column(Modifier.padding(16.dp).weight(1f)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (venues.isEmpty()) {
                Text("No venues yet.", style = MaterialTheme.typography.bodyMedium)
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

        Button(
            onClick = { navigator.push(Route.EditVenue(null, clubId)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) { Text("Add venue") }
    }
}
