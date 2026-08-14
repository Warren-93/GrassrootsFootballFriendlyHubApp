package com.gffh.mobile.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
}
