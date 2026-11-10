package com.kturker.multiplatform.core.ads

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import co.touchlab.kermit.platformLogWriter
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKermitApi::class)
class AutomationAdsOffTest {

    private val writer = TestLogWriter(loggable = Severity.Verbose)

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(writer)
        Logger.setMinSeverity(Severity.Verbose)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun theFlagTurnsAdsOffInAnAutomationBuild() {
        assertTrue(AutomationGate().adsOff(flagPassed = true, automationBuild = true))
    }

    @Test
    fun aShippedBuildIgnoresTheFlag() {
        assertFalse(AutomationGate().adsOff(flagPassed = true, automationBuild = false))
        assertTrue(writer.logs.isEmpty())
    }

    @Test
    fun withoutTheFlagAdsStayOn() {
        assertFalse(AutomationGate().adsOff(flagPassed = false, automationBuild = true))
        assertTrue(writer.logs.isEmpty())
    }

    @Test
    fun anHonoredFlagLogsOneLine() {
        val gate = AutomationGate()

        repeat(3) { gate.adsOff(flagPassed = true, automationBuild = true) }

        assertEquals(listOf("CMPOSE_ADS consent=automation"), writer.logs.map { it.message })
    }

    @Test
    fun consentAsksNothingAndAllowsTheScreenToCarryOn() = runTest {
        val allowed = ConsentStatus(canRequestAds = true, privacyOptionsRequired = false)

        assertEquals(allowed, NoAdsController.gatherConsent())
        assertEquals(allowed, NoAdsController.showPrivacyOptions())
    }

    @Test
    fun fullScreenAdsEndWithoutShowingOrRewarding() = runTest {
        assertEquals(AdResult.Dismissed, NoAdsController.showInterstitial())
        assertEquals(AdResult.Dismissed, NoAdsController.showRewarded())
    }
}
