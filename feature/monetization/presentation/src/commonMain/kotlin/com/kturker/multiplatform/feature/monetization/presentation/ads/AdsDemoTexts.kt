package com.kturker.multiplatform.feature.monetization.presentation.ads

import androidx.compose.runtime.staticCompositionLocalOf

// The ad examples screen's texts; the defaults are English. The showcase passes its translations.
// {coins} and {reason} are filled in by the screen; reason is the SDK's own text.
data class AdsDemoTexts(
    val title: String = "Ad examples",
    val back: String = "Back",
    val premiumAdsOff: String = "Premium: ads are off",
    val showInterstitial: String = "Show interstitial",
    val watchRewarded: String = "Watch rewarded ad",
    val privacyOptions: String = "Privacy options",
    val coins: String = "Coins: {coins}",
    val testAdsNote: String = "Test ads (Google test units). Debug builds force the EEA consent form.",
    val consentFailed: String = "Ads unavailable: consent could not be gathered",
    val interstitialPaced: String = "Interstitial paced: not in the first minute, at most one per session",
    val interstitialSkipped: String = "Premium: interstitial skipped",
    val adFailed: String = "Ad failed: {reason}"
)

val LocalAdsDemoTexts = staticCompositionLocalOf { AdsDemoTexts() }

internal fun AdsDemoTexts.message(message: AdsMessage): String = when (message) {
    AdsMessage.ConsentFailed -> consentFailed
    AdsMessage.InterstitialPaced -> interstitialPaced
    AdsMessage.InterstitialSkipped -> interstitialSkipped
    is AdsMessage.AdFailed -> adFailed.replace("{reason}", message.reason)
}

internal fun AdsDemoTexts.coinsLabel(coins: Int): String =
    this.coins.replace("{coins}", coins.toString())
