package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AddMemberOutcome
import com.gffh.mobile.model.AddMemberRequest
import com.gffh.mobile.model.InviteView
import com.gffh.mobile.model.JoinCodeView
import com.gffh.mobile.model.JoinResultView
import com.gffh.mobile.model.MemberInvitedResponse
import com.gffh.mobile.model.MemberView
import com.gffh.mobile.model.RedeemJoinCodeRequest
import com.gffh.mobile.model.UpdateMemberRoleRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class MemberRepository(private val api: ApiClient) {

    suspend fun list(teamId: String): ApiResult<List<MemberView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/members")
    }

    /**
     * 200 with the new member when the email already has an account; 202 with
     * `{status: "INVITED", invite}` when it doesn't, and the API has emailed
     * them an invitation instead. Read as a raw object first since the two
     * bodies have different shapes.
     */
    suspend fun add(teamId: String, request: AddMemberRequest): ApiResult<AddMemberOutcome> {
        val result = api.request<JsonObject> {
            method = HttpMethod.Post
            url("/api/v1/teams/$teamId/members")
            setBody(request)
        }
        return when (result) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> parseAddMemberResponse(result.value)?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure("UNEXPECTED_RESPONSE", "Something went wrong. Please try again.", null, null)
        }
    }

    suspend fun updateRole(teamId: String, membershipId: String, request: UpdateMemberRoleRequest): ApiResult<MemberView> = api.request {
        method = HttpMethod.Patch
        url("/api/v1/teams/$teamId/members/$membershipId")
        setBody(request)
    }

    suspend fun remove(teamId: String, membershipId: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/teams/$teamId/members/$membershipId")
    }

    /** Pending, unexpired invitations for email addresses that don't have an account yet. */
    suspend fun listInvites(teamId: String): ApiResult<List<InviteView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/members/invites")
    }

    suspend fun cancelInvite(teamId: String, inviteId: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/teams/$teamId/members/invites/$inviteId")
    }

    suspend fun joinCode(teamId: String): ApiResult<JoinCodeView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/members/join-code")
    }

    suspend fun regenerateJoinCode(teamId: String): ApiResult<JoinCodeView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/members/join-code/regenerate")
    }

    suspend fun join(code: String): ApiResult<JoinResultView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/join")
        setBody(RedeemJoinCodeRequest(code))
    }
}

private val memberJson = Json { ignoreUnknownKeys = true }

/** Which of the two add-member bodies [body] is - null when it's neither. */
internal fun parseAddMemberResponse(body: JsonObject): AddMemberOutcome? = runCatching {
    if ((body["status"] as? JsonPrimitive)?.contentOrNull == "INVITED") {
        AddMemberOutcome.Invited(memberJson.decodeFromJsonElement(MemberInvitedResponse.serializer(), body).invite)
    } else {
        AddMemberOutcome.Added(memberJson.decodeFromJsonElement(MemberView.serializer(), body))
    }
}.getOrNull()
