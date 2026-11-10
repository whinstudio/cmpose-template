package com.kturker.multiplatform.core.ads

import android.app.Activity
import android.content.pm.ApplicationInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

// The Mobile Ads SDK (GMA Next-Gen) starts once per process, after consent allows ad requests.
private val mobileAdsStarted = AtomicBoolean(false)

@Composable
actual fun rememberAdsController(): AdsController {
    val activity = requireNotNull(LocalActivity.current) { "Ads need the hosting Activity" }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val adsOff = rememberAdsOffForAutomation()
    return remember(activity, lifecycle, adsOff) {
        if (adsOff) NoAdsController else LoggingAdsController(AndroidAdsController(activity, lifecycle))
    }
}

// Every callback checks that the caller still waits: a screen that is gone (left or recreated) cancels
// its call, and its ad or consent form must not open on top of another screen. A loaded ad opens only on
// the resumed screen (presentWhenResumed), from the main thread: the Next-Gen SDK calls back on
// background threads.
internal class AndroidAdsController(
    private val activity: Activity,
    private val lifecycle: Lifecycle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AdsController {

    private val isDebuggable = (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    // The consent form loads first and opens only on the resumed screen, like an ad: a form that loads after
    // Back is dropped and never opens over the next screen. A failed step ends the flow; canRequestAds then
    // tells whether ads may be requested.
    override suspend fun gatherConsent(): ConsentStatus {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val form = loadConsentFormIfRequired(consentInformation)
        if (form != null) {
            lifecycle.presentWhenResumed { result ->
                form.show(activity) { result.complete(Unit) }
            }
        }
        return consentInformation.startAdsIfAllowed()
    }

    // Null when the user has nothing to answer (consent not required, or given earlier) or a step failed.
    // UMP calls back on the main thread.
    private suspend fun loadConsentFormIfRequired(
        consentInformation: ConsentInformation
    ): ConsentForm? {
        val updated = suspendCancellableCoroutine { continuation ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                consentRequestParameters(),
                { continuation.resumeIfActive(true) },
                { continuation.resumeIfActive(false) }
            )
        }
        val required = updated &&
            consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED &&
            consentInformation.isConsentFormAvailable
        if (!required) return null
        return suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.loadConsentForm(
                activity,
                { form -> continuation.resumeIfActive(form) },
                { continuation.resumeIfActive(null) }
            )
        }
    }

    // Opened by a tap. UMP shows the form if it has one ready; if not, it reports an error at once and
    // loads one for a later tap. A form already on screen is not closed by a cancellation: only its
    // result is dropped.
    override suspend fun showPrivacyOptions(): ConsentStatus {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { continuation.resumeIfActive(Unit) }
        }
        return consentInformation.startAdsIfAllowed()
    }

    override suspend fun showInterstitial(): AdResult = when (val loaded = loadInterstitial()) {
        is Loaded.Failed -> AdResult.Failed(loaded.message)
        is Loaded.Ready -> show(loaded.ad)
    }

    override suspend fun showRewarded(): AdResult = when (val loaded = loadRewarded()) {
        is Loaded.Failed -> AdResult.Failed(loaded.message)
        is Loaded.Ready -> show(loaded.ad)
    }

    private suspend fun show(ad: InterstitialAd): AdResult =
        lifecycle.presentWhenResumed { result ->
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    result.complete(AdResult.Dismissed)
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    result.complete(AdResult.Failed(fullScreenContentError.message))
                }
            }
            ad.show(activity)
        }

    private suspend fun show(ad: RewardedAd): AdResult = lifecycle.presentWhenResumed { result ->
        // The reward and the dismissal arrive on SDK threads, so the reward is kept thread-safe.
        val earned = AtomicReference<AdResult?>(null)
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                result.complete(earned.get() ?: AdResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                result.complete(AdResult.Failed(fullScreenContentError.message))
            }
        }
        ad.show(
            activity,
            object : OnUserEarnedRewardListener {
                override fun onUserEarnedReward(reward: RewardItem) {
                    earned.set(AdResult.Rewarded(amount = reward.amount, type = reward.type))
                }
            }
        )
    }

    private suspend fun ConsentInformation.startAdsIfAllowed(): ConsentStatus {
        val status = ConsentStatus(
            canRequestAds = canRequestAds(),
            privacyOptionsRequired = privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        )
        if (status.canRequestAds) startMobileAds()
        return status
    }

    // MobileAds.initialize blocks, so it runs off the main thread. The flag is set inside the block: a
    // call cancelled before the block starts leaves the start to the next call.
    private suspend fun startMobileAds() {
        withContext(ioDispatcher) {
            if (mobileAdsStarted.compareAndSet(false, true)) {
                val config = InitializationConfig.Builder(ADMOB_APP_ID).build()
                MobileAds.initialize(activity.applicationContext, config) {
                    // Mediation adapters have started; the template uses none.
                }
            }
        }
    }

    // Debug builds force the EEA geography, so the consent form shows on emulators wherever they are
    // (a physical device also needs its test device hashed ID: addTestDeviceHashedId). Release builds
    // get the user's real geography.
    private fun consentRequestParameters(): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (isDebuggable) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            builder.setConsentDebugSettings(debugSettings)
        }
        return builder.build()
    }
}

// The load callbacks arrive on a background thread; resumeIfActive is safe from any thread.
private suspend fun loadInterstitial(): Loaded<InterstitialAd> =
    suspendCancellableCoroutine { continuation ->
        InterstitialAd.load(
            AdRequest.Builder(AdUnitIds.interstitial).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    continuation.resumeIfActive(Loaded.Ready(ad))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    continuation.resumeIfActive(Loaded.Failed(adError.message))
                }
            }
        )
    }

private suspend fun loadRewarded(): Loaded<RewardedAd> =
    suspendCancellableCoroutine { continuation ->
        RewardedAd.load(
            AdRequest.Builder(AdUnitIds.rewarded).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    continuation.resumeIfActive(Loaded.Ready(ad))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    continuation.resumeIfActive(Loaded.Failed(adError.message))
                }
            }
        )
    }

private sealed interface Loaded<out A> {
    data class Ready<out A>(val ad: A) : Loaded<A>

    data class Failed(val message: String) : Loaded<Nothing>
}
