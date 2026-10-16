package com.gffh.mobile.core.image

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Only the cases that must not reach a decoder: a badge that isn't an
 * inlined image falls back to the team's initials rather than failing. The
 * decode itself needs a real graphics stack, so it belongs on a device.
 */
class BadgeImageTest {

    @Test
    fun noBadgeDecodesToNothing() {
        assertNull(decodeBadge(null))
        assertNull(decodeBadge(""))
        assertNull(decodeBadge("   "))
    }

    @Test
    fun anHttpsBadgeIsNotFetched() {
        assertNull(decodeBadge("https://example.com/badge.png"))
    }

    @Test
    fun aDataUrlWithNoPayloadDecodesToNothing() {
        assertNull(decodeBadge("data:image/jpeg;base64,"))
    }
}
