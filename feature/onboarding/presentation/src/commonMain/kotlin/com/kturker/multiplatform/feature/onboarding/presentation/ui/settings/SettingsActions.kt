package com.kturker.multiplatform.feature.onboarding.presentation.ui.settings

import com.kturker.multiplatform.core.database.theme.ThemeMode
import com.kturker.multiplatform.core.multilang.language.AppLanguage

internal interface SettingsActions {
    fun onModeChange(themeMode: ThemeMode)
    fun onLanguageChange(language: AppLanguage)
    fun onNavigateUp()
    fun onPremiumClick()
    fun onAdExamplesClick()
    fun onRateAppClick()
    fun onLicensesClick(
        title: String,
        backLabel: String
    )
}
