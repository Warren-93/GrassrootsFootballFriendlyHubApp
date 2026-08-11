package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ClubView
import com.gffh.mobile.model.CreateClubRequest
import com.gffh.mobile.model.UpdateClubRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class ClubRepository(private val api: ApiClient) {

    suspend fun create(request: CreateClubRequest): ApiResult<ClubView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/clubs")
        setBody(request)
    }

    suspend fun get(clubId: String): ApiResult<ClubView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/clubs/$clubId")
    }

    suspend fun update(clubId: String, request: UpdateClubRequest): ApiResult<ClubView> = api.request {
        method = HttpMethod.Patch
        url("/api/v1/clubs/$clubId")
        setBody(request)
    }
}
