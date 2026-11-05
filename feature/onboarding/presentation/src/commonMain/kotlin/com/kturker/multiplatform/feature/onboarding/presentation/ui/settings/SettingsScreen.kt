package com.kturker.multiplatform.feature.onboarding.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.database.theme.ThemeMode
import com.kturker.multiplatform.core.designsystem.component.KtIcon
import com.kturker.multiplatform.core.designsystem.component.KtText
import com.kturker.multiplatform.core.designsystem.icon.IcBack
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette
import com.kturker.multiplatform.core.multilang.language.AppLanguage
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsScreenRoute(viewModel: SettingsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        actions = viewModel
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    actions: SettingsActions
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            KtIcon(
                modifier = Modifier.padding(all = 16.dp).size(size = 24.dp).clickable {
                    actions.onNavigateUp()
                }.align(Alignment.CenterStart),
                imageVector = IcBack,
                contentDescription = LocalStringResources.current.back,
                tint = LocalColorsPalette.current.tintBlack
            )

            KtText(
                modifier = Modifier.align(Alignment.Center),
                text = LocalStringResources.current.settingsTitle,
                color = LocalColorsPalette.current.textBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        ThemeModeToggle(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selectedMode = uiState.selectedMode,
            onModeChange = actions::onModeChange
        )

        LanguageModeToggle(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selectedLanguage = uiState.appLanguage,
            onLanguageChange = actions::onLanguageChange
        )

        SettingsLinkRow(
            text = LocalStringResources.current.settingsPremium,
            onClick = actions::onPremiumClick
        )

        SettingsLinkRow(
            text = LocalStringResources.current.settingsAdExamples,
            onClick = actions::onAdExamplesClick
        )

        SettingsLinkRow(
            text = LocalStringResources.current.settingsRateApp,
            onClick = actions::onRateAppClick
        )

        SettingsValueRow(
            label = LocalStringResources.current.settingsVersion,
            value = uiState.versionText
        )

        uiState.privacyPolicyUrl?.let { url ->
            SettingsLinkRow(
                text = LocalStringResources.current.settingsPrivacyPolicy,
                onClick = { uriHandler.openUri(url) }
            )
        }

        uiState.termsUrl?.let { url ->
            SettingsLinkRow(
                text = LocalStringResources.current.settingsTerms,
                onClick = { uriHandler.openUri(url) }
            )
        }

        val licensesTitle = LocalStringResources.current.settingsLicenses
        val backLabel = LocalStringResources.current.back
        SettingsLinkRow(
            text = licensesTitle,
            onClick = { actions.onLicensesClick(title = licensesTitle, backLabel = backLabel) }
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        KtText(
            text = label,
            color = LocalColorsPalette.current.textBlack,
            fontSize = 16.sp
        )

        KtText(
            text = value,
            color = LocalColorsPalette.current.textGray,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SettingsLinkRow(
    text: String,
    onClick: () -> Unit
) {
    KtText(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        text = text,
        color = LocalColorsPalette.current.textBlack,
        fontSize = 16.sp
    )
}

@Composable
private fun ThemeModeToggle(
    selectedMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = LocalColorsPalette.current
    val strings = LocalStringResources.current

    val modes = listOf(
        ThemeMode.LIGHT to strings.themeLight,
        ThemeMode.SYSTEM to strings.themeSystem,
        ThemeMode.DARK to strings.themeDark
    )

    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(percent = 12))
            .background(color.toggleBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = mode == selectedMode

            val textColor = if (isSelected) color.toggleSelectedText else color.toggleUnSelectedText

            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50))
                    .background(
                        color = if (isSelected) {
                            color.toggleSelectedBackground
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LanguageModeToggle(
    selectedLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = LocalColorsPalette.current

    FlowRow(
        modifier = modifier.clip(RoundedCornerShape(12))
            .background(color.toggleBackground)
            .padding(4.dp),
        maxItemsInEachRow = 4
    ) {
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selectedLanguage

            val textColor = if (isSelected) color.toggleSelectedText else color.toggleUnSelectedText

            Box(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50))
                    .background(
                        color = if (isSelected) {
                            color.toggleSelectedBackground
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onLanguageChange(language) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.displayName ?: LocalStringResources.current.languageSystem,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
