package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class StartConversationRequest(val teamId: String, val otherTeamId: String, val fixtureId: String? = null)

/** Text, a GIF (its [gifId] from a GIF search), or both. */
@Serializable
data class SendMessageRequest(val body: String, val gifId: String? = null)

/** A GIPHY GIF: [url] is 200px tall for the chat, [previewUrl] 100px for the picker grid. */
@Serializable
data class GifView(
    val id: String,
    val title: String = "",
    val url: String,
    val previewUrl: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class GifResults(val results: List<GifView> = emptyList())

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
    /** Empty for a GIF-only message. Blocked words arrive already masked (****) by the API. */
    val body: String,
    val createdAt: String,
    val gif: GifView? = null
)
