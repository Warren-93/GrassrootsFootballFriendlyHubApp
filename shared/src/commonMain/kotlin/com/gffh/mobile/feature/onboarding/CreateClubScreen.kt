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
import com.gffh.mobile.model.CreateClubRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.repository.ClubRepository
import com.gffh.mobile.repository.GeoPoint
import com.gffh.mobile.repository.GeocodeRepository
import com.gffh.mobile.theme.displayHeadlineSmall
import com.gffh.mobile.theme.eyebrowLabel
import com.gffh.mobile.ui.components.PostcodeLocationField
import kotlinx.coroutines.launch

/**
 * SCR-ON-02 Create club. Purpose: capture club identity so teams can be
 * nested beneath it.
 *
 * Latitude/longitude are resolved from the postcode via postcodes.io rather
 * than entered directly - see PostcodeLocationField.
 */
@Composable
fun CreateClubScreen(
    clubRepository: ClubRepository,
    geocodeRepository: GeocodeRepository,
    authRepository: AuthRepository,
    navigator: Navigator
) {
    var name by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf<GeoPoint?>(null) }
    var website by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf(authRepository.session.value?.email ?: "") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val formValid = name.trim().length in 3..80 && postcode.isNotBlank() && coordinates != null

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("STEP 1 OF 3 · ROLE & CLUB", style = eyebrowLabel, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text("Create your club".uppercase(), style = displayHeadlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Club name") },
            supportingText = { Text("3-80 characters") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        PostcodeLocationField(
            geocodeRepository = geocodeRepository,
            postcode = postcode, onPostcodeChange = { postcode = it },
            coordinates = coordinates, onCoordinatesChange = { coordinates = it },
            label = "Home area postcode"
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = website, onValueChange = { website = it },
            label = { Text("Website (optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = contactEmail, onValueChange = { contactEmail = it },
            label = { Text("Contact email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                submitting = true
                errorMessage = null
                scope.launch {
                    val result = clubRepository.create(
                        CreateClubRequest(
                            name = name.trim(), postcode = postcode.trim(),
                            longitude = coordinates!!.longitude, latitude = coordinates!!.latitude,
                            website = website.ifBlank { null }, contactEmail = contactEmail.ifBlank { null }
                        )
                    )
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> navigator.push(Route.CreateTeam(result.value.id))
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = formValid && !submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Continue")
        }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}