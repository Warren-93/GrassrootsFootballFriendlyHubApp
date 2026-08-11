package com.gffh.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.platform.openMailApp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SCR-AU-06 Email verification. Purpose: prompt verification without
 * blocking use of the app. True foreground-resume polling needs a
 * platform lifecycle hook this slice doesn't wire up yet; "Check now"
 * covers the same need explicitly instead.
 */
@Composable
fun EmailVerificationScreen(authRepository: AuthRepository, navigator: Navigator) {
    val session by authRepository.session.collectAsState()
    var resendSecondsLeft by remember { mutableStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Verify your email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            "We sent a verification link to ${session?.email ?: "your email address"}. " +
                "Verifying unlocks sending invitations and messages, and confirms you're a reachable club official.",
            style = MaterialTheme.typography.bodyLarge
        )

        statusMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = { openMailApp() }, modifier = Modifier.fillMaxWidth()) {
            Text("Open mail app")
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    authRepository.resendVerification()
                    resendSecondsLeft = 60
                    while (resendSecondsLeft > 0) {
                        delay(1000)
                        resendSecondsLeft -= 1
                    }
                }
            },
            enabled = resendSecondsLeft == 0,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { Text(if (resendSecondsLeft > 0) "Resend in ${resendSecondsLeft}s" else "Resend") }

        TextButton(
            onClick = {
                checking = true
                scope.launch {
                    when (val result = authRepository.me()) {
                        is ApiResult.Success ->
                            statusMessage = if (result.value.emailVerified) "Verified!" else "Not verified yet."
                        is ApiResult.Failure -> statusMessage = result.message
                    }
                    checking = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (checking) "Checking..." else "Check now") }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = { navigator.resetTo(Route.Home) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue to app")
        }
    }
}
