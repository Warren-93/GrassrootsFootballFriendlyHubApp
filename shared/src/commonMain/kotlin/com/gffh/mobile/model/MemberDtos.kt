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

/**
 * A pending invitation to join a team, for an email address with no account
 * yet. It becomes a real membership, with this [role], once that address
 * signs up and is verified.
 */
@Serializable
data class InviteView(
    val id: String,
    val email: String,
    val role: String = MemberRole.TEAM_MANAGER.name,
    val teamId: String? = null,
    val teamName: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null
)

/** The 202 body when the email added has no account yet: an invitation was emailed instead. */
@Serializable
data class MemberInvitedResponse(
    val status: String,
    val invite: InviteView
)

/** What adding an official by email did: added an existing account (201), or emailed an invitation (202). */
sealed class AddMemberOutcome {
    data class Added(val member: MemberView) : AddMemberOutcome()
    data class Invited(val invite: InviteView) : AddMemberOutcome()
}

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
