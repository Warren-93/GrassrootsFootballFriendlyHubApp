package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ConversationView
import com.gffh.mobile.model.MessageView
import com.gffh.mobile.model.SendMessageRequest
import com.gffh.mobile.model.StartConversationRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class ConversationRepository(private val api: ApiClient) {

    suspend fun start(teamId: String, otherTeamId: String, fixtureId: String? = null): ApiResult<ConversationView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/conversations")
        setBody(StartConversationRequest(teamId, otherTeamId, fixtureId))
    }

    suspend fun list(teamId: String): ApiResult<List<ConversationView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/conversations")
    }

    suspend fun get(conversationId: String): ApiResult<ConversationView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/conversations/$conversationId")
    }

    /** Oldest first. With [since] (an ISO time), only messages from then on, including one at exactly that time. */
    suspend fun messages(conversationId: String, since: String? = null): ApiResult<List<MessageView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/conversations/$conversationId/messages")
        if (since != null) parameter("since", since)
    }

    /** Text, a GIF (its id from a GIF search), or both. */
    suspend fun send(conversationId: String, body: String, gifId: String? = null): ApiResult<MessageView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/conversations/$conversationId/messages")
        setBody(SendMessageRequest(body, gifId))
    }
}
