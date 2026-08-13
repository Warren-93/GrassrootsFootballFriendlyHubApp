package com.gffh.mobile.model

import kotlinx.serialization.Serializable

enum class MemberRole { USER, TEAM_MANAGER, CLUB_ADMIN }

@Serializable
data class AddMemberRequest(val email: String, val role: String)

@Serializable
data class UpdateMemberRoleRequest(val role: String)

@Serializable
data class MemberView(
    val membershipId: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
    val scope: String,
    val joinedAt: String
)

@Serializable
data class JoinCodeView(val code: String)

@Serializable
data class RedeemJoinCodeRequest(val code: String)

@Serializable
data class JoinResultView(
    val teamId: String,
    val teamName: String,
    val clubId: String,
    val membership: MemberView
)
