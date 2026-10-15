package com.gffh.mobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The API leaves null fields out entirely, and an older API doesn't send the
 * newer ones at all - both must decode, with the same Json settings as ApiClient.
 */
class DtoDecodingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val requestWithoutNewFields = """
        {"id":"r1","senderTeamId":"t1","recipientTeamId":"t2","status":"SENT","date":"2026-11-01",
         "startTime":"10:00:00","endTime":"11:30:00","homeTeamId":"t1","awayTeamId":"t2",
         "costShare":"SPLIT","refereeArrangement":"HOME_PROVIDES","availableActions":["withdraw"]}
    """.trimIndent()

    @Test
    fun aRequestFromAnOlderApiStillDecodes() {
        val request = json.decodeFromString(FriendlyRequestView.serializer(), requestWithoutNewFields)

        assertNull(request.venue)
        assertNull(request.proposedVenue)
        assertNull(request.senderTeam)
        assertNull(request.recipientTeam)
    }

    @Test
    fun aRequestCarriesItsVenueTeamsAndContactNames() {
        val body = requestWithoutNewFields.removeSuffix("}") + """,
            "venue":{"id":"v1","name":"Rec Ground","postcode":"SW1A 1AA","latitude":51.5,"longitude":-0.12},
            "proposedVenue":{"id":"v2","name":"Astro Centre","pitchSurface":"ASTRO"},
            "senderTeam":{"id":"t1","name":"Denny Warriors U12","clubName":"Denny FC"},
            "recipientTeam":{"id":"t2","name":"Falkirk Juniors U12","badgeUrl":"https://example.com/b.png"},
            "senderContact":{"teamId":"t1","teamName":"Denny Warriors U12","managerName":"Sam"}}
        """.trimIndent()

        val request = json.decodeFromString(FriendlyRequestView.serializer(), body)

        assertEquals("Rec Ground", request.venue?.name)
        assertEquals(51.5, request.venue?.latitude)
        assertNull(request.venue?.address)
        assertEquals("ASTRO", request.proposedVenue?.pitchSurface)
        assertEquals("Denny FC", request.senderTeam?.clubName)
        assertEquals("https://example.com/b.png", request.recipientTeam?.badgeUrl)
        assertEquals("Denny Warriors U12", request.senderContact?.teamName)
    }

    @Test
    fun aFixtureCarriesItsVenueAndBadges() {
        val body = """
            {"id":"f1","friendlyRequestId":"r1","date":"2026-11-01","startTime":"10:00:00","endTime":"11:30:00",
             "status":"CONFIRMED","costShare":"SPLIT","refereeArrangement":"HOME_PROVIDES",
             "homeTeam":{"id":"t1","name":"Home","clubName":"Home FC","badgeUrl":"https://example.com/h.png"},
             "awayTeam":{"id":"t2","name":"Away","clubName":"Away FC"},
             "venue":{"id":"v1","name":"Rec Ground","accessNotes":"Gate code 1234","parkingNotes":"Car park behind"}}
        """.trimIndent()

        val fixture = json.decodeFromString(FixtureView.serializer(), body)

        assertEquals("https://example.com/h.png", fixture.homeTeam.badgeUrl)
        assertNull(fixture.awayTeam.badgeUrl)
        assertEquals("Gate code 1234", fixture.venue?.accessNotes)
        assertEquals("Car park behind", fixture.venue?.parkingNotes)
    }

    @Test
    fun preferencesWithoutDeliverySwitchesDefaultToOn() {
        val prefs = json.decodeFromString(
            NotificationPreferenceView.serializer(),
            """{"friendlyRequests":true,"fixtures":true,"verification":false,"messages":true}"""
        )

        assertTrue(prefs.emailEnabled)
        assertTrue(prefs.pushEnabled)
    }

    @Test
    fun preferencesReadTheDeliverySwitches() {
        val prefs = json.decodeFromString(
            NotificationPreferenceView.serializer(),
            """{"friendlyRequests":true,"fixtures":true,"verification":true,"messages":true,"emailEnabled":false,"pushEnabled":true}"""
        )

        assertFalse(prefs.emailEnabled)
        assertTrue(prefs.pushEnabled)
    }

    @Test
    fun anEmptyConfigMeansNoSupportAddressAndNoThirdPartySignIn() {
        val config = json.decodeFromString(ClientConfigView.serializer(), "{}")

        assertNull(config.supportEmail)
        assertFalse(config.google.enabled)
        assertFalse(config.apple.enabled)
    }

    @Test
    fun configReadsEveryField() {
        val config = json.decodeFromString(
            ClientConfigView.serializer(),
            """{"supportEmail":"help@example.com","google":{"enabled":true,"clientId":"123-abc.apps.googleusercontent.com"},
               "apple":{"enabled":true}}"""
        )

        assertEquals("help@example.com", config.supportEmail)
        assertEquals("123-abc.apps.googleusercontent.com", config.google.clientId)
        assertTrue(config.apple.enabled)
        assertNull(config.apple.servicesId)
    }

    @Test
    fun aChangedEmailComesBackUnverified() {
        val response = json.decodeFromString(
            ChangeEmailResponse.serializer(),
            """{"user":{"id":"u1","email":"new@example.com","displayName":"Sam","emailVerified":false}}"""
        )

        assertEquals("new@example.com", response.user.email)
        assertFalse(response.user.emailVerified)
        assertNull(response.verificationToken)
    }

    @Test
    fun aVenueReadsItsPostcodeAndStillDecodesWithoutOne() {
        val body = """
            {"id":"v1","clubId":"c1","name":"Rec Ground","address":"1 Park Road","postcode":"SW1A 1AA",
             "longitude":-0.12,"latitude":51.5,"facilities":[],"isDefault":true,"createdAt":"2026-10-01T09:00:00Z"}
        """.trimIndent()

        assertEquals("SW1A 1AA", json.decodeFromString(VenueView.serializer(), body).postcode)
        assertNull(json.decodeFromString(VenueView.serializer(), body.replace(""""postcode":"SW1A 1AA",""", "")).postcode)
    }

    @Test
    fun aNewVenueSendsItsPostcodeOnlyWhenThereIsOne() {
        val withPostcode = CreateVenueRequest(
            clubId = "c1", name = "Rec Ground", address = "1 Park Road", postcode = "SW1A 1AA",
            longitude = -0.12, latitude = 51.5
        )

        assertTrue(json.encodeToString(CreateVenueRequest.serializer(), withPostcode).contains(""""postcode":"SW1A 1AA""""))
        assertFalse(json.encodeToString(CreateVenueRequest.serializer(), withPostcode.copy(postcode = null)).contains("postcode"))
    }
}
