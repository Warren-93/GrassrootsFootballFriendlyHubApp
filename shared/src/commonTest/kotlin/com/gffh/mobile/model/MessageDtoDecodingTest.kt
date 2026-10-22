package com.gffh.mobile.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Chat messages and the client config, from an API with and without GIFs - same Json settings as ApiClient. */
class MessageDtoDecodingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun aTextMessageHasNoGif() {
        val m = json.decodeFromString(MessageView.serializer(), """
            {"id":"m1","conversationId":"c1","senderTeamId":"t1","senderUserId":"u1","body":"Hi ****","createdAt":"2026-10-22T10:00:00Z"}
        """.trimIndent())
        assertEquals("Hi ****", m.body)
        assertNull(m.gif)
    }

    @Test
    fun aGifMessageCarriesTheGif() {
        val m = json.decodeFromString(MessageView.serializer(), """
            {"id":"m1","conversationId":"c1","senderTeamId":"t1","senderUserId":"u1","body":"","createdAt":"2026-10-22T10:00:00Z",
             "gif":{"id":"abc","title":"Goal","url":"https://media.giphy.com/a/200.gif","previewUrl":"https://media.giphy.com/a/100.gif","width":356,"height":200}}
        """.trimIndent())
        assertEquals("https://media.giphy.com/a/200.gif", m.gif?.url)
        assertEquals(356, m.gif?.width)
    }

    @Test
    fun gifsAreOffUnlessTheApiSaysSo() {
        assertFalse(json.decodeFromString(ClientConfigView.serializer(), "{}").gifsEnabled)
        assertTrue(json.decodeFromString(ClientConfigView.serializer(), """{"gifsEnabled":true}""").gifsEnabled)
    }

    @Test
    fun aTextOnlySendLeavesTheGifIdOut() {
        assertEquals("""{"body":"Hi"}""", json.encodeToString(SendMessageRequest.serializer(), SendMessageRequest("Hi")))
    }
}
