package com.gffh.mobile.core.network

/**
 * Every repository call returns this rather than throwing, so a ViewModel can
 * render [ApiFailure.code] directly - the backend's error envelope (Technical
 * Specification section 12) is designed to be shown, not just logged.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val code: String, val message: String, val requestId: String?, val httpStatus: Int?) : ApiResult<Nothing>()
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
