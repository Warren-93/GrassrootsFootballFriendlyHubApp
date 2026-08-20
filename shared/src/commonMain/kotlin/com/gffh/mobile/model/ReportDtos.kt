package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class SubmitReportRequest(
    val reportedTeamId: String,
    val relatedFixtureId: String? = null,
    val type: String,
    val severity: String,
    val details: String
)

@Serializable
data class BlockRequest(
    val blockedTeamId: String,
    val reason: String? = null
)

@Serializable
data class BlockView(
    val id: String,
    val blockedTeamId: String,
    val blockedTeamName: String,
    val reason: String? = null,
    val createdAt: String
)
