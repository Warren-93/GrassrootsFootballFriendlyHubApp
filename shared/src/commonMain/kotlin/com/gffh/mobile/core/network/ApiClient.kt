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
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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

    suspend inline fun <reified T> request(block: HttpRequestBuilder.() -> Unit): ApiResult<T> {
        return try {
            val response: HttpResponse = client.request {
                tokenStore.currentAccessToken()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                block()
            }
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
        } catch (e: Exception) {
            ApiResult.Failure(
                code = "NETWORK_ERROR",
                message = "Check your connection and try again.",
                requestId = null,
                httpStatus = null
            )
        }
    }
}
