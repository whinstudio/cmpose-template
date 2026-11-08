package com.kturker.multiplatform.feature.monetization.presentation.paywall

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.AppVersion
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PaywallOffer
import com.kturker.multiplatform.core.purchases.PaywallPackage
import com.kturker.multiplatform.core.purchases.PurchaseResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val WEEKLY = PaywallPackage("weekly", PackagePeriod.WEEKLY, "$2.99", "$12.99", null, null)
private val MONTHLY = PaywallPackage("monthly", PackagePeriod.MONTHLY, "$4.99", null, null, null)
private val ANNUAL = PaywallPackage("annual", PackagePeriod.ANNUAL, "$29.99", "$2.49", 50, "7 days")
private val LIFETIME = PaywallPackage("lifetime", PackagePeriod.LIFETIME, "$79.99", null, null, null)
private val PRIVACY = LegalLink(kind = LegalLinkKind.PrivacyPolicy, url = "https://example.com/privacy")
private val TERMS = LegalLink(kind = LegalLinkKind.Terms, url = "https://example.com/terms")
private val OFFER = PaywallOffer(listOf(WEEKLY, MONTHLY, ANNUAL))

// viewModelScope runs on Dispatchers.Main, which the tests swap for an eager test dispatcher.
@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {

    private val navigationManager = RecordingNavigationManager()
    private val createdViewModels = mutableListOf<PaywallViewModel>()

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

    private fun createViewModel(
        purchasesManager: FakePurchasesManager,
        legalLinks: List<LegalLink> = emptyList(),
        isDebug: Boolean = false
    ) = PaywallViewModel(
        environment = BuildEnvironment(isDebug = isDebug),
        navigationManager = navigationManager,
        purchasesManager = purchasesManager,
        appInfo = FakeAppInfo(legalLinks)
    ).also { createdViewModels += it }

    @Test
    fun theOfferLoadsWithTheAnnualPlanSelected() {
        val state = createViewModel(FakePurchasesManager(offerResult = OFFER)).uiState.value

        assertFalse(state.loading)
        assertEquals(OFFER, state.offer)
        assertEquals(ANNUAL.id, state.selectedPackageId)
        assertNull(state.error)
    }

    @Test
    fun withoutAnAnnualPlanTheFirstIsSelected() {
        assertEquals(MONTHLY.id, defaultSelection(listOf(MONTHLY, WEEKLY)))
        assertNull(defaultSelection(emptyList()))
    }

    @Test
    fun withoutRevenueCatNothingLoadsAndNothingIsAsked() {
        val purchases = FakePurchasesManager(isAvailable = false, offerResult = OFFER)
        val state = createViewModel(purchases).uiState.value

        assertFalse(state.isAvailable)
        assertFalse(state.loading)
        assertNull(state.offer)
        assertEquals(0, purchases.offerCalls)
    }

    @Test
    fun anOfferThatCannotLoadShowsAnErrorAndRetryLoadsAgain() {
        val purchases = FakePurchasesManager(offerResult = null)
        val viewModel = createViewModel(purchases)

        assertEquals(PLANS_UNAVAILABLE_MESSAGE, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.loading)

        purchases.offerResult = OFFER
        viewModel.onRetry()

        assertEquals(2, purchases.offerCalls)
        assertEquals(OFFER, viewModel.uiState.value.offer)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun anEmptyOfferIsAnError() {
        val state = createViewModel(FakePurchasesManager(offerResult = PaywallOffer(emptyList()))).uiState.value

        assertEquals(PLANS_UNAVAILABLE_MESSAGE, state.error)
        assertNull(state.selectedPackageId)
    }

    @Test
    fun aSuccessfulPurchaseOfTheSelectedPlanClosesThePaywallOnce() {
        val purchases = FakePurchasesManager(offerResult = OFFER)
        val viewModel = createViewModel(purchases)

        viewModel.onPackageSelected(MONTHLY.id)
        viewModel.onPurchaseClick()
        viewModel.onDismiss()

        assertEquals(listOf(MONTHLY.id), purchases.purchasedIds)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigationManager.commands)
    }

    @Test
    fun aCancelledPurchaseKeepsThePaywallOpenWithoutAnError() {
        val viewModel = createViewModel(FakePurchasesManager(offerResult = OFFER, purchaseResult = PurchaseResult.Cancelled))

        viewModel.onPurchaseClick()

        assertFalse(viewModel.uiState.value.inProgress)
        assertNull(viewModel.uiState.value.error)
        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)
    }

    @Test
    fun aFailedPurchaseShowsItsMessageUntilTheNextChoice() {
        val viewModel = createViewModel(FakePurchasesManager(offerResult = OFFER, purchaseResult = PurchaseResult.Error("No connection")))

        viewModel.onPurchaseClick()
        assertEquals("No connection", viewModel.uiState.value.error)
        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)

        viewModel.onPackageSelected(WEEKLY.id)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aSecondTapWhileAPurchaseRunsIsIgnored() {
        val pending = CompletableDeferred<Unit>()
        val purchases = FakePurchasesManager(offerResult = OFFER).also { it.pending = pending }
        val viewModel = createViewModel(purchases)

        viewModel.onPurchaseClick()
        viewModel.onPurchaseClick()
        viewModel.onRestoreClick()
        viewModel.onPackageSelected(WEEKLY.id)

        assertTrue(viewModel.uiState.value.inProgress)
        assertEquals(listOf(ANNUAL.id), purchases.purchasedIds)
        assertEquals(0, purchases.restoreCalls)
        assertEquals(ANNUAL.id, viewModel.uiState.value.selectedPackageId)

        pending.complete(Unit)

        assertFalse(viewModel.uiState.value.inProgress)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigationManager.commands)
    }

    @Test
    fun nothingIsBoughtWithoutAPlan() {
        val purchases = FakePurchasesManager(offerResult = null)

        createViewModel(purchases).onPurchaseClick()

        assertEquals(emptyList<String>(), purchases.purchasedIds)
    }

    @Test
    fun aRestoreThatBringsPremiumBackClosesThePaywall() {
        val purchases = FakePurchasesManager(offerResult = OFFER, restoreResult = PurchaseResult.Success)

        createViewModel(purchases).onRestoreClick()

        assertEquals(1, purchases.restoreCalls)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigationManager.commands)
    }

    @Test
    fun aRestoreWithoutPurchasesShowsTheMessageAndStaysOpen() {
        val viewModel = createViewModel(
            FakePurchasesManager(offerResult = OFFER, restoreResult = PurchaseResult.Error("No purchases to restore"))
        )

        viewModel.onRestoreClick()

        assertEquals("No purchases to restore", viewModel.uiState.value.error)
        assertEquals(emptyList<NavigationCommand>(), navigationManager.commands)
    }

    @Test
    fun theLegalLinksComeFromAppInfo() {
        val links = listOf(LegalLink(kind = LegalLinkKind.PrivacyPolicy, url = "https://example.com/privacy"))

        assertEquals(links, createViewModel(FakePurchasesManager(offerResult = OFFER), legalLinks = links).uiState.value.legalLinks)
    }

    @Test
    fun aDebugBuildWithoutBothLegalLinksWarnsTheDeveloper() {
        val viewModel = createViewModel(FakePurchasesManager(offerResult = OFFER), legalLinks = listOf(PRIVACY), isDebug = true)

        assertTrue(viewModel.uiState.value.missingLegalLinks)
    }

    @Test
    fun aReleaseBuildWithoutLegalLinksShowsNoWarning() {
        val viewModel = createViewModel(FakePurchasesManager(offerResult = OFFER), isDebug = false)

        assertFalse(viewModel.uiState.value.missingLegalLinks)
    }

    @Test
    fun aDebugBuildWithBothLegalLinksShowsNoWarning() {
        val viewModel = createViewModel(FakePurchasesManager(offerResult = OFFER), legalLinks = listOf(PRIVACY, TERMS), isDebug = true)

        assertFalse(viewModel.uiState.value.missingLegalLinks)
    }

    @Test
    fun subscriptionPlansShowTheRenewalTerms() {
        assertTrue(PaywallUiState(isAvailable = true, offer = PaywallOffer(listOf(MONTHLY, ANNUAL))).showsRenewalTerms)
    }

    @Test
    fun aLifetimeOnlyOfferShowsNoRenewalTerms() {
        assertFalse(PaywallUiState(isAvailable = true, offer = PaywallOffer(listOf(LIFETIME))).showsRenewalTerms)
    }

    @Test
    fun aMixedOfferShowsTheRenewalTerms() {
        assertTrue(PaywallUiState(isAvailable = true, offer = PaywallOffer(listOf(LIFETIME, MONTHLY))).showsRenewalTerms)
    }
}

private class FakeAppInfo(override val legalLinks: List<LegalLink>) : AppInfo {
    override val version: AppVersion = AppVersion(name = "1.0", code = "1")
}
