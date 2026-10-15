package com.gffh.mobile.model

import kotlinx.serialization.Serializable

enum class PitchSurface { GRASS, THREE_G, FOUR_G, ASTRO, INDOOR }
enum class VenueFacility { PARKING, CHANGING_ROOMS, TOILETS, REFRESHMENTS, FLOODLIGHTS, SPECTATOR_AREA }

@Serializable
data class CreateVenueRequest(
    val clubId: String,
    val name: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    val pitchSurface: String? = null,
    val facilities: List<String> = emptyList(),
    val accessNotes: String? = null,
    val parkingNotes: String? = null,
    val pitchNumber: String? = null
)

@Serializable
data class UpdateVenueRequest(
    val name: String? = null,
    val address: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val pitchSurface: String? = null,
    val facilities: List<String>? = null,
    val accessNotes: String? = null,
    val parkingNotes: String? = null,
    val pitchNumber: String? = null
)

@Serializable
data class VenueView(
    val id: String,
    val clubId: String,
    val name: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    val pitchSurface: String? = null,
    val facilities: List<String> = emptyList(),
    val accessNotes: String? = null,
    val parkingNotes: String? = null,
    val pitchNumber: String? = null,
    val isDefault: Boolean,
    val createdAt: String
)

/**
 * Where a friendly request or fixture is being played, as the API attaches it
 * to FriendlyRequestView and FixtureView: enough to show the venue and link
 * directions, without the club-management fields. Only [id] and [name] are
 * always set.
 */
@Serializable
data class VenueSummary(
    val id: String,
    val name: String,
    val address: String? = null,
    val postcode: String? = null,
    val pitchSurface: String? = null,
    val accessNotes: String? = null,
    val parkingNotes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
