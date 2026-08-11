package com.gffh.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.validation.Validators
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SCR-AU-05 Forgot password. Purpose: trigger a reset without confirming
 * whether an account exists - the response is identical either way (see
 * AuthService.requestPasswordReset on the backend), and this screen must not
 * distinguish it either.
 *
 * The emailed reset link's destination (setting a new password with the
 * token it carries) is not one of the 38 screens in Volume 2 - it is
 * presumed to be a web or deep-link surface outside this slice, so it is not
 * built here; [com.gffh.mobile.repository.AuthRepository.confirmPasswordReset]
 * exists for whenever that surface is defined.
 */
@Composable
fun ForgotPasswordScreen(authRepository: AuthRepository, navigator: Navigator, prefilledEmail: String) {
    var email by remember { mutableStateOf(prefilledEmail) }
    var sent by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var resendSecondsLeft by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun send() {
        submitting = true
        scope.launch {
            authRepository.requestPasswordReset(email.trim())
            submitting = false
            sent = true
            resendSecondsLeft = 60
            while (resendSecondsLeft > 0) {
                delay(1000)
                resendSecondsLeft -= 1
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        if (!sent) {
            Text("Reset your password", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = ::send,
                enabled = Validators.isValidEmail(email) && !submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Send reset link")
            }
            TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "If an account exists for $email, a reset link has been sent.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = ::send, enabled = resendSecondsLeft == 0) {
                    Text(if (resendSecondsLeft > 0) "Resend in ${resendSecondsLeft}s" else "Resend")
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navigator.pop() }) { Text("Back to sign in") }
            }
        }
    }
}
