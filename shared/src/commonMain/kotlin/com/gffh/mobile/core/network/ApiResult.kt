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

/**
 * Whether a failed `POST /api/v1/auth/refresh` means the session is really
 * over - the API refused the refresh token (401 INVALID_REFRESH_TOKEN), the
 * account is suspended (403) or the request was malformed (400) - rather than
 * that the API couldn't be asked: no connection, a timeout while the free-tier
 * host wakes up, a 5xx. Those keep the stored session to try again later.
 */
fun ApiResult.Failure.endsSession(): Boolean = httpStatus == 400 || httpStatus == 401 || httpStatus == 403
