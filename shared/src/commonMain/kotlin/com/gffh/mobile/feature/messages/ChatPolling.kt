package com.gffh.mobile.feature.messages

import com.gffh.mobile.model.MessageView
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** How often an open chat checks for new messages while it's lively. */
const val ACTIVE_POLL_MS = 4_000L
/** How often it checks once nothing has arrived for [IDLE_AFTER_MS]. */
const val IDLE_POLL_MS = 15_000L
/** Quiet time after which polling slows to [IDLE_POLL_MS]. */
const val IDLE_AFTER_MS = 60_000L

/**
 * Adds newly fetched messages to those already shown. A poll asks only for
 * messages since the newest one held, and the API includes that one too, so
 * duplicates are dropped by id and the result stays oldest first. Returns
 * [current] itself when nothing is new.
 */
fun mergeMessages(current: List<MessageView>, incoming: List<MessageView>): List<MessageView> {
    val known = current.mapTo(HashSet()) { it.id }
    val added = incoming.filter { it.id !in known }
    if (added.isEmpty()) return current
    // Compared as times: the API's timestamps vary in how many decimal places they carry.
    return (current + added).sortedBy { epochMillis(it.createdAt) }
}

/** The time to poll from: the newest message held, or null to fetch the whole history. */
fun pollSince(messages: List<MessageView>): String? = messages.lastOrNull()?.createdAt

/** Polls quickly while messages are arriving, and slows down once the chat goes quiet. */
fun nextPollDelayMs(quietForMs: Long): Long = if (quietForMs < IDLE_AFTER_MS) ACTIVE_POLL_MS else IDLE_POLL_MS

@OptIn(ExperimentalTime::class)
private fun epochMillis(iso: String): Long =
    runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrDefault(Long.MAX_VALUE)
