package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.DeletePushTokenRequest
import com.gffh.mobile.model.RegisterPushTokenRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

/** The devices that get the signed-in user's push notifications. Both calls answer 204. */
class PushTokenRepository(private val api: ApiClient) {

    /** Idempotent: the API moves the token to this user if another account had it. */
    suspend fun register(platform: String, token: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/me/push-tokens")
        setBody(RegisterPushTokenRequest(platform, token))
    }

    suspend fun unregister(token: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/me/push-tokens")
        setBody(DeletePushTokenRequest(token))
    }
}
