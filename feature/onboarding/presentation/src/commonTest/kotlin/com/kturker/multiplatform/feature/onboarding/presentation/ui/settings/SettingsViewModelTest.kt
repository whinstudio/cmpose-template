package com.kturker.multiplatform.feature.onboarding.presentation.ui.settings

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.AppVersion
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.about.licenses.LicensesDestination
import com.kturker.multiplatform.core.database.theme.DarkModeManager
import com.kturker.multiplatform.core.database.theme.ThemeMode
import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.ReviewCounterStore
import com.kturker.multiplatform.core.review.ReviewPrompter
import com.kturker.multiplatform.core.review.ReviewRequest
import com.kturker.multiplatform.feature.account.contract.AccountSettingsDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// viewModelScope runs on Dispatchers.Main, which the tests swap for a test dispatcher
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val createdViewModels = mutableListOf<SettingsViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    // Every view model's scope is cancelled and joined while Main is still the test dispatcher
    @AfterTest
    fun tearDown() = runTest {
        createdViewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        reviewPrompter: ReviewPrompter = ReviewPrompter(store = InMemoryReviewCounterStore()),
        navigationManager: NavigationManager = RecordingNavigationManager(),
        appInfo: AppInfo = FakeAppInfo()
    ) = SettingsViewModel(
        darkModeManager = FakeDarkModeManager(),
        navigationManager = navigationManager,
        languageManager = FakeLanguageManager(),
        reviewPrompter = reviewPrompter,
        appInfo = appInfo
    ).also { createdViewModels += it }

    @Test
    fun rateTheAppOpensTheStorePage() = runTest {
        val prompter = ReviewPrompter(store = InMemoryReviewCounterStore())
        val viewModel = createViewModel(reviewPrompter = prompter)

        prompter.requests.test {
            viewModel.onRateAppClick()
            assertEquals(ReviewRequest.StorePage, awaitItem())
        }
    }

    @Test
    fun theVersionRowShowsTheNameAndBuildNumber() {
        val viewModel = createViewModel()

        assertEquals("2.1.0 (42)", viewModel.uiState.value.versionText)
    }

    @Test
    fun legalRowsFollowTheFilledUrls() {
        val terms = LegalLink(kind = LegalLinkKind.Terms, url = "https://e.example/t")
        val viewModel = createViewModel(appInfo = FakeAppInfo(legalLinks = listOf(terms)))

        assertNull(viewModel.uiState.value.privacyPolicyUrl)
        assertEquals("https://e.example/t", viewModel.uiState.value.termsUrl)
    }

    @Test
    fun licensesOpenWithTheirTitleAndBackLabel() {
        val navigationManager = RecordingNavigationManager()
        val viewModel = createViewModel(navigationManager = navigationManager)

        viewModel.onLicensesClick(title = "Lisanslar", backLabel = "Geri")

        assertEquals(LicensesDestination(title = "Lisanslar", backLabel = "Geri"), navigationManager.commands.last())
    }
    // cmpose:backend

    @Test
    fun accountRowOpensTheAccountSettings() {
        val navigationManager = RecordingNavigationManager()
        val viewModel = createViewModel(navigationManager = navigationManager)

        viewModel.onAccountClick()

        assertEquals(AccountSettingsDestination, navigationManager.commands.last())
    }
    // /cmpose:backend
}

private class FakeDarkModeManager : DarkModeManager {
    override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit
}

// The real strings (resourceEN) are internal to core/multilang: the settings screen's view model
// does not read them, so every string is empty
private class FakeLanguageManager : LanguageManager {
    override val currentResources: StateFlow<StringResourcesUiModel> = MutableStateFlow(emptyStrings())

    override fun getCurrentLanguageFlow(): Flow<AppLanguage> = flowOf(AppLanguage.SYSTEM)

    override suspend fun setLanguage(language: AppLanguage) = Unit

    override suspend fun setSystemLanguage() = Unit
}

private class RecordingNavigationManager : NavigationManager {
    val commands = mutableListOf<NavigationCommand>()
    override val navigationCommandFlow: Flow<NavigationCommand> = emptyFlow()

    override fun navigate(navigationCommand: NavigationCommand) {
        commands += navigationCommand
    }
}

private class FakeAppInfo(
    override val version: AppVersion = AppVersion(name = "2.1.0", code = "42"),
    override val legalLinks: List<LegalLink> = emptyList()
) : AppInfo

private class InMemoryReviewCounterStore : ReviewCounterStore {
    private val values = mutableMapOf<String, Int>()

    override fun getInt(
        key: String,
        default: Int
    ): Int = values[key] ?: default

    override fun putInt(
        key: String,
        value: Int
    ) {
        values[key] = value
    }
}

private fun emptyStrings() = StringResourcesUiModel(
    settingsTitle = "",
    githubSearchHint = "",
    themeLight = "",
    themeSystem = "",
    themeDark = "",
    languageSystem = "",
    statFollowers = "",
    statFollowing = "",
    statRepos = "",
    statGists = "",
    errorNoConnection = "",
    errorRateLimit = "",
    errorServer = "",
    errorUnexpected = "",
    settingsPremium = "",
    settingsAdExamples = "",
    settingsRateApp = "",
    settingsVersion = "",
    settingsPrivacyPolicy = "",
    settingsTerms = "",
    settingsLicenses = "",
    // cmpose:backend
    settingsAccount = "",
    // /cmpose:backend
    insightsPromptTitle = "",
    insightsPromptMessage = "",
    insightsAllow = "",
    insightsDeny = "",
    insightsSettingsTitle = "",
    insightsAnalytics = "",
    insightsAnalyticsDescription = "",
    insightsCrashReports = "",
    insightsCrashReportsDescription = "",
    offlineBanner = "",
    updateAvailableTitle = "",
    updateAvailableMessage = "",
    updateNow = "",
    updateLater = "",
    updateRequiredTitle = "",
    updateRequiredMessage = "",
    updateDownloaded = "",
    updateRestart = "",
    favoritesTitle = "",
    addFavorite = "",
    removeFavorite = "",
    retry = "",
    back = "",
    paywallNotNow = "",
    paywallHeadline = "",
    paywallBenefitRemoveAds = "",
    paywallBenefitOne = "",
    paywallBenefitTwo = "",
    paywallContinue = "",
    paywallRestore = "",
    paywallPurchasesDisabled = "",
    paywallRenewalTerms = "",
    paywallMissingLegalLinks = "",
    paywallPeriodWeekly = "",
    paywallPeriodMonthly = "",
    paywallPeriodAnnual = "",
    paywallPeriodLifetime = "",
    paywallPeriodOther = "",
    paywallPriceWeekly = "",
    paywallPriceMonthly = "",
    paywallPriceAnnual = "",
    paywallPriceLifetime = "",
    paywallPricePerMonth = "",
    paywallSave = "",
    paywallFreeTrial = "",
    paywallPlansUnavailable = "",
    purchaseErrorUnavailable = "",
    purchaseErrorPackageUnavailable = "",
    purchaseErrorNothingToRestore = "",
    purchaseErrorNetwork = "",
    purchaseErrorPending = "",
    purchaseErrorStore = "",
    purchaseErrorNotAllowed = "",
    purchaseErrorFailed = "",
    adsPremiumOff = "",
    adsShowInterstitial = "",
    adsWatchRewarded = "",
    adsPrivacyOptions = "",
    adsCoins = "",
    adsTestNote = "",
    adsConsentFailed = "",
    adsInterstitialPaced = "",
    adsInterstitialSkipped = "",
    adsFailed = "",
    searchPromptTitle = "",
    searchPromptMessage = "",
    searchNoResultsTitle = "",
    searchNoResultsMessage = "",
    favoritesEmptyTitle = "",
    favoritesEmptyMessage = ""
)
