package com.gffh.mobile.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.*
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.GeoPoint
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.repository.TeamRepository
import com.gffh.mobile.ui.components.PostcodeLocationField
import kotlinx.coroutines.launch

private fun gendersFor(ageGroup: AgeGroup): List<Gender> =
    if (ageGroup == AgeGroup.ADULT || ageGroup == AgeGroup.VETERANS) listOf(Gender.MEN, Gender.WOMEN, Gender.MIXED)
    else listOf(Gender.BOYS, Gender.GIRLS, Gender.MIXED)

private fun suggestedFormat(ageGroup: AgeGroup): Format = when (ageGroup) {
    AgeGroup.U7, AgeGroup.U8 -> Format.FIVE_A_SIDE
    AgeGroup.U9, AgeGroup.U10 -> Format.SEVEN_A_SIDE
    AgeGroup.U11, AgeGroup.U12 -> Format.NINE_A_SIDE
    else -> Format.ELEVEN_A_SIDE
}

private fun formatLabel(f: Format) = when (f) {
    Format.FIVE_A_SIDE -> "5v5"; Format.SEVEN_A_SIDE -> "7v7"
    Format.NINE_A_SIDE -> "9v9"; Format.ELEVEN_A_SIDE -> "11v11"
}

/**
 * SCR-ON-03 Create team (4-step stepper). Purpose: capture the full team
 * profile the matching engine depends on, split so no single screen is
 * intimidating.
 *
 * Simplifications from spec: no client-side draft persistence between steps
 * (an interrupted session restarts the form rather than resuming mid-step),
 * no live "teams in range" count on step 3 (needs a preview-count endpoint
 * that doesn't exist yet), and no badge image picker (no upload backend).
 *
 * Step 0 has no wizard step behind it, so its Back button pops the screen
 * instead of decrementing step - otherwise it would be a dead end.
 */
@Composable
fun CreateTeamScreen(
    teamRepository: TeamRepository,
    geocodeRepository: GeocodeRepository,
    navigator: Navigator,
    clubId: String?
) {
    var step by remember { mutableStateOf(0) }

    var teamName by remember { mutableStateOf("") }
    var ageGroup by remember { mutableStateOf(AgeGroup.U12) }
    var gender by remember { mutableStateOf(Gender.MIXED) }

    var format by remember { mutableStateOf(suggestedFormat(AgeGroup.U12)) }
    var formatManuallySet by remember { mutableStateOf(false) }
    var ability by remember { mutableStateOf(AbilityLevel.INTERMEDIATE) }
    var league by remember { mutableStateOf("") }

    var postcode by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf<GeoPoint?>(null) }
    var travelRadius by remember { mutableStateOf(15) }
    var homeAway by remember { mutableStateOf(HomeAwayPreference.EITHER) }

    var managerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val step1Valid = teamName.trim().length in 3..80
    val step3Valid = postcode.isNotBlank() && coordinates != null
    val step4Valid = managerName.trim().length in 2..60

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Step ${step + 1} of 4", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    Text("Team identity", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = teamName, onValueChange = { teamName = it },
                        label = { Text("Team name") },
                        supportingText = { Text("3-80 characters") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Age group", style = MaterialTheme.typography.labelLarge)
                    FlowChips(AgeGroup.entries.toList(), ageGroup, { it.name }) {
                        ageGroup = it
                        if (!formatManuallySet) format = suggestedFormat(it)
                        if (gender !in gendersFor(it)) gender = Gender.MIXED
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Gender", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceRow(gendersFor(ageGroup), gender, { it.name }) { gender = it }
                }
                1 -> {
                    Text("Format and ability", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceRow(Format.entries.toList(), format, ::formatLabel) {
                        format = it; formatManuallySet = true
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Ability level", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceRow(AbilityLevel.entries.toList(), ability, { it.name }) { ability = it }
                    Text(
                        when (ability) {
                            AbilityLevel.DEVELOPMENT -> "New to competitive football, focused on participation."
                            AbilityLevel.INTERMEDIATE -> "Regular league football, solid fundamentals."
                            AbilityLevel.COMPETITIVE -> "Strong, established side playing at a high standard."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = league, onValueChange = { if (it.length <= 80) league = it },
                        label = { Text("League (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                2 -> {
                    Text("Location and travel", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    PostcodeLocationField(
                        geocodeRepository = geocodeRepository,
                        postcode = postcode, onPostcodeChange = { postcode = it },
                        coordinates = coordinates, onCoordinatesChange = { coordinates = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Travel radius: $travelRadius miles", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = travelRadius.toFloat(),
                        onValueChange = { travelRadius = (it / 5).toInt() * 5 },
                        valueRange = 5f..50f,
                        steps = 8
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Home / Away preference", style = MaterialTheme.typography.labelLarge)
                    SingleChoiceRow(HomeAwayPreference.entries.toList(), homeAway, { it.name }) { homeAway = it }
                }
                3 -> {
                    Text("Contact details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = managerName, onValueChange = { managerName = it },
                        label = { Text("Manager name") },
                        supportingText = { Text("2-60 characters") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contactPhone, onValueChange = { contactPhone = it },
                        label = { Text("Contact phone (optional)") },
                        supportingText = { Text("Shared only after a fixture is confirmed") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 280) description = it },
                        label = { Text("Description (optional)") },
                        supportingText = { Text("${description.length}/280") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row {
            OutlinedButton(
                onClick = { if (step > 0) step -= 1 else navigator.pop() },
                modifier = Modifier.weight(1f)
            ) { Text("Back") }
            Spacer(Modifier.width(12.dp))
            val currentStepValid = when (step) { 0 -> step1Valid; 2 -> step3Valid; 3 -> step4Valid; else -> true }
            if (step < 3) {
                Button(onClick = { step += 1 }, enabled = currentStepValid, modifier = Modifier.weight(1f)) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = {
                        submitting = true
                        errorMessage = null
                        scope.launch {
                            val result = teamRepository.create(
                                CreateTeamRequest(
                                    clubId = clubId, clubName = if (clubId == null) teamName.trim() else null,
                                    name = teamName.trim(), ageGroup = ageGroup.name, gender = gender.name,
                                    format = format.name, abilityLevel = ability.name,
                                    league = league.ifBlank { null }, postcode = postcode.trim(),
                                    longitude = coordinates!!.longitude, latitude = coordinates!!.latitude,
                                    travelRadiusMiles = travelRadius, homeAwayPreference = homeAway.name,
                                    managerName = managerName.trim(), contactPhone = contactPhone.ifBlank { null },
                                    description = description.ifBlank { null }
                                )
                            )
                            submitting = false
                            when (result) {
                                is ApiResult.Success -> navigator.push(
                                    Route.AddFirstVenue(result.value.clubId, result.value.id)
                                )
                                is ApiResult.Failure -> errorMessage = result.message
                            }
                        }
                    },
                    enabled = currentStepValid && !submitting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Create team")
                }
            }
        }
    }
}

@Composable
private fun <T> SingleChoiceRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun <T> FlowChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}