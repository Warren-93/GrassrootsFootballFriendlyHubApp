package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(val body: String)

@Serializable
data class MessageView(
    val id: String,
    val fixtureId: String,
    val senderTeamId: String,
    val senderUserId: String,
    val body: String,
    val createdAt: String
)
