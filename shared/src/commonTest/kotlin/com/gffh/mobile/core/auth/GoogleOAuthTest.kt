package com.gffh.mobile.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The plain-text half of Google sign-in on iOS: PKCE, the URLs, and reading Google's answers. */
class GoogleOAuthTest {

    private val clientId = "123-abc.apps.googleusercontent.com"
    private val request = GoogleOAuthRequest(clientId, codeVerifier = "0123456789abcdef".repeat(4), state = "s1")

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun sha256MatchesTheStandardTestVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex(Sha256.digest(ByteArray(0))))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex(Sha256.digest("abc".encodeToByteArray())))
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()))
        )
    }

    @Test
    fun sha256PadsCorrectlyAroundTheBlockBoundary() {
        // 55 bytes fit one block with the length; 56 need a second; 64 fill one exactly.
        assertEquals("9f4390f8d30c2dd92ec9f095b65e2b9ae9b0a925a5258e241c9f1e910f734318", hex(Sha256.digest(ByteArray(55) { 'a'.code.toByte() })))
        assertEquals("b35439a4ac6f0948b6d6f9e3c6af0f5f590ce20f1bde7090ef7970686ec6738a", hex(Sha256.digest(ByteArray(56) { 'a'.code.toByte() })))
        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", hex(Sha256.digest(ByteArray(64) { 'a'.code.toByte() })))
    }

    @Test
    fun theChallengeIsTheUnpaddedBase64UrlOfTheVerifiersHash() {
        assertEquals("qK5ubukpq-o6_PxSWMjM1vhSc-DUYm0mxyefMlD3fI4", GoogleOAuthRequest.codeChallenge(request.codeVerifier))
    }

    @Test
    fun theRedirectIsTheReversedClientIdScheme() {
        assertEquals("com.googleusercontent.apps.123-abc", request.callbackScheme)
        assertEquals("com.googleusercontent.apps.123-abc:/oauthredirect", request.redirectUri)
    }

    @Test
    fun theAuthorizeUrlAsksForAnIdTokenWithPkce() {
        val url = request.authorizeUrl
        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        assertTrue("client_id=123-abc.apps.googleusercontent.com" in url)
        assertTrue("redirect_uri=com.googleusercontent.apps.123-abc%3A%2Foauthredirect" in url)
        assertTrue("response_type=code" in url)
        assertTrue("scope=openid%20email%20profile" in url)
        assertTrue("code_challenge_method=S256" in url)
        assertTrue("state=s1" in url)
    }

    @Test
    fun theCallbackGivesTheCodeOnlyForOurOwnState() {
        assertEquals(
            GoogleOAuthCallback.Code("4/0Ab c"),
            request.parseCallback("com.googleusercontent.apps.123-abc:/oauthredirect?state=s1&code=4%2F0Ab+c&scope=email")
        )
        assertEquals(
            GoogleOAuthCallback.Failed,
            request.parseCallback("com.googleusercontent.apps.123-abc:/oauthredirect?state=other&code=x")
        )
        assertEquals(
            GoogleOAuthCallback.Cancelled,
            request.parseCallback("com.googleusercontent.apps.123-abc:/oauthredirect?error=access_denied&state=s1")
        )
        assertEquals(GoogleOAuthCallback.Failed, request.parseCallback("com.googleusercontent.apps.123-abc:/oauthredirect?state=s1"))
    }

    @Test
    fun theTokenRequestCarriesTheVerifierAndNoSecret() {
        val form = request.tokenRequest("c1").toMap()
        assertEquals("c1", form["code"])
        assertEquals("authorization_code", form["grant_type"])
        assertEquals(request.codeVerifier, form["code_verifier"])
        assertEquals(request.redirectUri, form["redirect_uri"])
        assertNull(form["client_secret"])
    }

    @Test
    fun theIdTokenIsReadFromGooglesAnswer() {
        assertEquals("eyJ.x.y", GoogleOAuthRequest.idTokenFrom("""{"access_token":"a","id_token":"eyJ.x.y","expires_in":3599}"""))
        assertNull(GoogleOAuthRequest.idTokenFrom("""{"error":"invalid_grant"}"""))
        assertNull(GoogleOAuthRequest.idTokenFrom("not json"))
    }
}
