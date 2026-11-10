package com.kturker.multiplatform.feature.monetization.presentation.ads

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.ads.AdResult
import com.kturker.multiplatform.core.ads.ConsentStatus
import com.kturker.multiplatform.core.ads.InterstitialPacer
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
internal class AdsDemoViewModel(
    @Provided private val navigationManager: NavigationManager,
    private val adFreeSource: AdFreeSource,
    private val interstitialPacer: InterstitialPacer
) : CoreViewModel(),
    AdsDemoActions {

    private val _uiState = MutableStateFlow(AdsDemoUiState())
    val uiState: StateFlow<AdsDemoUiState> = _uiState.asStateFlow()

    // Ad SDK calls need the Activity (Android) or the Swift bridge (iOS): the screen collects the commands and
    // hands each SDK call back to runAd / runPrivacyOptions
    private val _commands = Channel<AdCommand>(Channel.BUFFERED)
    val commands: Flow<AdCommand> = _commands.receiveAsFlow()

    // The full-screen ad on its way: an interstitial counts for the pacer only once it was on screen and closed
    private var adInFlight: AdCommand? = null

    init {
        viewModelScope.launch {
            adFreeSource.isAdFree.collect { adFree ->
                _uiState.update { it.copy(adFree = adFree) }
            }
        }
    }

    override fun onConsentResult(status: ConsentStatus) {
        _uiState.update {
            it.copy(
                consentResolved = true,
                consentReady = status.canRequestAds,
                privacyOptionsRequired = status.privacyOptionsRequired,
                message = if (status.canRequestAds) it.message else AdsMessage.ConsentFailed
            )
        }
    }

    override fun onPrivacyOptionsClick() {
        val state = _uiState.value
        if (state.showPrivacyOptionsButton && !state.adBusy) show(AdCommand.ShowPrivacyOptions)
    }

    // The SDK call runs in the ViewModel's scope, not the screen's: Android recreates the screen on rotation
    // while a full-screen ad stays open, and the ad's result must still arrive. The screen's controller keeps
    // the gate (a loaded ad opens only on the resumed screen; one that did not open when its screen went away
    // ends with a cancellation). Leaving the screen for good clears the ViewModel and cancels the call.
    fun runAd(show: suspend () -> AdResult) = launchCall(show, ::onAdResult)

    fun runPrivacyOptions(show: suspend () -> ConsentStatus) =
        launchCall(show, ::onPrivacyOptionsResult)

    private fun <T> launchCall(
        call: suspend () -> T,
        onResult: (T) -> Unit
    ) {
        viewModelScope.launch {
            val result = try {
                call()
            } catch (cancelled: CancellationException) {
                onAdCancelled()
                throw cancelled
            }
            onResult(result)
        }
    }

    // The user may have withdrawn or given consent in the form: ads follow the new answer
    private fun onPrivacyOptionsResult(status: ConsentStatus) {
        _uiState.update {
            it.copy(
                adBusy = false,
                consentReady = status.canRequestAds,
                privacyOptionsRequired = status.privacyOptionsRequired,
                message = if (status.canRequestAds) null else AdsMessage.ConsentFailed
            )
        }
    }

    override fun onShowInterstitialClick() {
        val state = _uiState.value
        // Premium unknown (adFree null, RevenueCat has not answered): the button is off, a premium user may be waiting
        when {
            !state.canShowInterstitial -> Unit

            state.adFree == true -> _uiState.update { it.copy(message = AdsMessage.InterstitialSkipped) }

            // A real app asks the pacer at a natural break and simply carries on when it says no
            !interstitialPacer.shouldShow() -> _uiState.update { it.copy(message = AdsMessage.InterstitialPaced) }

            else -> show(AdCommand.ShowInterstitial)
        }
    }

    // Rewarded ads are opt-in, so premium users (and users whose premium state is unknown) can still watch one
    override fun onWatchRewardedClick() {
        if (_uiState.value.canShowAds) show(AdCommand.ShowRewarded)
    }

    private fun onAdResult(result: AdResult) {
        if (adInFlight == AdCommand.ShowInterstitial && result == AdResult.Dismissed) interstitialPacer.recordShown()
        adInFlight = null
        _uiState.update { state ->
            when (result) {
                is AdResult.Rewarded -> state.copy(adBusy = false, coins = state.coins + result.amount, message = null)
                is AdResult.Failed -> state.copy(adBusy = false, message = AdsMessage.AdFailed(result.reason))
                AdResult.Dismissed -> state.copy(adBusy = false)
            }
        }
    }

    // The ad did not open before its screen went away, or the ViewModel was cleared: no result will come
    private fun onAdCancelled() {
        adInFlight = null
        _uiState.update { it.copy(adBusy = false) }
    }

    override fun onNavigateUp() {
        navigationManager.navigate(NavigationCommand.NavigateUp)
    }

    private fun show(command: AdCommand) {
        adInFlight = command
        _uiState.update { it.copy(adBusy = true, message = null) }
        _commands.trySend(command)
    }
}
