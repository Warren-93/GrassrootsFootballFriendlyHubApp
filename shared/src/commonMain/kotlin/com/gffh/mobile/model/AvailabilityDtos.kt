package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateSlotRequest(
    val date: String,
    val startTime: String,
    val endTime: String,
    val homeAwayPreference: String,
    val venueId: String? = null,
    val format: String? = null,
    val notes: String? = null
)

@Serializable
data class SlotView(
    val id: String,
    val teamId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val homeAwayPreference: String,
    val venueId: String? = null,
    val format: String? = null,
    val notes: String? = null,
    val status: String
)
