package com.gffh.mobile.model

import kotlinx.serialization.Serializable

@Serializable
data class MembershipExport(
    val membershipId: String,
    val role: String,
    val scope: String,
    val teamId: String? = null,
    val teamName: String? = null,
    val clubId: String? = null,
    val clubName: String? = null,
    val joinedAt: String
)

@Serializable
data class AccountExport(
    val userId: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean,
    val createdAt: String,
    val memberships: List<MembershipExport> = emptyList()
)
