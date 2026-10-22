package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.GifResults
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

/**
 * GIF search for the chat's GIF picker. Goes through the API, which holds the
 * GIPHY key and only returns GIFs at the configured safe rating.
 */
class GifRepository(private val api: ApiClient) {

    suspend fun trending(limit: Int = 24): ApiResult<GifResults> = api.request {
        method = HttpMethod.Get
        url("/api/v1/gifs/trending")
        parameter("limit", limit)
    }

    suspend fun search(query: String, limit: Int = 24): ApiResult<GifResults> = api.request {
        method = HttpMethod.Get
        url("/api/v1/gifs/search")
        parameter("q", query)
        parameter("limit", limit)
    }
}
