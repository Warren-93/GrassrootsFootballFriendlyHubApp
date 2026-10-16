package com.gffh.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.auth.SocialProvider
import com.gffh.mobile.core.auth.SocialSignIn
import com.gffh.mobile.core.auth.SocialSignInResult
import com.gffh.mobile.core.auth.rememberSocialSignIn
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.repository.ConfigRepository
import com.gffh.mobile.repository.TeamRepository
import kotlinx.coroutines.launch

/**
 * "Continue with Google" on Android, "Continue with Apple" on iOS, under an
 * "or" divider - the same pair gffh-web shows, each only once the API's
 * config says that provider is switched on. Renders nothing while the config
 * loads, if it can't be fetched, or when the provider is off, leaving the
 * screen exactly as it was.
 *
 * The provider's ID token is traded for a session by
 * [AuthRepository.signInWith], the same path as a password sign-in (which
 * also registers the phone for push). A brand-new account then starts
 * onboarding, as after registering - unless a pending invitation, which the
 * API applies at this sign-in, already made it a team member (gffh-web checks
 * the same). Anyone else goes to the dashboard, as after signing in. Backing
 * out of the provider's sheet shows nothing; any other failure goes to
 * [onError], the screen's own error text, which is cleared (null) as each
 * attempt starts.
 *
 * [enabled] is false while the screen's own form is submitting, and
 * [onWorkingChange] lets the screen disable that form while this runs.
 * [lightBackground] picks Apple's black button (for a light background) or
 * its white one (for a dark background).
 */
@Composable
fun SocialSignInSection(
    configRepository: ConfigRepository,
    authRepository: AuthRepository,
    teamRepository: TeamRepository,
    navigator: Navigator,
    enabled: Boolean,
    onWorkingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    lightBackground: Boolean = true,
    modifier: Modifier = Modifier
) {
    val config by configRepository.config.collectAsState()
    LaunchedEffect(Unit) { configRepository.get() }

    val socialSignIn = rememberSocialSignIn(config)
    if (socialSignIn != null) {
        SocialSignInButton(
            socialSignIn, authRepository, teamRepository, navigator, enabled, onWorkingChange, onError,
            lightBackground, modifier
        )
    }
}

@Composable
private fun SocialSignInButton(
    socialSignIn: SocialSignIn,
    authRepository: AuthRepository,
    teamRepository: TeamRepository,
    navigator: Navigator,
    enabled: Boolean,
    onWorkingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    lightBackground: Boolean,
    modifier: Modifier
) {
    val provider = socialSignIn.provider
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun stop(message: String?) {
        working = false
        onWorkingChange(false)
        onError(message)
    }

    val onClick: () -> Unit = {
        onError(null)
        working = true
        onWorkingChange(true)
        scope.launch {
            when (val token = socialSignIn.idToken()) {
                is SocialSignInResult.Cancelled -> stop(null)
                is SocialSignInResult.Failed -> stop(token.message ?: "${provider.label} sign-in didn't work - please try again.")
                is SocialSignInResult.Token -> {
                    when (val result = authRepository.signInWith(provider, token.idToken, token.displayName)) {
                        // A new account usually has no team yet, so it starts where registering
                        // does (SCR-ON-01). Home picks up a team an invitation added it to.
                        is ApiResult.Success -> navigator.resetTo(
                            if (result.value.newUser && !isOnATeam(teamRepository)) Route.RoleSelection else Route.Home
                        )
                        is ApiResult.Failure -> stop(result.message)
                    }
                }
            }
        }
    }
    val label: @Composable () -> Unit = {
        if (working) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("Continue with ${provider.label}")
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                "or",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(Modifier.weight(1f))
        }

        when (provider) {
            // Apple's guidelines: a black button on a light background, a white one on a dark background.
            SocialProvider.APPLE -> Button(
                onClick = onClick,
                enabled = enabled && !working,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lightBackground) Color.Black else Color.White,
                    contentColor = if (lightBackground) Color.White else Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) { label() }
            SocialProvider.GOOGLE -> OutlinedButton(
                onClick = onClick,
                enabled = enabled && !working,
                modifier = Modifier.fillMaxWidth()
            ) { label() }
        }

        Text(
            "New to Kickabout? This creates your account, and means you accept the terms, privacy and safeguarding policy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Whether the person just signed in already belongs to a team. If that can't be checked, assume not. */
private suspend fun isOnATeam(teamRepository: TeamRepository): Boolean =
    when (val result = teamRepository.mine()) {
        is ApiResult.Success -> result.value.isNotEmpty()
        is ApiResult.Failure -> false
    }
