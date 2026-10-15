@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.gffh.mobile.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gffh.mobile.model.ClientConfigView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Sign in with Apple, whenever the API has Apple sign-in on. Needs the
 * `com.apple.developer.applesignin` entitlement (iosApp/project.yml) and the
 * Sign in with Apple capability on the bundle ID - see the README's "Google
 * and Apple sign-in" section. The token's audience is the bundle ID, which
 * the API accepts as APPLE_BUNDLE_ID.
 */
@Composable
actual fun rememberSocialSignIn(config: ClientConfigView?): SocialSignIn? {
    val enabled = config?.apple?.enabled == true
    return remember(enabled) { if (enabled) AppleSignIn else null }
}

/**
 * ASAuthorizationController holds its delegate and presentation context
 * provider weakly, so both are kept here, with the controller, for as long as
 * Apple's sheet is up. Only ever touched on the main thread.
 */
private var activeRequest: Pair<ASAuthorizationController, AppleSignInDelegate>? = null

/** ASAuthorizationErrorCanceled: the person closed Apple's sheet. */
private const val APPLE_SIGN_IN_CANCELLED = 1001L

private object AppleSignIn : SocialSignIn {

    override val provider = SocialProvider.APPLE

    override suspend fun idToken(): SocialSignInResult = suspendCancellableCoroutine { continuation ->
        val window = UIApplication.sharedApplication.keyWindow
        if (window == null) {
            continuation.resume(SocialSignInResult.Failed())
            return@suspendCancellableCoroutine
        }
        val request = ASAuthorizationAppleIDProvider().createRequest()
        // Apple only shares the name and email the first time someone signs in to the app.
        request.requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)

        val controller = ASAuthorizationController(authorizationRequests = listOf(request))
        val delegate = AppleSignInDelegate(window) { result ->
            activeRequest = null
            // The screen may have gone (and cancelled this) while the sheet was up.
            if (continuation.isActive) continuation.resume(result)
        }
        activeRequest = controller to delegate
        controller.delegate = delegate
        controller.presentationContextProvider = delegate
        controller.performRequests()
    }
}

private class AppleSignInDelegate(
    private val anchor: UIWindow,
    private val onResult: (SocialSignInResult) -> Unit
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    /** Apple's sheet is shown over the app's own window. */
    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): UIWindow = anchor

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val idToken = credential?.identityToken?.let { utf8String(it) }
        if (idToken.isNullOrBlank()) {
            onResult(SocialSignInResult.Failed())
            return
        }
        val name = credential?.fullName
            ?.let { listOfNotNull(it.givenName, it.familyName).filter { part -> part.isNotBlank() }.joinToString(" ") }
            ?.takeIf { it.isNotBlank() }
        onResult(SocialSignInResult.Token(idToken, name))
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        onResult(
            if (didCompleteWithError.code == APPLE_SIGN_IN_CANCELLED) SocialSignInResult.Cancelled
            else SocialSignInResult.Failed()
        )
    }
}

/** The identity token arrives as the UTF-8 bytes of the JWT. */
private fun utf8String(data: NSData): String? = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
