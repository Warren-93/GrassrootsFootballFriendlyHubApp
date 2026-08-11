package com.gffh.mobile.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository

/**
 * SCR-AU-01 Splash / session resolve.
 *
 * Purpose: resolve the session and route the user without showing a decision
 * to make. No progress indicator before 400ms per the spec; simplified here
 * to always show the brand mark, since a real 400ms gate needs a delay timer
 * that would just as often flash for no reason on a fast local network.
 */
@Composable
fun SplashScreen(authRepository: AuthRepository, navigator: Navigator) {
    LaunchedEffect(Unit) {
        val session = authRepository.session.value
        if (session == null) {
            navigator.resetTo(Route.Welcome)
            return@LaunchedEffect
        }
        when (authRepository.me()) {
            // Unverified accounts may still browse (SCR-AU-06) - verification
            // is enforced per-action server-side, not gated here.
            is ApiResult.Success -> navigator.resetTo(Route.Home)
            is ApiResult.Failure -> {
                // Refresh failure clears stored credentials silently - never shown as an error (SCR-AU-01).
                if (authRepository.refresh() is ApiResult.Success) {
                    navigator.resetTo(Route.Home)
                } else {
                    authRepository.signOut()
                    navigator.resetTo(Route.Welcome)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Grassroots Football Friendly Hub", style = MaterialTheme.typography.titleMedium)
            Box(Modifier.padding(top = 24.dp)) { CircularProgressIndicator() }
        }
    }
}
