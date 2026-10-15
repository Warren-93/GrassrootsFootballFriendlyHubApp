package com.gffh.mobile.core.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.gffh.mobile.model.ClientConfigView
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

/**
 * "Continue with Google" through Credential Manager, whenever the API has
 * Google sign-in on. The config's client ID is the Google OAuth *Web* client
 * (the token's audience, which the API checks); Google matches this app to
 * its Android OAuth client by package name and signing certificate - see the
 * README's "Google and Apple sign-in" section.
 */
@Composable
actual fun rememberSocialSignIn(config: ClientConfigView?): SocialSignIn? {
    // The Activity: Credential Manager shows its sheet over it.
    val context = LocalContext.current
    val clientId = config?.google?.takeIf { it.enabled }?.clientId?.takeIf { it.isNotBlank() }
    return remember(context, clientId) { clientId?.let { GoogleSignIn(context, it) } }
}

private class GoogleSignIn(private val context: Context, private val serverClientId: String) : SocialSignIn {

    override val provider = SocialProvider.GOOGLE

    /**
     * The button flow (GetSignInWithGoogleOption), not the bottom sheet: it
     * lists every Google account on the phone, and offers to add one when
     * there are none, rather than only accounts that used the app before.
     */
    override suspend fun idToken(): SocialSignInResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context = context, request = request).credential
            if (credential is CustomCredential) {
                // The name and email are in the token itself, so only the token goes to the API.
                SocialSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                SocialSignInResult.Failed()
            }
        } catch (e: GetCredentialCancellationException) {
            SocialSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SocialSignInResult.Failed("Add a Google account to this phone first, or use your email and password.")
        } catch (e: GetCredentialException) {
            // Usually set-up: a wrong client ID, or no Android OAuth client for this signing certificate.
            Log.w(TAG, "Google sign-in failed: ${e.type}", e)
            SocialSignInResult.Failed()
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Google sign-in returned a credential that isn't an ID token", e)
            SocialSignInResult.Failed()
        } catch (e: CancellationException) {
            // The screen went away mid-sign-in: let the coroutine finish cancelling.
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in failed", e)
            SocialSignInResult.Failed()
        }
    }

    private companion object {
        const val TAG = "KickaboutSignIn"
    }
}
