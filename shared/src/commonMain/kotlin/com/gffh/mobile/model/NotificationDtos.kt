package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationView(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedTeamId: String? = null,
    val relatedRequestId: String? = null,
    val relatedFixtureId: String? = null,
    val relatedConversationId: String? = null,
    val read: Boolean,
    val createdAt: String
)

@Serializable
data class UnreadCountView(val count: Long)

/**
 * The four category flags pick which events create a notification at all;
 * [emailEnabled] and [pushEnabled] pick whether those also arrive by email
 * and as phone notifications. Both default to on, as the API does for an
 * account that has never changed them.
 */
@Serializable
data class NotificationPreferenceView(
    val friendlyRequests: Boolean,
    val fixtures: Boolean,
    val verification: Boolean,
    val messages: Boolean,
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = true
)

/**
 * PATCH body for notification preferences. [emailEnabled] and [pushEnabled]
 * are nullable (null = keep the current setting, which is what older clients
 * get by leaving them out) rather than defaulted: ApiClient's Json has
 * encodeDefaults = false, so a `= true` default would never be sent and
 * switching one back on would silently do nothing.
 */
@Serializable
data class UpdateNotificationPreferenceRequest(
    val friendlyRequests: Boolean,
    val fixtures: Boolean,
    val verification: Boolean,
    val messages: Boolean,
    val emailEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null
)
