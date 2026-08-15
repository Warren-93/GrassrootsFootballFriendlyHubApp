package com.gffh.mobile.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.VenueView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.repository.VenueRepository
import com.gffh.mobile.ui.components.HeroBand
import com.gffh.mobile.ui.components.StatTile
import com.gffh.mobile.ui.components.StatTileRow
import com.gffh.mobile.ui.components.StatTileTone

/**
 * SCR-PR-01 Team profile. Purpose: show the manager their team exactly as
 * opponents see it, and what is missing.
 *
 * "Preview as opponent" is omitted here - it renders the team through the
 * SCR-FF-05 opponent-profile component, which belongs to the Discover slice.
 */
@Composable
fun TeamProfileScreen(teamRepository: TeamRepository, venueRepository: VenueRepository, navigator: Navigator, teamId: String) {
    var team by remember { mutableStateOf<TeamView?>(null) }
    var venues by remember { mutableStateOf<List<VenueView>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(teamId) {
        when (val result = teamRepository.get(teamId)) {
            is ApiResult.Success -> {
                team = result.value
                (venueRepository.list(result.value.clubId) as? ApiResult.Success)?.let { venues = it.value }
            }
            is ApiResult.Failure -> loadError = result.message
        }
    }

    val current = team
    if (loadError != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(loadError!!) }
        return
    }
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroBand(
            title = current.name,
            eyebrow = "Team profile",
            subtitle = "Exactly what opponents see when they look you up.",
            compact = true,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            current.clubName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable { navigator.push(Route.ClubProfile(current.clubId)) }
        )

        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            StatTileRow {
                StatTile(
                    label = "Profile completeness",
                    value = "${current.completenessPercent}%",
                    sub = if (current.completenessPercent < 80) "Must reach 80% to invite" else "Ready to invite",
                    tone = if (current.completenessPercent < 80) StatTileTone.Amber else StatTileTone.Default,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.padding(horizontal = 24.dp)) {
        SectionCard("Identity") {
            InfoRow("Age group", current.ageGroup)
            InfoRow("Gender", current.gender)
            InfoRow("Format", current.format)
            InfoRow("Ability", current.abilityLevel)
            current.league?.let { InfoRow("League", it) }
        }
        Spacer(Modifier.height(12.dp))
        SectionCard("Location") {
            InfoRow("Postcode", current.postcode)
            InfoRow("Travel radius", "${current.travelRadiusMiles} miles")
            InfoRow("Home/Away", current.homeAwayPreference)
        }
        Spacer(Modifier.height(12.dp))
        SectionCard("Contact") {
            current.managerName?.let { InfoRow("Manager", it) }
            current.contactPhone?.let { InfoRow("Phone", it) }
        }
        if (!current.description.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            SectionCard("Description") { Text(current.description, style = MaterialTheme.typography.bodyMedium) }
        }

        Spacer(Modifier.height(12.dp))
        Card(onClick = { navigator.push(Route.EditTeam(teamId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Edit team", modifier = Modifier.padding(16.dp))
        }

        Spacer(Modifier.height(12.dp))
        Card(onClick = { navigator.push(Route.Verification(teamId)) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Verification", style = MaterialTheme.typography.titleSmall)
                Text(current.verification, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(onClick = { navigator.push(Route.VenuesList(current.clubId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Venues (${venues.size})", modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(12.dp))
        Card(onClick = { navigator.push(Route.Members(teamId)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Members", modifier = Modifier.padding(16.dp))
        }

        Spacer(Modifier.height(12.dp))
        Card(onClick = { navigator.push(Route.Settings) }, modifier = Modifier.fillMaxWidth()) {
            Text("Settings", modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
