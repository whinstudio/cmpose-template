package com.kturker.multiplatform.core.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForcedUpdateSignalTest {

    @Test
    fun raiseSetsRequired() {
        val signal = ForcedUpdateSignal()
        assertFalse(signal.required.value)

        signal.raise()

        assertTrue(signal.required.value)
    }

    // Every request after the first 426 raises it again: the screen stays, nothing toggles back
    @Test
    fun raiseIsIdempotent() {
        val signal = ForcedUpdateSignal()

        signal.raise()
        signal.raise()

        assertTrue(signal.required.value)
    }
}
