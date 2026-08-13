package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.CancelFixtureRequest
import com.gffh.mobile.model.FixtureView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class FixtureRepository(private val api: ApiClient) {

    suspend fun list(teamId: String): ApiResult<List<FixtureView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/fixtures?teamId=$teamId")
    }

    suspend fun get(fixtureId: String): ApiResult<FixtureView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/fixtures/$fixtureId")
    }

    suspend fun cancel(fixtureId: String, reason: String?): ApiResult<FixtureView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/fixtures/$fixtureId/cancel")
        setBody(CancelFixtureRequest(reason))
    }
}
