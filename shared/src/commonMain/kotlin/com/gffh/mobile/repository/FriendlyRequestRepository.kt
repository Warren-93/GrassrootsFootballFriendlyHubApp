package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.FriendlyRequestActionRequest
import com.gffh.mobile.model.FriendlyRequestView
import com.gffh.mobile.model.SendFriendlyRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class FriendlyRequestRepository(private val api: ApiClient) {

    suspend fun send(request: SendFriendlyRequest): ApiResult<FriendlyRequestView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/friendly-requests")
        setBody(request)
    }

    suspend fun act(requestId: String, action: String, reason: String? = null): ApiResult<FriendlyRequestView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/friendly-requests/$requestId/actions/$action")
        if (reason != null) setBody(FriendlyRequestActionRequest(reason))
    }

    suspend fun list(teamId: String): ApiResult<List<FriendlyRequestView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/friendly-requests?teamId=$teamId")
    }

    suspend fun get(requestId: String): ApiResult<FriendlyRequestView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/friendly-requests/$requestId")
    }
}
