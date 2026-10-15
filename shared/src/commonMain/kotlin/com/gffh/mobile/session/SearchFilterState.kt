package com.gffh.mobile.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gffh.mobile.model.SearchRequest

/**
 * Optional filters (SCR-FF-02) over the default search, which already shows
 * every team with the same age group and gender. Empty means "all". Held for
 * the app process so Filters and Results read the same state; Discover
 * resets it so a new search always opens on the full list.
 */
class SearchFilterState {
    var maxDistanceMiles by mutableStateOf<Int?>(null)
    var formats by mutableStateOf<Set<String>>(emptySet())
    var abilityLevels by mutableStateOf<Set<String>>(emptySet())
    var verifiedOnly by mutableStateOf(false)

    fun reset() {
        maxDistanceMiles = null
        formats = emptySet()
        abilityLevels = emptySet()
        verifiedOnly = false
    }

    fun activeCount(): Int = listOfNotNull(
        maxDistanceMiles,
        formats.takeIf { it.isNotEmpty() },
        abilityLevels.takeIf { it.isNotEmpty() },
        verifiedOnly.takeIf { it }
    ).size

    fun toRequest(teamId: String, limit: Int): SearchRequest = SearchRequest(
        teamId = teamId,
        formats = formats.toList().ifEmpty { null },
        abilityLevels = abilityLevels.toList().ifEmpty { null },
        maxDistanceMiles = maxDistanceMiles,
        verifiedOnly = verifiedOnly.takeIf { it },
        limit = limit
    )
}
