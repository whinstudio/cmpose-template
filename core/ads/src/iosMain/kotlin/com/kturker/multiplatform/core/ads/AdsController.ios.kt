package com.kturker.multiplatform.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

// How long a dismissed rewarded ad waits for a reward the SDK reports late.
internal val LATE_REWARD_GRACE = 1.seconds

@Composable
actual fun rememberAdsController(): AdsController {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val adsOff = rememberAdsOffForAutomation()
    return remember(lifecycle, adsOff) {
        if (adsOff) NoAdsController else LoggingAdsController(IosAdsController(lifecycle))
    }
}

// Each call hands the Swift bridge callbacks that resume the waiting coroutine. A cancelled call (the
// screen left or was recreated) tells the bridge, so a late consent form or ad is not presented. A loaded
// ad opens only on the resumed screen (presentWhenResumed).
internal class IosAdsController(private val lifecycle: Lifecycle) : AdsController {

    // The consent form, and the ATT prompt after it, open only on the resumed screen, like an ad: a form that
    // loads after Back is dropped and never opens over the next screen.
    override suspend fun gatherConsent(): ConsentStatus {
        val bridge = IosAds.requireBridge()
        return cancelPendingWhenCancelled(bridge) {
            loadConsent(bridge) ?: lifecycle.presentWhenResumed { result ->
                bridge.presentConsent { canRequestAds, privacyOptionsRequired ->
                    result.complete(ConsentStatus(canRequestAds, privacyOptionsRequired))
                }
            }
        }
    }

    // Null once a consent form or the ATT prompt waits to be presented, the consent otherwise.
    private suspend fun loadConsent(bridge: IosAdsBridge): ConsentStatus? =
        suspendCancellableCoroutine { continuation ->
            bridge.loadConsent(
                onReady = { continuation.resumeIfActive(null) },
                onComplete = { canRequestAds, privacyOptionsRequired ->
                    continuation.resumeIfActive(ConsentStatus(canRequestAds, privacyOptionsRequired))
                }
            )
        }

    override suspend fun showPrivacyOptions(): ConsentStatus =
        suspendCancellableCoroutine { continuation ->
            val bridge = IosAds.requireBridge()
            bridge.showPrivacyOptions { canRequestAds, privacyOptionsRequired ->
                continuation.resumeIfActive(ConsentStatus(canRequestAds, privacyOptionsRequired))
            }
            continuation.cancelPendingOnCancellation(bridge)
        }

    override suspend fun showInterstitial(): AdResult {
        val bridge = IosAds.requireBridge()
        return loadAndPresent(
            bridge,
            start = { onLoaded, onFailed -> bridge.loadInterstitial(AdUnitIds.interstitial, onLoaded, onFailed) },
            present = { result ->
                bridge.presentInterstitial(
                    onDismissed = { result.complete(AdResult.Dismissed) },
                    onFailed = { reason -> result.complete(AdResult.Failed(reason)) }
                )
            }
        )
    }

    // The SDK does not document whether the reward comes before or after the dismissal: a dismissal
    // without a reward waits LATE_REWARD_GRACE for it. Leaving the screen still cancels at once.
    override suspend fun showRewarded(): AdResult {
        val bridge = IosAds.requireBridge()
        val earned = CompletableDeferred<AdResult.Rewarded>()
        val shown: AdResult = loadAndPresent(
            bridge,
            start = { onLoaded, onFailed -> bridge.loadRewarded(AdUnitIds.rewarded, onLoaded, onFailed) },
            present = { result ->
                bridge.presentRewarded(
                    onReward = { amount, type -> earned.complete(AdResult.Rewarded(amount = amount, type = type)) },
                    onDismissed = { result.complete(AdResult.Dismissed) },
                    onFailed = { reason -> result.complete(AdResult.Failed(reason)) }
                )
            }
        )
        if (shown != AdResult.Dismissed) return shown
        return withTimeoutOrNull(LATE_REWARD_GRACE) { earned.await() } ?: AdResult.Dismissed
    }

    private suspend fun loadAndPresent(
        bridge: IosAdsBridge,
        start: (onLoaded: () -> Unit, onFailed: (String) -> Unit) -> Unit,
        present: (result: CompletableDeferred<AdResult>) -> Unit
    ): AdResult = cancelPendingWhenCancelled(bridge) {
        load(start) ?: lifecycle.presentWhenResumed(present)
    }

    // Null once the ad is loaded, the failure otherwise.
    private suspend fun load(
        start: (onLoaded: () -> Unit, onFailed: (String) -> Unit) -> Unit
    ): AdResult.Failed? = suspendCancellableCoroutine { continuation ->
        start(
            { continuation.resumeIfActive(null) },
            { reason -> continuation.resumeIfActive(AdResult.Failed(reason)) }
        )
    }
}

// The one place that tells the bridge a cancelled load-then-present call is gone: a call cancelled while
// loading, after the bridge reported "loaded" but before this coroutine resumed, or before the form or ad
// opens drops what was loaded. The single catch covers the cancel whenever it lands. After the form or ad
// opens only its result is dropped. Cancellation arrives on the main thread, where the bridge runs.
private suspend fun <T> cancelPendingWhenCancelled(
    bridge: IosAdsBridge,
    call: suspend () -> T
): T = try {
    call()
} catch (cancelled: CancellationException) {
    bridge.cancelPending()
    throw cancelled
}

// Registered after the request starts, so a cancellation always reaches the bridge after it. Compose
// cancels on the main thread, where the bridge runs.
private fun CancellableContinuation<*>.cancelPendingOnCancellation(bridge: IosAdsBridge) {
    invokeOnCancellation { bridge.cancelPending() }
}
