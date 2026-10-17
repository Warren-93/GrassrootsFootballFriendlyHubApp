package com.gffh.mobile.repository

import com.gffh.mobile.core.network.httpClientEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * SCR-FF-04's results map needs approximate opponent positions, but the
 * backend deliberately never returns exact team coordinates in search
 * results (see MatchDtos.TeamSummary's javadoc: "no exact location - only a
 * general area and a distance"). This geocodes only the outward postcode
 * (e.g. "SW1A") already disclosed as text, via the free, keyless
 * postcodes.io API, so a pin never reveals more than the team card already
 * does.
 */
class GeocodeRepository {

    private val client = HttpClient(httpClientEngineFactory()) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    private val cache = mutableMapOf<String, GeoPoint?>()

    suspend fun geocodeOutcode(outcode: String): GeoPoint? {
        val key = outcode.trim().uppercase()
        cache[key]?.let { return it }
        if (cache.containsKey(key)) return null

        return lookUp("https://api.postcodes.io/outcodes/$key", key)
    }

    /**
     * Full-postcode precision, for real location entry (create club/team,
     * add venue) - distinct from geocodeOutcode's deliberately coarser
     * outward-code-only resolution used for the privacy-preserving map.
     */
    suspend fun geocodePostcode(postcode: String): GeoPoint? {
        val trimmed = postcode.trim()
        if (trimmed.isEmpty()) return null
        val key = "full:${trimmed.uppercase()}"
        cache[key]?.let { return it }
        if (cache.containsKey(key)) return null

        return lookUp("https://api.postcodes.io/postcodes/$trimmed", key)
    }

    /**
     * Only a found point or postcodes.io's own 404 is cached: a dropped
     * connection or a server error is tried again next time, rather than
     * leaving that postcode without a pin for the rest of the session.
     */
    private suspend fun lookUp(url: String, key: String): GeoPoint? = try {
        val response = client.get(url)
        when {
            response.status.isSuccess() -> {
                val point = response.body<OutcodeResponse>().result?.let { GeoPoint(it.latitude, it.longitude) }
                if (point != null) cache[key] = point
                point
            }
            response.status == HttpStatusCode.NotFound -> {
                cache[key] = null
                null
            }
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

data class GeoPoint(val latitude: Double, val longitude: Double)

@Serializable
private data class OutcodeResponse(val result: OutcodeResult? = null)

@Serializable
private data class OutcodeResult(val latitude: Double, val longitude: Double)
