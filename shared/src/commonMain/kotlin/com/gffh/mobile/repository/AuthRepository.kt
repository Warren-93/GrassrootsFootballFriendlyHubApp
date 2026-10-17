package com.gffh.mobile.repository

import com.gffh.mobile.core.auth.SocialProvider
import com.gffh.mobile.core.auth.StoredSession
import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.network.endsSession
import com.gffh.mobile.core.network.map
import com.gffh.mobile.core.push.PushRegistration
import com.gffh.mobile.model.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.StateFlow

/**
 * [pushRegistration] is only there for sign-out: this device's push token has
 * to be withdrawn from the API while the access token still works. Null in
 * tests and previews, where there's no push transport to withdraw from.
 */
class AuthRepository(
    private val api: ApiClient,
    private val tokenStore: TokenStore,
    private val pushRegistration: PushRegistration? = null
) {

    val session: StateFlow<StoredSession?> = tokenStore.session

    init {
        // Lets ApiClient renew a lapsed access token mid-session and retry the call.
        api.sessionRefresher = { refresh() is ApiResult.Success }
    }

    suspend fun register(email: String, password: String, displayName: String): ApiResult<TokenResponse> {
        val result = api.request<TokenResponse> {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/auth/register")
            setBody(RegisterRequest(email, password, displayName))
        }
        if (result is ApiResult.Success) persist(result.value)
        return result
    }

    suspend fun login(email: String, password: String): ApiResult<TokenResponse> {
        val result = api.request<TokenResponse> {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/auth/login")
            setBody(LoginRequest(email, password))
        }
        if (result is ApiResult.Success) persist(result.value)
        return result
    }

    /**
     * `POST /api/v1/auth/oauth/{google|apple}`: trades the provider's ID
     * token for a Kickabout session, stored exactly as [login] stores one -
     * which is also what gets this phone registered for push, since
     * PushRegistration sends the device token whenever a session appears.
     * [TokenResponse.newUser] says whether this sign-in created the account.
     * [displayName] is Apple's, given only on someone's first Apple sign-in.
     */
    suspend fun signInWith(provider: SocialProvider, idToken: String, displayName: String? = null): ApiResult<TokenResponse> {
        val result = api.request<TokenResponse> {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/auth/oauth/${provider.path}")
            setBody(OAuthSignInRequest(idToken, displayName?.trim()?.takeIf { it.isNotEmpty() }))
        }
        if (result is ApiResult.Success) persist(result.value)
        return result
    }

    /**
     * Trades the refresh token for a new pair. Unauthenticated: the API answers
     * 401 to any expired bearer token, even here. The session is only dropped
     * when the API refuses the refresh token ([endsSession]), not when it
     * can't be reached.
     */
    suspend fun refresh(): ApiResult<Unit> {
        val refreshToken = tokenStore.currentRefreshToken()
            ?: return ApiResult.Failure("NO_SESSION", "Not signed in.", null, null)
        val result = api.request<TokenResponse>(authenticated = false) {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/auth/refresh")
            setBody(RefreshRequest(refreshToken))
        }
        return when (result) {
            is ApiResult.Success -> {
                tokenStore.updateTokens(result.value.accessToken, result.value.refreshToken)
                ApiResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                if (result.endsSession()) tokenStore.clear()
                result
            }
        }
    }

    suspend fun me(): ApiResult<UserView> = api.request<UserView> {
        method = io.ktor.http.HttpMethod.Get
        url("/api/v1/me")
    }.also { result ->
        if (result is ApiResult.Success) tokenStore.updateEmailVerified(result.value.emailVerified)
    }

    suspend fun requestPasswordReset(email: String): ApiResult<Unit> = api.request<Unit> {
        method = io.ktor.http.HttpMethod.Post
        url("/api/v1/auth/password-reset")
        setBody(PasswordResetRequest(email))
    }

    suspend fun confirmPasswordReset(token: String, newPassword: String): ApiResult<Unit> = api.request<Unit> {
        method = io.ktor.http.HttpMethod.Post
        url("/api/v1/auth/password-reset/confirm")
        setBody(PasswordResetConfirmRequest(token, newPassword))
    }

    suspend fun resendVerification(): ApiResult<VerificationResendResponse> = api.request<VerificationResendResponse> {
        method = io.ktor.http.HttpMethod.Post
        url("/api/v1/auth/verify/resend")
    }

    suspend fun confirmVerification(token: String): ApiResult<Unit> = api.request<Unit> {
        method = io.ktor.http.HttpMethod.Post
        url("/api/v1/auth/verify/confirm")
        setBody(VerifyConfirmRequest(token))
    }.also { result ->
        if (result is ApiResult.Success) tokenStore.updateEmailVerified(true)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit> = api.request<Unit> {
        method = io.ktor.http.HttpMethod.Post
        url("/api/v1/me/password")
        setBody(ChangePasswordRequest(currentPassword, newPassword))
    }

    /** The new address starts unverified, so the stored session follows it straight away. */
    suspend fun changeEmail(newEmail: String, currentPassword: String): ApiResult<ChangeEmailResponse> =
        api.request<ChangeEmailResponse> {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/me/email")
            setBody(ChangeEmailRequest(newEmail, currentPassword))
        }.also { result ->
            if (result is ApiResult.Success) tokenStore.updateEmail(result.value.user.email, result.value.user.emailVerified)
        }

    /**
     * Signs out on purpose: tells the API to stop pushing to this device
     * first (`DELETE /api/v1/me/push-tokens`, which needs an access token that
     * still works - see [refreshedAccessToken]), then drops the session.
     */
    suspend fun signOut() {
        pushRegistration?.unregister(freshAccessToken = { refreshedAccessToken() })
        tokenStore.clear()
    }

    /**
     * A new access token for sign-out's last call, when the stored one has
     * lapsed. Deliberately not saved: the session is dropped straight after.
     */
    private suspend fun refreshedAccessToken(): String? {
        val refreshToken = tokenStore.currentRefreshToken() ?: return null
        val result = api.request<TokenResponse>(authenticated = false) {
            method = io.ktor.http.HttpMethod.Post
            url("/api/v1/auth/refresh")
            setBody(RefreshRequest(refreshToken))
        }
        return when (result) {
            is ApiResult.Success -> result.value.accessToken
            is ApiResult.Failure -> null
        }
    }

    /**
     * Drops the stored session without telling the API anything - for when
     * there's nothing to tell it: a session that can no longer be refreshed,
     * or an account that has just been deleted (the API drops its push tokens
     * with it).
     */
    fun clearSession() = tokenStore.clear()

    private fun persist(response: TokenResponse) {
        tokenStore.save(
            StoredSession(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.user.id,
                email = response.user.email,
                displayName = response.user.displayName,
                emailVerified = response.user.emailVerified
            )
        )
    }
}
