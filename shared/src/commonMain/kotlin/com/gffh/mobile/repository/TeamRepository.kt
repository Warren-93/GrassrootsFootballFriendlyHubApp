package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CreateTeamRequest
import com.gffh.mobile.model.TeamView
import com.gffh.mobile.model.UpdateTeamRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class TeamRepository(private val api: ApiClient) {

    suspend fun create(request: CreateTeamRequest): ApiResult<TeamView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams")
        setBody(request)
    }

    suspend fun get(teamId: String): ApiResult<TeamView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId")
    }

    suspend fun update(teamId: String, request: UpdateTeamRequest): ApiResult<TeamView> = api.request {
        method = HttpMethod.Patch
        url("/api/v1/teams/$teamId")
        setBody(request)
    }
}
