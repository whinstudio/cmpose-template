package com.kturker.multiplatform.feature.sharing.presentation.ui.members

private const val DAY_MILLIS = 86_400_000L

/**
 * The whole days a pending invite link has left at [now] (both epoch milliseconds): any part of a day counts as a
 * day, and a link still listed never shows fewer than 1.
 */
internal fun daysLeft(
    expiresAt: Long,
    now: Long
): Int = ((expiresAt - now + DAY_MILLIS - 1) / DAY_MILLIS).toInt().coerceAtLeast(1)
