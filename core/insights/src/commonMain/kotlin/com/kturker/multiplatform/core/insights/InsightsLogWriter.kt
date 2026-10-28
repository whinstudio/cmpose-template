package com.kturker.multiplatform.core.insights

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Sends the app's warnings and errors to Crashlytics: each one as a breadcrumb, an error with a throwable also
 * as a non-fatal. Lower levels stay local. [Insights] must not log through Kermit, or this writer would call
 * itself again.
 */
class InsightsLogWriter(private val insights: Insights) : LogWriter() {

    override fun isLoggable(
        tag: String,
        severity: Severity
    ): Boolean = severity >= Severity.Warn && insights.isEnabled

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?
    ) {
        if (!isLoggable(tag = tag, severity = severity)) return
        val line = "[$tag] $message"
        insights.log(line)
        if (severity >= Severity.Error && throwable != null) insights.recordNonFatal(throwable, line)
    }
}
