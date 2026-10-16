package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class CancelFixtureRequest(val reason: String? = null)

@Serializable
data class FixtureTeamView(
    val id: String,
    val name: String,
    val clubName: String,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val venueId: String? = null,
    val badgeUrl: String? = null
)

@Serializable
data class FixtureView(
    val id: String,
    val friendlyRequestId: String,
    val homeTeam: FixtureTeamView,
    val awayTeam: FixtureTeamView,
    val date: String,
    val startTime: String,
    val endTime: String,
    val venueId: String? = null,
    val status: String,
    val costShare: String,
    val refereeArrangement: String,
    /** The fixture's venue, else the home team's default venue - null when neither is set. */
    val venue: VenueSummary? = null
)
