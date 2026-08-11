package com.gffh.mobile.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * SCR-FF-02's filter set, held for the lifetime of the app process so SCR-FF-01
 * (implicit filter summary) and SCR-FF-03 (filter chip bar) can read the same
 * state Filters just wrote. Not persisted - filters reset defaults each app
 * session, matching the spec's "D: own age group" etc. defaults, which are
 * re-derived from the active team each time rather than remembered stale.
 */
class SearchFilterState {
    var maxDistanceMiles by mutableStateOf<Int?>(null)
    var formats by mutableStateOf<Set<String>>(emptySet())
    var abilityLevels by mutableStateOf<Set<String>>(emptySet())
    var verifiedOnly by mutableStateOf(false)
    var fromDate by mutableStateOf<String?>(null)
    var toDate by mutableStateOf<String?>(null)

    fun reset() {
        maxDistanceMiles = null
        formats = emptySet()
        abilityLevels = emptySet()
        verifiedOnly = false
        fromDate = null
        toDate = null
    }

    fun activeCount(): Int = listOfNotNull(
        maxDistanceMiles,
        formats.takeIf { it.isNotEmpty() },
        abilityLevels.takeIf { it.isNotEmpty() },
        verifiedOnly.takeIf { it }
    ).size
}
