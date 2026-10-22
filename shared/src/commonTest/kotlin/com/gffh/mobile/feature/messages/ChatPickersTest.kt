package com.gffh.mobile.feature.messages

import com.gffh.mobile.model.GifView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatPickersTest {

    @Test
    fun everyEmojiTabHasEmojisAndNoDuplicates() {
        for ((label, emojis) in EMOJI_CATEGORIES) {
            assertTrue(emojis.isNotEmpty(), label)
            assertEquals(emojis.size, emojis.toSet().size, "duplicate in $label")
        }
    }

    @Test
    fun gifAspectRatioFallsBackToSquare() {
        assertEquals(2f, gifAspectRatio(GifView("a", "", "u", "p", 400, 200)))
        assertEquals(1f, gifAspectRatio(GifView("a", "", "u", "p", 0, 0)))
    }
}
