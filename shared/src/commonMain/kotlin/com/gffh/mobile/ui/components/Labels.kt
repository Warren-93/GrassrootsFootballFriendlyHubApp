package com.gffh.mobile.ui.components

/** The codes that read badly with just their underscores taken out. */
private val KNOWN_LABELS = mapOf(
    "FIVE_A_SIDE" to "5v5",
    "SEVEN_A_SIDE" to "7v7",
    "NINE_A_SIDE" to "9v9",
    "ELEVEN_A_SIDE" to "11v11",
    "EITHER" to "No preference",
    // Verification: the team's status and a request's.
    "NOT_STARTED" to "Not verified",
    "PENDING" to "Under review",
    "AWAITING_SECOND_REJECTION" to "Under review",
    "USER" to "Member"
)

/**
 * The API's enum codes as people say them, for every screen that shows one:
 * "U12", "Boys", "7v7", "Intermediate", "No preference", "Changes requested".
 * Any other code - including one the API adds after this build - just loses
 * its underscores and capitals ("CLUB_SUPPLIED" reads "Club supplied"), so
 * nothing is ever shown raw. "NONE" depends on what it's the answer to: see
 * [refereeLabel] and [costShareLabel].
 */
fun codeLabel(code: String): String =
    KNOWN_LABELS[code] ?: code.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

fun refereeLabel(code: String): String = when (code) {
    "NONE" -> "No referee"
    "APPOINTED" -> "Appointed referee"
    else -> codeLabel(code)
}

fun costShareLabel(code: String): String = when (code) {
    "NONE" -> "No costs"
    else -> codeLabel(code)
}
