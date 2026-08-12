package com.gffh.mobile.repository

import com.gffh.mobile.core.auth.StoredSession
import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.network.map
import com.gffh.mobile.model.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(private val api: ApiClient, private val tokenStore: TokenStore) {

    val session: StateFlow<StoredSession?> = tokenStore.session

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

    suspend fun refresh(): ApiResult<Unit> {
        val refreshToken = tokenStore.currentRefreshToken()
            ?: return ApiResult.Failure("NO_SESSION", "Not signed in.", null, null)
        val result = api.request<TokenResponse> {
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
                tokenStore.clear()
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

    fun signOut() = tokenStore.clear()

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
