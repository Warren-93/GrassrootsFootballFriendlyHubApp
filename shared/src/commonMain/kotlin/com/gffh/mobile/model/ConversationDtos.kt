package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class StartConversationRequest(val teamId: String, val otherTeamId: String)

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class ConversationView(
    val id: String,
    val otherTeam: TeamSummary,
    val lastMessageBody: String? = null,
    val lastMessageSenderTeamId: String? = null,
    val lastMessageAt: String? = null,
    val createdAt: String
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
