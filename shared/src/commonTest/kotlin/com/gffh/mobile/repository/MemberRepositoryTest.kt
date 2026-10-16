package com.gffh.mobile.repository

import com.gffh.mobile.model.AddMemberOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Adding an official answers 200 with the member, or 202 with an invitation when there's no account yet. */
class MemberRepositoryTest {

    // Same settings as ApiClient's.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private fun obj(text: String): JsonObject = json.parseToJsonElement(text).jsonObject

    @Test
    fun a200BodyIsTheAddedMember() {
        val outcome = parseAddMemberResponse(
            obj(
                """{"membershipId":"m1","userId":"u1","email":"sam@example.com","displayName":"Sam",
                   "role":"TEAM_MANAGER","scope":"TEAM","joinedAt":"2026-10-15T12:00:00Z"}"""
            )
        )

        assertIs<AddMemberOutcome.Added>(outcome)
        assertEquals("Sam", outcome.member.displayName)
    }

    @Test
    fun a202BodyIsTheInvitation() {
        val outcome = parseAddMemberResponse(
            obj(
                """{"status":"INVITED","invite":{"id":"i1","email":"new@example.com","role":"CLUB_ADMIN",
                   "teamId":"t1","teamName":"Denny Warriors U12","createdAt":"2026-10-15T12:00:00Z",
                   "expiresAt":"2026-11-14T12:00:00Z"}}"""
            )
        )

        assertIs<AddMemberOutcome.Invited>(outcome)
        assertEquals("new@example.com", outcome.invite.email)
        assertEquals("CLUB_ADMIN", outcome.invite.role)
        assertEquals("2026-11-14T12:00:00Z", outcome.invite.expiresAt)
    }

    @Test
    fun aBodyThatIsNeitherIsRejected() {
        assertNull(parseAddMemberResponse(obj("""{"unexpected":true}""")))
    }
}
