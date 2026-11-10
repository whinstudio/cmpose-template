package com.kturker.multiplatform.core.ads

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger

// Launch flag for automated runs such as store screenshots: no ad and no consent form reaches the screen, and the
// Mobile Ads SDK does not start. Android reads it from the launch intent, iOS from the launch arguments.
internal const val AUTOMATION_FLAG = "compassCapture"

private val log = Logger.withTag("Ads")

// Honored only where a shipped app can never run: debuggable Android builds and the iOS simulator.
internal class AutomationGate {

    // Compose calls this on the main thread only.
    private var markerLogged = false

    fun adsOff(
        flagPassed: Boolean,
        automationBuild: Boolean
    ): Boolean {
        val off = flagPassed && automationBuild
        if (off && !markerLogged) {
            markerLogged = true
            log.i { "CMPOSE_ADS consent=automation" }
        }
        return off
    }
}

internal val automationGate = AutomationGate()

// Answers as if ads were allowed, so a screen shows its normal state rather than a consent error.
internal object NoAdsController : AdsController {

    private val allowed = ConsentStatus(canRequestAds = true, privacyOptionsRequired = false)

    override suspend fun gatherConsent(): ConsentStatus = allowed

    override suspend fun showPrivacyOptions(): ConsentStatus = allowed

    override suspend fun showInterstitial(): AdResult = AdResult.Dismissed

    override suspend fun showRewarded(): AdResult = AdResult.Dismissed
}

@Composable
internal expect fun rememberAdsOffForAutomation(): Boolean
