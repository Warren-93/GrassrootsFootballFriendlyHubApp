package com.gffh.mobile.core.auth

import androidx.compose.runtime.Composable
import com.gffh.mobile.model.ClientConfigView

/** [path] is the provider's segment of `POST /api/v1/auth/oauth/{provider}`; [label] is its name in the UI. */
enum class SocialProvider(val path: String, val label: String) {
    GOOGLE("google", "Google"),
    APPLE("apple", "Apple")
}

/** What came back from the platform's own sign-in sheet. */
sealed class SocialSignInResult {
    /**
     * [idToken] is the provider's signed ID token, for the API to verify.
     * [displayName] is only ever set by Apple, which hands the person's name
     * to the app (the first time only) rather than putting it in the token.
     */
    data class Token(val idToken: String, val displayName: String? = null) : SocialSignInResult()

    /** The person backed out of the sheet - not an error, so nothing is shown. */
    data object Cancelled : SocialSignInResult()

    /** Anything else. [message] is a friendlier explanation when the platform gave a specific reason. */
    data class Failed(val message: String? = null) : SocialSignInResult()
}

/** One platform's way of getting an ID token for [provider]. */
interface SocialSignIn {
    val provider: SocialProvider

    /** Shows the provider's sheet and waits for the person to finish with it. Never throws. */
    suspend fun idToken(): SocialSignInResult
}

/**
 * The sign-ins this platform offers, in the order they're shown, for the
 * providers [config] (`GET /api/v1/config`) has switched on - empty while
 * the config hasn't loaded yet. Android offers Google, through Credential
 * Manager. iOS offers Sign in with Apple, plus Google through Google's own
 * page when the app was built with an iOS client ID; Apple's App Review
 * rule 4.8 means Google is only offered there alongside Apple. Apple on
 * Android isn't offered.
 */
@Composable
expect fun rememberSocialSignIns(config: ClientConfigView?): List<SocialSignIn>
