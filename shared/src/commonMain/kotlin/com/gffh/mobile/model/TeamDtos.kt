package com.gffh.mobile.model

import kotlinx.serialization.Serializable

enum class AgeGroup { U7, U8, U9, U10, U11, U12, U13, U14, U15, U16, U17, U18, ADULT, VETERANS }
enum class Gender { BOYS, GIRLS, MIXED, MEN, WOMEN }
enum class Format { FIVE_A_SIDE, SEVEN_A_SIDE, NINE_A_SIDE, ELEVEN_A_SIDE }
enum class AbilityLevel { DEVELOPMENT, INTERMEDIATE, COMPETITIVE }
enum class HomeAwayPreference { HOME, AWAY, EITHER }

@Serializable
data class CreateTeamRequest(
    val clubId: String? = null,
    val clubName: String? = null,
    val name: String,
    val badgeUrl: String? = null,
    val ageGroup: String,
    val gender: String,
    val format: String? = null,
    val abilityLevel: String,
    val league: String? = null,
    val postcode: String,
    val longitude: Double,
    val latitude: Double,
    val travelRadiusMiles: Int,
    val homeAwayPreference: String,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val description: String? = null,
    val defaultVenueId: String? = null
)

@Serializable
data class UpdateTeamRequest(
    val name: String? = null,
    val badgeUrl: String? = null,
    val ageGroup: String? = null,
    val gender: String? = null,
    val format: String? = null,
    val abilityLevel: String? = null,
    val league: String? = null,
    val postcode: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val travelRadiusMiles: Int? = null,
    val homeAwayPreference: String? = null,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val description: String? = null,
    val defaultVenueId: String? = null
)

@Serializable
data class TeamView(
    val id: String,
    val clubId: String,
    val name: String,
    val clubName: String,
    val badgeUrl: String? = null,
    val ageGroup: String,
    val gender: String,
    val format: String,
    val abilityLevel: String,
    val league: String? = null,
    val postcode: String,
    val longitude: Double,
    val latitude: Double,
    val travelRadiusMiles: Int,
    val homeAwayPreference: String,
    val managerName: String? = null,
    val contactPhone: String? = null,
    val description: String? = null,
    val verification: String,
    val defaultVenueId: String? = null,
    val completenessPercent: Int,
    val createdAt: String
)
