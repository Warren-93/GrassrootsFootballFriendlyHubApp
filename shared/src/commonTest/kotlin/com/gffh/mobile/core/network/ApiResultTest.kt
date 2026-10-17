package com.gffh.mobile.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiResultTest {

    @Test
    fun mapTransformsTheValueOfASuccess() {
        val result: ApiResult<Int> = ApiResult.Success(2)

        val mapped = result.map { it * 21 }

        assertIs<ApiResult.Success<Int>>(mapped)
        assertEquals(42, mapped.value)
    }

    @Test
    fun mapPassesThroughAFailureUnchanged() {
        val failure = ApiResult.Failure(code = "NOT_FOUND", message = "Team not found", requestId = "req-1", httpStatus = 404)
        val result: ApiResult<Int> = failure

        val mapped = result.map { it * 21 }

        assertIs<ApiResult.Failure>(mapped)
        assertEquals(failure, mapped)
    }

    @Test
    fun onlyARefusedRefreshTokenEndsTheSession() {
        fun failure(status: Int?) = ApiResult.Failure("X", "x", null, status)

        assertTrue(failure(401).endsSession())
        assertTrue(failure(403).endsSession())
        assertTrue(failure(400).endsSession())
        // No connection, a timeout while the API wakes up, or a server error: keep it.
        assertFalse(failure(null).endsSession())
        assertFalse(failure(500).endsSession())
        assertFalse(failure(503).endsSession())
        assertFalse(failure(429).endsSession())
    }
}
