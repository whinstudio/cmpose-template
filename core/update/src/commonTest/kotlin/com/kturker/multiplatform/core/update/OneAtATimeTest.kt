package com.kturker.multiplatform.core.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class OneAtATimeTest {

    private val gate = OneAtATime()

    // A second "Update" tap while the first one still runs does nothing
    @Test
    fun aSecondRunWhileTheFirstRunsIsIgnored() = runTest {
        val release = CompletableDeferred<Unit>()
        var runs = 0

        launch {
            gate.run {
                runs++
                release.await()
            }
        }
        runCurrent()
        gate.run { runs++ }
        release.complete(Unit)

        assertEquals(1, runs)
    }

    @Test
    fun runsAgainOnceTheFirstHasFinished() = runTest {
        var runs = 0

        gate.run { runs++ }
        gate.run { runs++ }

        assertEquals(2, runs)
    }

    @Test
    fun runsAgainAfterAFailure() = runTest {
        var runs = 0

        assertFailsWith<IllegalStateException> { gate.run { error("Play check failed") } }
        gate.run { runs++ }

        assertEquals(1, runs)
    }
}
