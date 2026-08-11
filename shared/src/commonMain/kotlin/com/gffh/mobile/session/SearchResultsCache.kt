package com.gffh.mobile.session

import com.gffh.mobile.model.MatchSummary
import com.gffh.mobile.model.SearchResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the most recent opponent search so SCR-FF-05 (opponent profile) and
 * SCR-FF-06 (match explanation) can read a candidate's data without a second
 * network call - the spec says as much explicitly for FF-06 ("included in the
 * matches/search response; no separate call") and the same reasoning applies
 * to FF-05 here, since the backend's `GET /api/v1/teams/{id}` requires
 * managing the team, which a searching manager never does for an opponent.
 *
 * Deliberately in-memory only, not persisted: search results are a session
 * concern, not account state, and go stale the moment availability changes
 * elsewhere.
 */
class SearchResultsCache {
    private val _lastResponse = MutableStateFlow<SearchResponse?>(null)
    val lastResponse: StateFlow<SearchResponse?> = _lastResponse

    fun store(response: SearchResponse) {
        _lastResponse.value = response
    }

    fun find(teamId: String): MatchSummary? = _lastResponse.value?.results?.firstOrNull { it.team.id == teamId }
}
