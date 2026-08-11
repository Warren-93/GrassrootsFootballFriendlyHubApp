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
