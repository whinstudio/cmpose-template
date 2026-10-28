package com.kturker.multiplatform.core.insights

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Kotlin crash reporting (CrashKiOS) is installed only once Firebase is configured: without it there is no
// Crashlytics to report to
class IosInsightsTest {

    private val realInstall = IosInsights.installKotlinCrashReporting
    private var installs = 0

    @BeforeTest
    fun countInstalls() {
        IosInsights.installKotlinCrashReporting = { installs++ }
    }

    @AfterTest
    fun restoreInstall() {
        IosInsights.installKotlinCrashReporting = realInstall
    }

    @Test
    fun aConfiguredBridgeInstallsKotlinCrashReportingOnce() {
        IosInsights.register(FakeIosInsightsBridge(isConfigured = true))

        assertEquals(1, installs)
    }

    @Test
    fun aBridgeWithoutFirebaseLeavesItOff() {
        IosInsights.register(FakeIosInsightsBridge(isConfigured = false))

        assertEquals(0, installs)
    }
}
