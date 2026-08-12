package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AccountExport
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class PrivacyRepository(private val api: ApiClient) {

    suspend fun export(): ApiResult<AccountExport> = api.request {
        method = HttpMethod.Get
        url("/api/v1/me/export")
    }

    suspend fun deleteAccount(): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/me")
    }
}
