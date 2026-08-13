package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.MessageView
import com.gffh.mobile.model.SendMessageRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class MessageRepository(private val api: ApiClient) {

    suspend fun list(fixtureId: String): ApiResult<List<MessageView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/fixtures/$fixtureId/messages")
    }

    suspend fun send(fixtureId: String, body: String): ApiResult<MessageView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/fixtures/$fixtureId/messages")
        setBody(SendMessageRequest(body))
    }
}
