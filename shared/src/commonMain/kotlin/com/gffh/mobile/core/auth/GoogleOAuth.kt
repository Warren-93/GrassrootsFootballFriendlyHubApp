package com.gffh.mobile.core.auth

import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * Google's OAuth flow for an installed app (the authorization code flow with
 * PKCE, RFC 7636), as the iOS app runs it: Google has no Credential Manager
 * there, so the person signs in on Google's own page in a system browser
 * sheet, which hands a code back to [redirectUri], and the app trades that
 * code (plus [codeVerifier]) for an ID token. The platform does the sheet and
 * the HTTP; everything here is plain text, so it's tested on every platform.
 *
 * [clientId] is the Google OAuth *iOS* client ("…apps.googleusercontent.com"),
 * which is also the ID token's audience - the API accepts it once it's in
 * GOOGLE_ADDITIONAL_CLIENT_IDS. iOS clients have no secret: PKCE stands in.
 */
class GoogleOAuthRequest(val clientId: String, val codeVerifier: String, val state: String) {

    /** Google's iOS redirect: the client ID reversed, as a custom URL scheme. */
    val callbackScheme: String = reversedClientId(clientId)

    val redirectUri: String = "$callbackScheme:/oauthredirect"

    val authorizeUrl: String
        get() = AUTHORIZE_ENDPOINT + "?" + listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to "openid email profile",
            "code_challenge" to codeChallenge(codeVerifier),
            "code_challenge_method" to "S256",
            "state" to state,
            // Always show the account list, as Android's button flow does.
            "prompt" to "select_account"
        ).joinToString("&") { (key, value) -> "$key=${value.encodeURLParameter()}" }

    /** The form body for [TOKEN_ENDPOINT] that trades [code] for tokens. */
    fun tokenRequest(code: String): List<Pair<String, String>> = listOf(
        "code" to code,
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
        "grant_type" to "authorization_code",
        "code_verifier" to codeVerifier
    )

    /**
     * What Google's redirect to [redirectUri] says: the code, or that the
     * person backed out. A redirect with someone else's [state] is refused.
     */
    fun parseCallback(callbackUrl: String): GoogleOAuthCallback {
        val query = callbackUrl.substringAfter('?', "").substringBefore('#')
        val params = query.split('&').filter { it.isNotEmpty() }.associate { part ->
            part.substringBefore('=').decodeURLQueryComponent() to
                part.substringAfter('=', "").decodeURLQueryComponent(plusIsSpace = true)
        }
        return when {
            params["state"] != state -> GoogleOAuthCallback.Failed
            params["error"] == "access_denied" -> GoogleOAuthCallback.Cancelled
            !params["code"].isNullOrBlank() -> GoogleOAuthCallback.Code(params.getValue("code"))
            else -> GoogleOAuthCallback.Failed
        }
    }

    companion object {
        const val AUTHORIZE_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        private val json = Json { ignoreUnknownKeys = true }

        /** The ID token from [TOKEN_ENDPOINT]'s answer, or null when there's none (or it isn't JSON). */
        fun idTokenFrom(tokenResponseBody: String): String? =
            runCatching { json.decodeFromString<GoogleTokenResponse>(tokenResponseBody).idToken }
                .getOrNull()?.takeIf { it.isNotBlank() }

        /** "123-abc.apps.googleusercontent.com" -> "com.googleusercontent.apps.123-abc". */
        fun reversedClientId(clientId: String): String = clientId.split('.').reversed().joinToString(".")

        /** RFC 7636's S256: BASE64URL(SHA-256(verifier)), without padding. */
        fun codeChallenge(codeVerifier: String): String =
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(Sha256.digest(codeVerifier.encodeToByteArray()))
    }
}

@Serializable
private class GoogleTokenResponse(@SerialName("id_token") val idToken: String? = null)

sealed class GoogleOAuthCallback {
    data class Code(val code: String) : GoogleOAuthCallback()
    data object Cancelled : GoogleOAuthCallback()
    data object Failed : GoogleOAuthCallback()
}

/**
 * SHA-256 (FIPS 180-4), only for [GoogleOAuthRequest.codeChallenge]: the
 * standard library has no digests, and it's a few short strings, so a plain
 * implementation beats pulling in a crypto library or native interop.
 */
internal object Sha256 {

    private val K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e
    )

    fun digest(message: ByteArray): ByteArray {
        val h = intArrayOf(
            0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6, 0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19
        )
        // Padding: a 1 bit, zeros, then the message length in bits, to a multiple of 64 bytes.
        val bitLength = message.size.toLong() * 8
        val paddedSize = ((message.size + 8) / 64 + 1) * 64
        val padded = message.copyOf(paddedSize)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) padded[paddedSize - 1 - i] = (bitLength ushr (8 * i)).toByte()

        val w = IntArray(64)
        for (chunk in 0 until paddedSize / 64) {
            for (t in 0 until 16) {
                val o = chunk * 64 + t * 4
                w[t] = ((padded[o].toInt() and 0xff) shl 24) or
                    ((padded[o + 1].toInt() and 0xff) shl 16) or
                    ((padded[o + 2].toInt() and 0xff) shl 8) or
                    (padded[o + 3].toInt() and 0xff)
            }
            for (t in 16 until 64) {
                val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
                val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (t in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = hh + s1 + ch + K[t] + w[t]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj
                hh = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d
            h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        }
        return ByteArray(32) { i -> (h[i / 4] ushr (24 - 8 * (i % 4))).toByte() }
    }
}
