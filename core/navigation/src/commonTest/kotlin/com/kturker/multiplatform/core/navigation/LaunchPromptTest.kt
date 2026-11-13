package com.kturker.multiplatform.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// What opens at launch on its own waits while a launch prompt is pending; an app without any has nothing to wait for
class LaunchPromptTest {

    private class FakePrompt(pending: Boolean) : LaunchPrompt {
        override val pending = MutableStateFlow(pending)
    }

    @Test
    fun withoutPromptsNothingIsPending() = runTest {
        val prompts = emptyList<LaunchPrompt>()

        assertFalse(prompts.anyPending().first())
        assertFalse(prompts.anyPendingNow())
    }

    @Test
    fun pendingWhileAnyPromptIs() = runTest {
        val first = FakePrompt(pending = false)
        val second = FakePrompt(pending = true)
        val prompts = listOf(first, second)

        assertTrue(prompts.anyPending().first())
        assertTrue(prompts.anyPendingNow())

        second.pending.value = false

        assertFalse(prompts.anyPending().first())
        assertFalse(prompts.anyPendingNow())
    }
}
