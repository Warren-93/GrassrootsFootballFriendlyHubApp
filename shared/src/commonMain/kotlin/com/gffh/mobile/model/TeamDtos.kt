package com.gffh.mobile.model

import kotlinx.serialization.Serializable

enum class AgeGroup { U7, U8, U9, U10, U11, U12, U13, U14, U15, U16, U17, U18, ADULT, VETERANS }
enum class Gender { BOYS, GIRLS, MIXED, MEN, WOMEN }
enum class Format { FIVE_A_SIDE, SEVEN_A_SIDE, NINE_A_SIDE, ELEVEN_A_SIDE }

/**
 * The division a team plays in within its league, shown as "Division". The
 * API property keeps the name `abilityLevel`. Every DTO carries it as a plain
 * String, so a value added after this build still decodes - see [fromCode].
 */
enum class AbilityLevel(val label: String) {
    PREMIER("Premier / top division"),
    DIVISION_1("Division 1 / Championship"),
    DIVISION_2("Division 2"),
    DIVISION_3_PLUS("Division 3 or lower"),
    NOT_IN_LEAGUE("Not in a league"),
    FESTIVAL("Festival / non-competitive");

    companion object {
        /** Null for a code this build doesn't know, rather than throwing. */
        fun fromCode(code: String?): AbilityLevel? = entries.firstOrNull { it.name == code }
    }
}

/**
 * The Scottish FA association a team plays under. Nullable on every DTO:
 * older teams have none. Carried as a String, like [AbilityLevel].
 */
enum class Organisation(val label: String) {
    SYFA("Scottish Youth FA (SYFA)"),
    SAFA("Scottish Amateur FA (SAFA)"),
    SWF("Scottish Women's Football (SWF)"),
    SWFA("Scottish Welfare FA (SWFA)"),
    SSFA("Scottish Schools' FA (SSFA)"),
    SJFA("Scottish Junior FA (SJFA)"),
    OTHER("Other / not affiliated");

    companion object {
        fun fromCode(code: String?): Organisation? = entries.firstOrNull { it.name == code }

        /**
         * Mirrors the API's Organisation.availableFor: youth, schools and girls'
         * bodies for youth teams; amateur, welfare, junior and women's for adults.
         * Scottish Women's Football is left out for boys' and men's teams.
         */
        fun availableFor(ageGroup: AgeGroup?, gender: Gender? = null): List<Organisation> {
            val byAge = when (ageGroup) {
                null -> entries.toList()
                AgeGroup.ADULT, AgeGroup.VETERANS -> listOf(SAFA, SWF, SWFA, SJFA, OTHER)
                else -> listOf(SYFA, SWF, SSFA, OTHER)
            }
            return if (gender == Gender.BOYS || gender == Gender.MEN) byAge - SWF else byAge
        }
    }
}
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
    /** An [Organisation] code. */
    val organisation: String? = null,
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
    /** An [Organisation] code; null leaves it unchanged. */
    val organisation: String? = null,
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
    /** An [Organisation] code. Null for teams created before it was asked for. */
    val organisation: String? = null,
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
    val createdAt: String,
    /** Only set on GET /teams/mine: false for a member who can't manage the team. */
    val canManage: Boolean? = null,
    /**
     * Only set on GET /teams/mine: true for the club's CLUB_ADMIN, false for
     * anyone else - a team manager can see the club's venues, but not add,
     * edit, make default or delete them. Null from an older API.
     */
    val canManageClub: Boolean? = null,
    /** Verified and has a venue: the only gate on sending and receiving friendly requests. */
    val readyForRequests: Boolean? = null,
    /** What's still to do. Empty for teams the caller doesn't manage. */
    val profileChecklist: List<ProfileChecklistItem> = emptyList()
)

@Serializable
data class ProfileChecklistItem(
    val key: String,
    val label: String,
    val done: Boolean,
    /** True for the items that gate friendly requests (verification, venue). */
    val requiredForRequests: Boolean
)

@Serializable
data class PrivacyPreferences(
    val searchVisible: Boolean,
    val shareContactDetails: Boolean
)
