package com.kturker.multiplatform.feature.onboarding.presentation.ui.settings

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.about.licenses.LicensesDestination
import com.kturker.multiplatform.core.database.theme.DarkModeManager
import com.kturker.multiplatform.core.database.theme.ThemeMode
import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.core.review.ReviewPrompter
import com.kturker.multiplatform.feature.account.contract.AccountSettingsDestination
import com.kturker.multiplatform.feature.monetization.contract.AdsDemoDestination
import com.kturker.multiplatform.feature.monetization.contract.PaywallDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

@KoinViewModel
internal class SettingsViewModel(
    @Provided val darkModeManager: DarkModeManager,
    @Provided val navigationManager: NavigationManager,
    @Provided val languageManager: LanguageManager,
    @Provided val reviewPrompter: ReviewPrompter,
    @Provided val appInfo: AppInfo
) : CoreViewModel(),
    SettingsActions {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState>
        get() = _uiState

    init {
        // The About rows: the app's version and the legal URLs that are filled in (core/about)
        _uiState.update { state ->
            state.copy(
                versionText = appInfo.version.displayText,
                privacyPolicyUrl = appInfo.legalLinks.firstOrNull { it.kind == LegalLinkKind.PrivacyPolicy }?.url,
                termsUrl = appInfo.legalLinks.firstOrNull { it.kind == LegalLinkKind.Terms }?.url
            )
        }

        viewModelScope.launch {
            darkModeManager.getThemeMode().collect { themeMode ->
                _uiState.update {
                    it.copy(selectedMode = themeMode)
                }
            }
        }

        viewModelScope.launch {
            languageManager.getCurrentLanguageFlow().collect { appLanguage ->
                _uiState.update {
                    it.copy(appLanguage = appLanguage)
                }
            }
        }
    }

    override fun onModeChange(themeMode: ThemeMode) {
        viewModelScope.launch {
            darkModeManager.setThemeMode(mode = themeMode)
        }
    }

    override fun onNavigateUp() {
        navigationManager.navigate(NavigationCommand.NavigateUp)
    }

    override fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
        }
    }

    // cmpose:backend
    // The account settings: sign-out, account deletion and the privacy links, which the stores require in reach
    override fun onAccountClick() {
        navigationManager.navigate(navigationCommand = AccountSettingsDestination)
    }

    // /cmpose:backend
    override fun onPremiumClick() {
        navigationManager.navigate(navigationCommand = PaywallDestination)
    }

    override fun onAdExamplesClick() {
        navigationManager.navigate(navigationCommand = AdsDemoDestination)
    }

    override fun onRateAppClick() = reviewPrompter.openStorePage()

    override fun onLicensesClick(
        title: String,
        backLabel: String
    ) =
        navigationManager.navigate(navigationCommand = LicensesDestination(title = title, backLabel = backLabel))
}
