package com.gffh.mobile.model

import kotlinx.serialization.Serializable

/**
 * `POST /api/v1/me/push-tokens`. [platform] is "ANDROID" (an FCM registration
 * token) or "IOS" (the APNs device token in lowercase hex).
 */
@Serializable
data class RegisterPushTokenRequest(val platform: String, val token: String)

/** `DELETE /api/v1/me/push-tokens`, sent on sign-out so this device stops getting the account's notifications. */
@Serializable
data class DeletePushTokenRequest(val token: String)
