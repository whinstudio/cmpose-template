package com.kturker.multiplatform.core.insights

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import co.touchlab.kermit.platformLogWriter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Without a Firebase app (here: not even an Android Context) insights are off, and saying so is not a warning:
// a warning would itself go to the insights writer
@OptIn(ExperimentalKermitApi::class)
class AndroidInsightsTest {

    private val console = TestLogWriter(loggable = Severity.Verbose)

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(console)
        Logger.setMinSeverity(Severity.Verbose)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun withoutFirebaseInsightsAreOffAndLogOnlyInfo() {
        startKoin {}

        val insights = platformInsights()

        assertSame(NoOpInsights, insights)
        assertEquals(listOf(Severity.Info), console.logs.map { it.severity })
        assertTrue(console.logs.single().message.startsWith("No Firebase config"))
    }

    @Test
    fun withoutKoinInsightsAreOff() {
        assertSame(NoOpInsights, platformInsights())
    }
}
