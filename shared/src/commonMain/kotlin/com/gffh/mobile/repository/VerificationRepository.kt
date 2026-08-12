package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SubmitVerificationRequest
import com.gffh.mobile.model.VerificationRequestView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class VerificationRepository(private val api: ApiClient) {

    suspend fun getForTeam(teamId: String): ApiResult<VerificationRequestView?> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/verification")
    }

    suspend fun submit(teamId: String, request: SubmitVerificationRequest): ApiResult<VerificationRequestView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/verification")
        setBody(request)
    }
}
