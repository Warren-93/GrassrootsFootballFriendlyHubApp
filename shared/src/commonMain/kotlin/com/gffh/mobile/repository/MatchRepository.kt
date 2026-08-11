package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SearchRequest
import com.gffh.mobile.model.SearchResponse
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class MatchRepository(private val api: ApiClient) {

    suspend fun search(request: SearchRequest): ApiResult<SearchResponse> = api.request {
        method = HttpMethod.Post
        url("/api/v1/matches/search")
        setBody(request)
    }
}
