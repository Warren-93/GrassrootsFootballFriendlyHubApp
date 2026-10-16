package com.gffh.mobile.model

import kotlinx.serialization.Serializable

enum class PitchSurface { GRASS, THREE_G, FOUR_G, ASTRO, INDOOR }
enum class VenueFacility { PARKING, CHANGING_ROOMS, TOILETS, REFRESHMENTS, FLOODLIGHTS, SPECTATOR_AREA }

@Serializable
data class CreateVenueRequest(
    val clubId: String,
    val name: String,
    val address: String,
    /** At most 10 characters. Left out, the API looks for one in [address] instead. */
    val postcode: String? = null,
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
    /** Null keeps the current postcode; "" clears it. */
    val postcode: String? = null,
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
    /** The postcode entered for the venue, else one the API found in [address]; null when neither. */
    val postcode: String? = null,
    val longitude: Double,
    val latitude: Double,
    val pitchSurface: String? = null,
    val facilities: List<String> = emptyList(),
    val accessNotes: String? = null,
    val parkingNotes: String? = null,
    val pitchNumber: String? = null,
    val isDefault: Boolean,
    val createdAt: String
) {
    /** What VenueCard shows: everything but the club-management fields. */
    fun toSummary(): VenueSummary = VenueSummary(
        id = id, name = name, address = address, postcode = postcode, pitchSurface = pitchSurface,
        accessNotes = accessNotes, parkingNotes = parkingNotes, latitude = latitude, longitude = longitude
    )
}

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
