package com.kturker.multiplatform.core.update

import kotlinx.coroutines.delay

// Calls block up to attempts times, waiting delayMs between two calls, until it returns a value; null when
// every attempt returned null
internal suspend fun <T : Any> retryOnNull(
    attempts: Int,
    delayMs: Long,
    block: suspend () -> T?
): T? {
    repeat(attempts) { attempt ->
        if (attempt > 0) delay(delayMs)
        block()?.let { return it }
    }
    return null
}
