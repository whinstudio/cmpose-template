package com.kturker.multiplatform.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/**
 * A dialog the app shows at launch on its own (core/insights: the analytics consent prompt). What opens at launch
 * too (the first-launch paywall, feature/monetization) waits while one is [pending], so the two never cover each
 * other. A module adds one with `@Single(binds = [LaunchPrompt::class])` and Koin collects them all (getAll): an app
 * without such a module has none, and nothing waits.
 */
interface LaunchPrompt {
    /** True while the prompt is due: neither answered nor dismissed in this run. */
    val pending: StateFlow<Boolean>
}

/** True while any of the prompts is pending; false at once when there are none. */
fun List<LaunchPrompt>.anyPending(): Flow<Boolean> =
    if (isEmpty()) flowOf(false) else combine(map { it.pending }) { states -> states.any { it } }

/** The same answer, now: the first value for a UI that collects [anyPending]. */
fun List<LaunchPrompt>.anyPendingNow(): Boolean = any { it.pending.value }
