package com.gffh.mobile.model

import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/config`: what the app needs to know before anyone signs in -
 * the support address (null when none is configured) and which third-party
 * sign-in options are switched on.
 */
@Serializable
data class ClientConfigView(
    val supportEmail: String? = null,
    val google: GoogleSignInConfig = GoogleSignInConfig(),
    val apple: AppleSignInConfig = AppleSignInConfig()
)

/** [clientId] is the Google OAuth web client ID (Android's serverClientId); null when Google sign-in is off. */
@Serializable
data class GoogleSignInConfig(
    val enabled: Boolean = false,
    val clientId: String? = null
)

/** [servicesId] is only for web sign-in with Apple; the iOS app can use Apple sign-in whenever [enabled]. */
@Serializable
data class AppleSignInConfig(
    val enabled: Boolean = false,
    val servicesId: String? = null
)
