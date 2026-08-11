package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val teamId: String,
    val fromDate: String? = null,
    val toDate: String? = null,
    val formats: List<String>? = null,
    val abilityLevels: List<String>? = null,
    val maxDistanceMiles: Int? = null,
    val verifiedOnly: Boolean? = null,
    val venueRequired: Boolean? = null,
    val limit: Int? = null,
    val cursor: String? = null
)

@Serializable
data class SearchResponse(
    val searchId: String,
    val totalResults: Int,
    val results: List<MatchSummary>,
    val weights: WeightsView,
    val nextCursor: String? = null
)

@Serializable
data class MatchSummary(
    val team: TeamSummary,
    val score: Int,
    val band: String,
    val milesApart: Double,
    val reasons: List<String>,
    val factors: List<FactorView>,
    val earliestOverlap: OverlapView? = null
)

@Serializable
data class TeamSummary(
    val id: String,
    val name: String,
    val clubName: String,
    val badgeUrl: String? = null,
    val ageGroup: String,
    val gender: String,
    val format: String,
    val abilityLevel: String,
    val league: String? = null,
    val description: String? = null,
    val generalArea: String? = null,
    val verified: Boolean
)

@Serializable
data class FactorView(
    val factor: String,
    val label: String,
    val weight: Int,
    val ratio: Double,
    val points: Double,
    val ourValue: String,
    val theirValue: String,
    val reason: String
)

@Serializable
data class OverlapView(
    val date: String,
    val startTime: String,
    val overlapMinutes: Int,
    val ourSlotId: String,
    val theirSlotId: String
)

@Serializable
data class WeightsView(
    val age: Int,
    val availability: Int,
    val format: Int,
    val distance: Int,
    val homeAway: Int,
    val ability: Int
)
