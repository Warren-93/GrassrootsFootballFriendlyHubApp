@file:OptIn(ExperimentalForeignApi::class)

package com.gffh.mobile.core.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * "Continue with Google" on iOS. Google has no Credential Manager here, so
 * the person signs in on Google's own page in Apple's system browser sheet
 * (ASWebAuthenticationSession - Safari's cookies, so someone already signed
 * in to Google there just picks an account). Google redirects back with a
 * code, which is traded for an ID token - see [GoogleOAuthRequest]. That
 * token's audience is the iOS client ID, which the API accepts once it's in
 * GOOGLE_ADDITIONAL_CLIENT_IDS.
 */
internal class GoogleWebSignIn(private val clientId: String) : SocialSignIn {

    override val provider = SocialProvider.GOOGLE

    override suspend fun idToken(): SocialSignInResult {
        val request = GoogleOAuthRequest(clientId, codeVerifier = randomToken() + randomToken(), state = randomToken())
        val code = when (val callback = showGooglePage(request)) {
            is GoogleOAuthCallback.Code -> callback.code
            GoogleOAuthCallback.Cancelled -> return SocialSignInResult.Cancelled
            GoogleOAuthCallback.Failed -> return SocialSignInResult.Failed()
        }
        return try {
            val response = tokenClient.submitForm(
                url = GoogleOAuthRequest.TOKEN_ENDPOINT,
                formParameters = Parameters.build { request.tokenRequest(code).forEach { (key, value) -> append(key, value) } }
            )
            val idToken = if (response.status.isSuccess()) GoogleOAuthRequest.idTokenFrom(response.bodyAsText()) else null
            // The name and email are in the token itself, so only the token goes to the API.
            if (idToken == null) SocialSignInResult.Failed() else SocialSignInResult.Token(idToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SocialSignInResult.Failed("Couldn't reach Google - check your connection and try again.")
        }
    }
}

/** Only ever talks to Google's token endpoint, so it's separate from the API's client. */
private val tokenClient by lazy { HttpClient(Darwin) }

/** 32 hex characters from a random (v4) UUID, which iOS draws from its secure random source. */
private fun randomToken(): String = NSUUID().UUIDString.replace("-", "").lowercase()

/**
 * The session holds its presentation context provider weakly, and nothing
 * else holds the session, so both are kept here while Google's page is up.
 * Only ever touched on the main thread.
 */
private var activeSession: Pair<ASWebAuthenticationSession, WebSignInAnchor>? = null

/** ASWebAuthenticationSessionErrorCodeCanceledLogin: the person closed the sheet. */
private const val WEB_SIGN_IN_CANCELLED = 1L

private suspend fun showGooglePage(request: GoogleOAuthRequest): GoogleOAuthCallback =
    suspendCancellableCoroutine { continuation ->
        val window = UIApplication.sharedApplication.keyWindow
        val url = NSURL.URLWithString(request.authorizeUrl)
        if (window == null || url == null) {
            continuation.resume(GoogleOAuthCallback.Failed)
            return@suspendCancellableCoroutine
        }
        val anchor = WebSignInAnchor(window)
        val session = ASWebAuthenticationSession(url, request.callbackScheme) { callbackUrl: NSURL?, error: NSError? ->
            activeSession = null
            val result = when {
                callbackUrl != null -> request.parseCallback(callbackUrl.absoluteString ?: "")
                error?.code == WEB_SIGN_IN_CANCELLED -> GoogleOAuthCallback.Cancelled
                else -> GoogleOAuthCallback.Failed
            }
            // The screen may have gone (and cancelled this) while the page was up.
            if (continuation.isActive) continuation.resume(result)
        }
        session.presentationContextProvider = anchor
        activeSession = session to anchor
        if (!session.start()) {
            activeSession = null
            continuation.resume(GoogleOAuthCallback.Failed)
        }
    }

/** Google's page is shown over the app's own window. */
private class WebSignInAnchor(private val anchor: UIWindow) :
    NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {

    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow = anchor
}
