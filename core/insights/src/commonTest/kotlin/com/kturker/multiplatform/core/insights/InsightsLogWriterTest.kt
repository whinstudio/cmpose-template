package com.kturker.multiplatform.core.insights

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import co.touchlab.kermit.platformLogWriter
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Warnings and errors reach Crashlytics as breadcrumbs, errors with a throwable as non-fatals; nothing else does
@OptIn(ExperimentalKermitApi::class)
class InsightsLogWriterTest {

    private val insights = RecordingInsights()
    private val writer = InsightsLogWriter(insights)

    @Test
    fun warnIsABreadcrumb() {
        writer.log(Severity.Warn, "slow", "Net", null)

        assertEquals(listOf("[Net] slow"), insights.logs)
        assertTrue(insights.nonFatals.isEmpty())
    }

    @Test
    fun errorWithThrowableIsANonFatal() {
        val ex = IllegalStateException("x")

        writer.log(Severity.Error, "boom", "Net", ex)

        assertEquals(listOf("[Net] boom"), insights.logs)
        assertEquals(listOf<Pair<Throwable, String?>>(ex to "[Net] boom"), insights.nonFatals)
    }

    @Test
    fun errorWithoutThrowableIsOnlyABreadcrumb() {
        writer.log(Severity.Error, "boom", "Net", null)

        assertEquals(listOf("[Net] boom"), insights.logs)
        assertTrue(insights.nonFatals.isEmpty())
    }

    @Test
    fun belowWarnIsIgnored() {
        writer.log(Severity.Info, "started", "Net", null)
        writer.log(Severity.Debug, "request", "Net", IllegalStateException("x"))

        assertTrue(insights.logs.isEmpty())
        assertTrue(insights.nonFatals.isEmpty())
        assertFalse(writer.isLoggable("Net", Severity.Info))
        assertTrue(writer.isLoggable("Net", Severity.Warn))
    }

    @Test
    fun disabledInsightsRecordNothing() {
        val disabled = RecordingInsights(isEnabled = false)
        val writer = InsightsLogWriter(disabled)

        writer.log(Severity.Warn, "slow", "Net", null)
        writer.log(Severity.Error, "boom", "Net", IllegalStateException("x"))

        assertTrue(disabled.logs.isEmpty())
        assertTrue(disabled.nonFatals.isEmpty())
        assertFalse(writer.isLoggable("Net", Severity.Error))
    }

    // Installed next to the app's writer, one warning is one breadcrumb and one line: the writer does not log
    // through Kermit itself, so it cannot call itself again
    @Test
    fun anInstalledWriterDoesNotRecurse() {
        val console = TestLogWriter(loggable = Severity.Verbose)
        Logger.setLogWriters(console)
        Logger.setMinSeverity(Severity.Verbose)

        installInsightsLogging(insights)
        Logger.withTag("Net").w { "slow" }

        assertEquals(listOf("[Net] slow"), insights.logs)
        assertEquals(1, console.logs.size)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun noOpInsightsIsDisabled() {
        assertFalse(NoOpInsights.isEnabled)

        NoOpInsights.logEvent("screen_view", mapOf("screen" to "home"))
        NoOpInsights.setUserId("u1")
        NoOpInsights.setUserId(null)
        NoOpInsights.recordNonFatal(IllegalStateException("x"), "boom")
        NoOpInsights.log("breadcrumb")
        NoOpInsights.setCollectionEnabled(false)
    }
}

private class RecordingInsights(override val isEnabled: Boolean = true) : Insights {
    val logs = mutableListOf<String>()
    val nonFatals = mutableListOf<Pair<Throwable, String?>>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>
    ) = Unit

    override fun setUserId(id: String?) = Unit

    override fun recordNonFatal(
        throwable: Throwable,
        message: String?
    ) {
        nonFatals += throwable to message
    }

    override fun log(message: String) {
        logs += message
    }

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
