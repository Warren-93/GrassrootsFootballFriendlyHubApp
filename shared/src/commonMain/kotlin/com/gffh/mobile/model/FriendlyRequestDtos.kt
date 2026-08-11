package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SendFriendlyRequest(
    val senderTeamId: String,
    val recipientTeamId: String,
    val senderSlotId: String,
    val recipientSlotId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venueId: String? = null,
    val homeTeamId: String,
    val costShare: String,
    val refereeArrangement: String,
    val message: String? = null
)

@Serializable
data class TeamContact(
    val teamId: String,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val venueId: String? = null
)

@Serializable
data class FriendlyRequestView(
    val id: String,
    val senderTeamId: String,
    val recipientTeamId: String,
    val status: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venueId: String? = null,
    val homeTeamId: String,
    val awayTeamId: String,
    val costShare: String,
    val refereeArrangement: String,
    val message: String? = null,
    val availableActions: List<String>,
    val senderContact: TeamContact? = null,
    val recipientContact: TeamContact? = null
)
