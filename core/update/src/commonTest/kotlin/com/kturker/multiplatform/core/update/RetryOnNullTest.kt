package com.kturker.multiplatform.core.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val DELAY = 1_000L

@OptIn(ExperimentalCoroutinesApi::class)
class RetryOnNullTest {

    @Test
    fun returnsTheFirstNonNull() = runTest {
        var calls = 0

        val result = retryOnNull(attempts = 3, delayMs = DELAY) {
            calls++
            if (calls == 2) "found" else null
        }

        assertEquals("found", result)
        assertEquals(2, calls)
    }

    @Test
    fun aFirstAnswerDoesNotWait() = runTest {
        val result = retryOnNull(attempts = 3, delayMs = DELAY) { "found" }

        assertEquals("found", result)
        assertEquals(0L, currentTime)
    }

    @Test
    fun stopsAfterTheLastAttempt() = runTest {
        var calls = 0

        val result = retryOnNull<String>(attempts = 3, delayMs = DELAY) {
            calls++
            null
        }

        assertNull(result)
        assertEquals(3, calls)
    }

    // A wait between two attempts, none after the last one
    @Test
    fun waitsBetweenAttempts() = runTest {
        val startedAt = mutableListOf<Long>()

        retryOnNull<String>(attempts = 3, delayMs = DELAY) {
            startedAt += currentTime
            null
        }

        assertEquals(listOf(0L, DELAY, 2 * DELAY), startedAt)
        assertEquals(2 * DELAY, currentTime)
    }

    @Test
    fun zeroAttemptsNeverCalls() = runTest {
        var calls = 0

        val result = retryOnNull<String>(attempts = 0, delayMs = DELAY) {
            calls++
            "found"
        }

        assertNull(result)
        assertEquals(0, calls)
    }
}
