package com.kturker.multiplatform.feature.monetization.presentation.ads

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.kturker.multiplatform.core.ads.AdResult
import com.kturker.multiplatform.core.ads.ConsentStatus
import com.kturker.multiplatform.core.ads.InterstitialPacer
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

// viewModelScope runs on Dispatchers.Main, which the tests swap for an eager test dispatcher.
@OptIn(ExperimentalCoroutinesApi::class)
class AdsDemoViewModelTest {

    private val adFree = MutableStateFlow<Boolean?>(false)
    private val createdViewModels = mutableListOf<AdsDemoViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() = runTest {
        createdViewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private val time = TestTimeSource()

    private fun createViewModel(
        pacer: InterstitialPacer = InterstitialPacer(timeSource = time, minSessionAge = Duration.ZERO)
    ) = AdsDemoViewModel(
        navigationManager = RecordingNavigationManager(),
        adFreeSource = FakeAdFreeSource(adFree),
        interstitialPacer = pacer
    ).also { createdViewModels += it }

    @Test
    fun opensWaitingForConsent() = runTest {
        val viewModel = createViewModel()

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            viewModel.onWatchRewardedClick()

            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.consentResolved)
        assertFalse(viewModel.uiState.value.canShowAds)
    }

    @Test
    fun consentEnablesTheButtons() {
        val viewModel = createViewModel()

        viewModel.onConsentResult(consent(canRequestAds = true))

        assertTrue(viewModel.uiState.value.consentResolved)
        assertTrue(viewModel.uiState.value.canShowAds)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun refusedConsentShowsAMessageAndKeepsTheButtonsOff() = runTest {
        val viewModel = createViewModel()

        viewModel.onConsentResult(consent(canRequestAds = false))

        assertTrue(viewModel.uiState.value.consentResolved)
        assertFalse(viewModel.uiState.value.canShowAds)
        assertEquals(AdsMessage.ConsentFailed, viewModel.uiState.value.message)
        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            viewModel.onWatchRewardedClick()

            expectNoEvents()
        }
    }

    @Test
    fun interstitialIsRequestedWhenNotAdFree() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
        }
    }

    @Test
    fun premiumSkipsTheInterstitial() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            adFree.value = true
            assertEquals(true, viewModel.uiState.value.adFree)

            viewModel.onShowInterstitialClick()

            expectNoEvents()
            assertEquals(AdsMessage.InterstitialSkipped, viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.adBusy)
        }
    }

    @Test
    fun premiumIsUnknownUntilTheSourceAnswers() {
        assertNull(AdsDemoUiState().adFree)
        assertFalse(AdsDemoUiState(consentReady = true).showBanner)
    }

    @Test
    fun unknownPremiumHidesTheBannerAndSendsNoInterstitial() = runTest {
        adFree.value = null
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()

            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.showBanner)
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.adBusy)
    }

    @Test
    fun unknownPremiumStillSendsTheRewardedAd() = runTest {
        adFree.value = null
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onWatchRewardedClick()
            assertEquals(AdCommand.ShowRewarded, awaitItem())
        }
    }

    @Test
    fun theBannerShowsOnlyWhenNotAdFree() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.showBanner)

        viewModel.onConsentResult(consent(canRequestAds = true))
        assertTrue(viewModel.uiState.value.showBanner)

        adFree.value = true
        assertFalse(viewModel.uiState.value.showBanner)
    }

    @Test
    fun premiumStillSendsTheRewardedAd() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))
        adFree.value = true

        viewModel.commands.test {
            viewModel.onWatchRewardedClick()
            assertEquals(AdCommand.ShowRewarded, awaitItem())
        }
    }

    @Test
    fun anAdInFlightKeepsTheButtonsOffUntilItsResult() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
            assertTrue(viewModel.uiState.value.adBusy)
            assertFalse(viewModel.uiState.value.canShowAds)

            viewModel.onWatchRewardedClick()
            viewModel.onShowInterstitialClick()
            expectNoEvents()

            viewModel.runAd { AdResult.Dismissed }
            assertTrue(viewModel.uiState.value.canShowAds)
            viewModel.onWatchRewardedClick()
            assertEquals(AdCommand.ShowRewarded, awaitItem())
        }
    }

    @Test
    fun aCancelledAdTurnsTheButtonsBackOn() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onWatchRewardedClick()
            assertEquals(AdCommand.ShowRewarded, awaitItem())

            viewModel.runAd { throw CancellationException("screen gone") }

            assertFalse(viewModel.uiState.value.adBusy)
            assertTrue(viewModel.uiState.value.canShowAds)
            assertEquals(0, viewModel.uiState.value.coins)
        }
    }

    @Test
    fun rewardAddsCoins() {
        val viewModel = createViewModel()

        viewModel.runAd { AdResult.Rewarded(amount = 10, type = "coins") }
        viewModel.runAd { AdResult.Rewarded(amount = 10, type = "coins") }

        assertEquals(20, viewModel.uiState.value.coins)
    }

    @Test
    fun aRewardedAdThatCompletesTurnsTheButtonsBackOn() {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))
        viewModel.onWatchRewardedClick()
        val ad = CompletableDeferred<AdResult>()

        viewModel.runAd { ad.await() }
        assertTrue(viewModel.uiState.value.adBusy)
        ad.complete(AdResult.Rewarded(amount = 10, type = "coins"))

        assertEquals(10, viewModel.uiState.value.coins)
        assertFalse(viewModel.uiState.value.adBusy)
    }

    // Android recreates the Activity on rotation: the screen's coroutine is cancelled while the ad stays on
    // screen, and its result must still reach the state.
    @Test
    fun theResultOfAnAdOnScreenSurvivesTheRecreatedScreen() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))
        viewModel.onWatchRewardedClick()
        val ad = CompletableDeferred<AdResult>()

        val screen = launch {
            viewModel.runAd { ad.await() }
            awaitCancellation()
        }
        runCurrent()
        assertTrue(viewModel.uiState.value.adBusy)
        screen.cancelAndJoin()
        ad.complete(AdResult.Rewarded(amount = 10, type = "coins"))

        assertEquals(10, viewModel.uiState.value.coins)
        assertFalse(viewModel.uiState.value.adBusy)
        assertTrue(viewModel.uiState.value.canShowAds)
    }

    // A loaded ad that did not open on a screen that went away is dropped by the controller with a cancellation.
    @Test
    fun aCancelledAdKeepsTheMessageAndTurnsTheButtonsBackOn() {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))
        viewModel.onWatchRewardedClick()
        viewModel.runAd { AdResult.Failed("no fill") }

        viewModel.runAd { throw CancellationException("screen gone") }

        assertFalse(viewModel.uiState.value.adBusy)
        assertEquals(AdsMessage.AdFailed("no fill"), viewModel.uiState.value.message)
        assertEquals(0, viewModel.uiState.value.coins)
    }

    @Test
    fun aClearedViewModelCancelsTheAdAndDropsItsResult() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))
        viewModel.onWatchRewardedClick()
        val ad = CompletableDeferred<AdResult>()
        var cancelled = false

        viewModel.runAd {
            try {
                ad.await()
            } catch (exception: CancellationException) {
                cancelled = true
                throw exception
            }
        }
        viewModel.viewModelScope.coroutineContext.job.cancelAndJoin()
        ad.complete(AdResult.Rewarded(amount = 10, type = "coins"))

        assertTrue(cancelled)
        assertEquals(0, viewModel.uiState.value.coins)
    }

    @Test
    fun failureIsShown() {
        val viewModel = createViewModel()

        viewModel.onConsentResult(consent(canRequestAds = true))
        viewModel.onWatchRewardedClick()

        viewModel.runAd { AdResult.Failed("no fill") }

        assertEquals(AdsMessage.AdFailed("no fill"), viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.canShowAds)
    }

    @Test
    fun interstitialWaitsForTheKnownPremiumState() {
        adFree.value = null
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        assertFalse(viewModel.uiState.value.canShowInterstitial)
        assertTrue(viewModel.uiState.value.canShowAds)

        adFree.value = false
        assertTrue(viewModel.uiState.value.canShowInterstitial)

        // Premium keeps the button on: a tap explains that the interstitial is skipped
        adFree.value = true
        assertTrue(viewModel.uiState.value.canShowInterstitial)
    }

    @Test
    fun privacyOptionsButtonFollowsTheConsentResult() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.showPrivacyOptionsButton)

        viewModel.onConsentResult(consent(canRequestAds = true, privacyOptionsRequired = true))
        assertTrue(viewModel.uiState.value.showPrivacyOptionsButton)

        val notRequired = createViewModel()
        notRequired.onConsentResult(consent(canRequestAds = true, privacyOptionsRequired = false))
        assertFalse(notRequired.uiState.value.showPrivacyOptionsButton)
    }

    @Test
    fun privacyOptionsClickSendsTheCommand() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = false, privacyOptionsRequired = true))

        viewModel.commands.test {
            viewModel.onPrivacyOptionsClick()
            assertEquals(AdCommand.ShowPrivacyOptions, awaitItem())
            assertTrue(viewModel.uiState.value.adBusy)

            // The form is on screen: a second tap waits for it
            viewModel.onPrivacyOptionsClick()
            expectNoEvents()
        }
    }

    @Test
    fun privacyOptionsClickDoesNothingWhenNotRequired() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true, privacyOptionsRequired = false))

        viewModel.commands.test {
            viewModel.onPrivacyOptionsClick()
            expectNoEvents()
        }
        assertFalse(viewModel.uiState.value.adBusy)
    }

    @Test
    fun theConsentAfterThePrivacyOptionsFormReachesTheState() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true, privacyOptionsRequired = true))

        viewModel.commands.test {
            viewModel.onPrivacyOptionsClick()
            assertEquals(AdCommand.ShowPrivacyOptions, awaitItem())
        }
        val form = CompletableDeferred<ConsentStatus>()
        val screen = launch {
            viewModel.runPrivacyOptions { form.await() }
            awaitCancellation()
        }
        runCurrent()
        assertTrue(viewModel.uiState.value.adBusy)
        screen.cancelAndJoin()
        form.complete(consent(canRequestAds = false, privacyOptionsRequired = true))

        assertFalse(viewModel.uiState.value.adBusy)
        assertFalse(viewModel.uiState.value.consentReady)
        assertFalse(viewModel.uiState.value.showBanner)
        assertEquals(AdsMessage.ConsentFailed, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.showPrivacyOptionsButton)

        viewModel.onPrivacyOptionsClick()
        viewModel.runPrivacyOptions { consent(canRequestAds = true, privacyOptionsRequired = true) }

        assertTrue(viewModel.uiState.value.consentReady)
        assertTrue(viewModel.uiState.value.canShowAds)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun aPacedInterstitialShowsWhyAndSendsNothing() = runTest {
        val viewModel = createViewModel(pacer = InterstitialPacer(timeSource = time, minSessionAge = 60.seconds))
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()

            expectNoEvents()
        }
        assertEquals(AdsMessage.InterstitialPaced, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.adBusy)
    }

    @Test
    fun aShownInterstitialUsesUpTheSession() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
            viewModel.runAd { AdResult.Dismissed }

            viewModel.onShowInterstitialClick()

            expectNoEvents()
        }
        assertEquals(AdsMessage.InterstitialPaced, viewModel.uiState.value.message)
    }

    @Test
    fun anInterstitialThatFailedToShowDoesNotCount() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
            viewModel.runAd { AdResult.Failed(reason = "no fill") }

            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
        }
    }

    @Test
    fun aRewardedAdDoesNotUseUpTheInterstitial() = runTest {
        val viewModel = createViewModel()
        viewModel.onConsentResult(consent(canRequestAds = true))

        viewModel.commands.test {
            viewModel.onWatchRewardedClick()
            assertEquals(AdCommand.ShowRewarded, awaitItem())
            viewModel.runAd { AdResult.Dismissed }

            viewModel.onShowInterstitialClick()
            assertEquals(AdCommand.ShowInterstitial, awaitItem())
        }
    }

    @Test
    fun premiumIsCheckedBeforeThePacer() = runTest {
        val viewModel = createViewModel(pacer = InterstitialPacer(timeSource = time, minSessionAge = 60.seconds))
        viewModel.onConsentResult(consent(canRequestAds = true))
        adFree.value = true

        viewModel.onShowInterstitialClick()

        assertEquals(AdsMessage.InterstitialSkipped, viewModel.uiState.value.message)
    }
}

private fun consent(canRequestAds: Boolean, privacyOptionsRequired: Boolean = false) =
    ConsentStatus(canRequestAds = canRequestAds, privacyOptionsRequired = privacyOptionsRequired)

private class RecordingNavigationManager : NavigationManager {
    val commands = mutableListOf<NavigationCommand>()
    override val navigationCommandFlow: Flow<NavigationCommand> = emptyFlow()

    override fun navigate(navigationCommand: NavigationCommand) {
        commands += navigationCommand
    }
}

private class FakeAdFreeSource(override val isAdFree: Flow<Boolean?>) : AdFreeSource
