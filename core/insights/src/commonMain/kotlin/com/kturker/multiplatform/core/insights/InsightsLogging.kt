@file:Suppress("Filename")

package com.kturker.multiplatform.core.insights

import co.touchlab.kermit.Logger

// Called by initKoin once Koin is up, next to the writers configureLogging set (Logcat, OSLog)
fun installInsightsLogging(insights: Insights) = Logger.addLogWriter(InsightsLogWriter(insights))
