package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.DeletePushTokenRequest
import com.gffh.mobile.model.RegisterPushTokenRequest
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

/** The devices that get the signed-in user's push notifications. Both calls answer 204. */
class PushTokenRepository(private val api: ApiClient) {

    /** Idempotent: the API moves the token to this user if another account had it. */
    suspend fun register(platform: String, token: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/me/push-tokens")
        setBody(RegisterPushTokenRequest(platform, token))
    }

    /** [accessToken] stands in for the stored session's - sign-out's retry after a 401 (see PushRegistration). */
    suspend fun unregister(token: String, accessToken: String? = null): ApiResult<Unit> =
        api.request(authenticated = accessToken == null) {
            method = HttpMethod.Delete
            url("/api/v1/me/push-tokens")
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(DeletePushTokenRequest(token))
        }
}
