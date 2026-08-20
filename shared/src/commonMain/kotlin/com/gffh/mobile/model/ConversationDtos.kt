package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class StartConversationRequest(val teamId: String, val otherTeamId: String, val fixtureId: String? = null)

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class ConversationView(
    val id: String,
    val otherTeam: TeamSummary,
    val lastMessageBody: String? = null,
    val lastMessageSenderTeamId: String? = null,
    val lastMessageAt: String? = null,
    val createdAt: String,
    /** What this thread is currently about - set when opened from a fixture's "Message" entry point. */
    val relatedFixtureId: String? = null
)

@Serializable
data class MessageView(
    val id: String,
    val conversationId: String,
    val senderTeamId: String,
    val senderUserId: String,
    val body: String,
    val createdAt: String
)
