package com.gffh.mobile.core.network

import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.model.ApiErrorEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thin wrapper around a configured [HttpClient]. Every repository call goes
 * through [request], which never throws - failures become [ApiResult.Failure]
 * carrying the backend's structured error code (GlobalExceptionHandler on the
 * server), which is exactly what the Screen Build Specification's field-level
 * validation and guardrail sheets need to render.
 *
 * The bearer token is attached manually rather than via Ktor's `Auth` plugin:
 * that plugin's `loadTokens` is called once and cached, so the very first
 * request in the app's life (registration, made while signed out) permanently
 * caches "no token" and every authenticated request after sign-in silently
 * goes out without one. Reading [TokenStore] fresh on every call avoids that.
 */
class ApiClient(baseUrl: String, @PublishedApi internal val tokenStore: TokenStore) {

    /**
     * Renews the session after an authenticated call gets a 401, answering
     * whether it did. Set by AuthRepository when it's created - it owns the
     * refresh call and decides when a refused refresh token ends the session -
     * so neither class needs the other at construction.
     */
    var sessionRefresher: (suspend () -> Boolean)? = null

    /** One refresh at a time: calls that hit a 401 together share it. */
    private val refreshMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @PublishedApi
    internal val client = HttpClient(httpClientEngineFactory()) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * Pass [authenticated] = false for public endpoints (`GET /api/v1/config`):
     * the API rejects an expired bearer token with 401 even on a route that
     * needs none, so a stale session must not break them.
     *
     * Any 2xx is a success, including 202 Accepted with a body (adding a member
     * by email answers 202 with an invitation when there's no account yet).
     */
    suspend inline fun <reified T> request(
        authenticated: Boolean = true,
        block: HttpRequestBuilder.() -> Unit
    ): ApiResult<T> {
        return try {
            val response: HttpResponse = execute(authenticated, HttpRequestBuilder().apply(block))
            if (response.status.value in 200..299) {
                ApiResult.Success(response.body())
            } else {
                val envelope = runCatching { response.body<ApiErrorEnvelope>() }.getOrNull()
                ApiResult.Failure(
                    code = envelope?.error?.code ?: "HTTP_${response.status.value}",
                    message = envelope?.error?.message ?: "Something went wrong. Please try again.",
                    requestId = envelope?.error?.requestId,
                    httpStatus = response.status.value
                )
            }
        } catch (e: CancellationException) {
            // The caller went away (a screen closed, a timeout fired): that has
            // to reach it, not turn into a "check your connection" failure.
            throw e
        } catch (e: Exception) {
            ApiResult.Failure(
                code = "NETWORK_ERROR",
                message = "Check your connection and try again.",
                requestId = null,
                httpStatus = null
            )
        }
    }

    /**
     * Sends [prepared] with the stored access token when [authenticated]. A
     * 401 to that token renews the session once ([renewedAccessToken]) and
     * sends the request again with the new one; otherwise the 401 stands.
     */
    @PublishedApi
    internal suspend fun execute(authenticated: Boolean, prepared: HttpRequestBuilder): HttpResponse {
        val sentToken = if (authenticated) tokenStore.currentAccessToken() else null
        val response = send(prepared, sentToken)
        if (sentToken == null || response.status != HttpStatusCode.Unauthorized) return response
        val renewed = renewedAccessToken(sentToken) ?: return response
        return send(prepared, renewed)
    }

    private suspend fun send(prepared: HttpRequestBuilder, accessToken: String?): HttpResponse =
        client.request(
            HttpRequestBuilder().takeFrom(prepared).apply {
                accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        )

    /**
     * An access token to retry with after [rejected] got a 401, or null when
     * there's none: no refresher, no session, or the refresh failed (when the
     * API refused the refresh token, AuthRepository has dropped the session
     * and App() takes the user back to Welcome). A call that waited while
     * another refreshed just uses the token that one got.
     */
    private suspend fun renewedAccessToken(rejected: String): String? {
        val refresher = sessionRefresher ?: return null
        return refreshMutex.withLock {
            val current = tokenStore.currentAccessToken() ?: return@withLock null
            if (current != rejected) return@withLock current
            if (refresher()) tokenStore.currentAccessToken() else null
        }
    }
}
