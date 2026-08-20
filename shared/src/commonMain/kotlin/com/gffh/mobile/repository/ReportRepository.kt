package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.BlockRequest
import com.gffh.mobile.model.BlockView
import com.gffh.mobile.model.SubmitReportRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class ReportRepository(private val api: ApiClient) {

    suspend fun submit(teamId: String, request: SubmitReportRequest): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/reports")
        setBody(request)
    }

    suspend fun block(teamId: String, request: BlockRequest): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/blocks")
        setBody(request)
    }

    suspend fun listBlocks(teamId: String): ApiResult<List<BlockView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/blocks")
    }

    suspend fun unblock(teamId: String, blockId: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/teams/$teamId/blocks/$blockId")
    }
}
