package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateVenueRequest
import com.gffh.mobile.model.UpdateVenueRequest
import com.gffh.mobile.model.VenueView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class VenueRepository(private val api: ApiClient) {

    suspend fun create(request: CreateVenueRequest): ApiResult<VenueView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/venues")
        setBody(request)
    }

    suspend fun list(clubId: String): ApiResult<List<VenueView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/venues?clubId=$clubId")
    }

    suspend fun update(venueId: String, request: UpdateVenueRequest): ApiResult<VenueView> = api.request {
        method = HttpMethod.Patch
        url("/api/v1/venues/$venueId")
        setBody(request)
    }

    suspend fun setDefault(venueId: String): ApiResult<VenueView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/venues/$venueId/default")
    }

    suspend fun delete(venueId: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/venues/$venueId")
    }
}
