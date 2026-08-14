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

@Serializable
data class NotificationPreferenceView(
    val friendlyRequests: Boolean,
    val fixtures: Boolean,
    val verification: Boolean,
    val messages: Boolean
)
