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
import kotlinx.coroutines.launch

/**
 * SCR-ON-02 Create club. Purpose: capture club identity so teams can be
 * nested beneath it.
 *
 * No geocoding service is configured (see gffh-mobile/README.md), so
 * latitude/longitude are entered directly rather than resolved from the
 * postcode on blur, as the spec assumes.
 */
@Composable
fun CreateClubScreen(clubRepository: ClubRepository, authRepository: AuthRepository, navigator: Navigator) {
    var name by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf(authRepository.session.value?.email ?: "") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val lonValue = longitude.toDoubleOrNull()
    val latValue = latitude.toDoubleOrNull()
    val formValid = name.trim().length in 3..80 && postcode.isNotBlank() &&
        lonValue != null && lonValue in -180.0..180.0 &&
        latValue != null && latValue in -90.0..90.0

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Create your club", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Club name") },
            supportingText = { Text("3-80 characters") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = postcode, onValueChange = { postcode = it },
            label = { Text("Home area postcode") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = latitude, onValueChange = { latitude = it },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = longitude, onValueChange = { longitude = it },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
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
                            longitude = lonValue!!, latitude = latValue!!,
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
