package com.gffh.mobile.model

/** Converts the API's ISO date to a concise day/month/year date for match cards. */
fun formatMatchDate(date: String): String {
    val parts = date.split('-')
    if (parts.size != 3) return date
    val year = parts[0].toIntOrNull() ?: return date
    val month = parts[1].toIntOrNull() ?: return date
    val day = parts[2].toIntOrNull() ?: return date
    if (month !in 1..12 || day !in 1..31) return date
    return "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"
}

