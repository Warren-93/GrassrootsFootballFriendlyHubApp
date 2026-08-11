package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateSlotRequest
import com.gffh.mobile.model.SlotView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class AvailabilityRepository(private val api: ApiClient) {

    suspend fun create(teamId: String, request: CreateSlotRequest): ApiResult<SlotView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/availability")
        setBody(request)
    }

    suspend fun list(teamId: String, from: String? = null, to: String? = null): ApiResult<List<SlotView>> = api.request {
        method = HttpMethod.Get
        val query = listOfNotNull(from?.let { "from=$it" }, to?.let { "to=$it" }).joinToString("&")
        url("/api/v1/teams/$teamId/availability" + if (query.isNotEmpty()) "?$query" else "")
    }

    suspend fun get(teamId: String, slotId: String): ApiResult<SlotView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/availability/$slotId")
    }

    suspend fun update(teamId: String, slotId: String, request: CreateSlotRequest): ApiResult<SlotView> = api.request {
        method = HttpMethod.Put
        url("/api/v1/teams/$teamId/availability/$slotId")
        setBody(request)
    }

    suspend fun withdraw(teamId: String, slotId: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/teams/$teamId/availability/$slotId")
    }
}
