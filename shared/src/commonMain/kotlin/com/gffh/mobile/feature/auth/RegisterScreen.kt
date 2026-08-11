package com.gffh.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.validation.Validators
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import kotlinx.coroutines.launch

/** SCR-AU-03 Register. Purpose: create a credentialed account with the minimum possible friction. */
@Composable
fun RegisterScreen(authRepository: AuthRepository, navigator: Navigator) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var marketingConsent by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var emailTaken by remember { mutableStateOf(false) }

    val nameValid = Validators.isValidDisplayName(name)
    val emailValid = Validators.isValidEmail(email)
    val passwordValid = Validators.isValidPassword(password)
    val formValid = nameValid && emailValid && passwordValid && termsAccepted

    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Create your account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            isError = name.isNotEmpty() && !nameValid,
            supportingText = { if (name.isNotEmpty() && !nameValid) Text("2-60 characters") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailTaken = false },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = (email.isNotEmpty() && !emailValid) || emailTaken,
            supportingText = {
                when {
                    emailTaken -> Text("An account with this email already exists. Sign in instead.")
                    email.isNotEmpty() && !emailValid -> Text("Enter a valid email address")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Hide" else "Show")
                }
            },
            isError = password.isNotEmpty() && !passwordValid,
            supportingText = { Text("Minimum 10 characters") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
            Text("I accept the terms, privacy and safeguarding policy", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = marketingConsent, onCheckedChange = { marketingConsent = it })
            Text("Send me occasional product updates", style = MaterialTheme.typography.bodySmall)
        }

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = null
                submitting = true
                scope.launch {
                    when (val result = authRepository.register(email.trim(), password, name.trim())) {
                        // Every fresh registration has no team yet - SCR-AU-03 routes to SCR-ON-01, not /home.
                        is ApiResult.Success -> navigator.resetTo(Route.RoleSelection)
                        is ApiResult.Failure -> {
                            submitting = false
                            if (result.code == "EMAIL_ALREADY_REGISTERED") emailTaken = true
                            else errorMessage = result.message
                        }
                    }
                }
            },
            enabled = formValid && !submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Create account")
        }

        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
