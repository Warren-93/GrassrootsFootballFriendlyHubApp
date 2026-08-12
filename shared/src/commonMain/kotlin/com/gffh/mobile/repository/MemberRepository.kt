package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AddMemberRequest
import com.gffh.mobile.model.MemberView
import com.gffh.mobile.model.UpdateMemberRoleRequest
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class MemberRepository(private val api: ApiClient) {

    suspend fun list(teamId: String): ApiResult<List<MemberView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/teams/$teamId/members")
    }

    suspend fun add(teamId: String, request: AddMemberRequest): ApiResult<MemberView> = api.request {
        method = HttpMethod.Post
        url("/api/v1/teams/$teamId/members")
        setBody(request)
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
}
