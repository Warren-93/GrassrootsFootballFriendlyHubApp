package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.ClientConfigView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * `GET /api/v1/config`: the support address and which sign-in options are
 * switched on. It only changes when the API is redeployed with new settings,
 * so it's fetched once per app run and cached. A failed fetch isn't cached -
 * the next caller simply tries again.
 */
class ConfigRepository(private val api: ApiClient) {

    private val mutex = Mutex()
    private val _config = MutableStateFlow<ClientConfigView?>(null)

    /** The config once [get] has fetched it, null before then. */
    val config: StateFlow<ClientConfigView?> = _config

    /** The cached config, fetching it first if this is the first call (concurrent callers share one request). */
    suspend fun get(): ApiResult<ClientConfigView> = mutex.withLock {
        _config.value?.let { return@withLock ApiResult.Success(it) }
        // Public endpoint: no bearer token, so a stale session can't turn it into a 401.
        val result = api.request<ClientConfigView>(authenticated = false) {
            method = HttpMethod.Get
            url("/api/v1/config")
        }
        if (result is ApiResult.Success) _config.value = result.value
        result
    }
}
