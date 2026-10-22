package com.gffh.mobile.feature.messages

import com.gffh.mobile.model.MessageView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ChatPollingTest {

    private fun message(id: String, createdAt: String) = MessageView(id, "c1", "t1", "u1", id, createdAt)

    @Test
    fun addsNewMessagesAndDropsTheRepeatedNewestOne() {
        val current = listOf(message("a", "2026-10-22T10:00:00Z"), message("b", "2026-10-22T10:00:01.5Z"))
        val incoming = listOf(message("b", "2026-10-22T10:00:01.5Z"), message("c", "2026-10-22T10:00:02Z"))
        assertEquals(listOf("a", "b", "c"), mergeMessages(current, incoming).map { it.id })
    }

    @Test
    fun keepsTheSameListWhenNothingIsNew() {
        val current = listOf(message("a", "2026-10-22T10:00:00Z"))
        assertSame(current, mergeMessages(current, listOf(message("a", "2026-10-22T10:00:00Z"))))
        assertSame(current, mergeMessages(current, emptyList()))
    }

    @Test
    fun ordersByTimeWhateverTheDecimalPlaces() {
        val current = listOf(message("a", "2026-10-22T10:00:00.9Z"))
        val merged = mergeMessages(current, listOf(message("b", "2026-10-22T10:00:01Z"), message("c", "2026-10-22T10:00:00.95Z")))
        assertEquals(listOf("a", "c", "b"), merged.map { it.id })
    }

    @Test
    fun pollsFromTheNewestMessage() {
        assertNull(pollSince(emptyList()))
        assertEquals("y", pollSince(listOf(message("a", "x"), message("b", "y"))))
    }

    @Test
    fun slowsDownOnceTheChatIsQuiet() {
        assertEquals(ACTIVE_POLL_MS, nextPollDelayMs(IDLE_AFTER_MS - 1))
        assertEquals(IDLE_POLL_MS, nextPollDelayMs(IDLE_AFTER_MS))
    }
}
