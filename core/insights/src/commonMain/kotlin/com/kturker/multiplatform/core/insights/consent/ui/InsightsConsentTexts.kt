package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The analytics prompt's and the privacy switches' texts. English by default; a multilang project provides them in
 * the app's language (MainScreen.kt).
 */
data class InsightsConsentTexts(
    val promptTitle: String = "Help improve the app",
    val promptMessage: String = "Can we collect usage data, such as which screens you open, to improve the app? " +
        "You can change this at any time in the settings.",
    val allow: String = "Allow",
    val deny: String = "Don't allow",
    val settingsTitle: String = "Privacy",
    val analytics: String = "Share usage data",
    val analyticsDescription: String = "Which screens and features you use, to help improve the app",
    val crashReports: String = "Send crash reports",
    val crashReportsDescription: String = "What went wrong when the app crashes, to help fix it"
)

val LocalInsightsConsentTexts = staticCompositionLocalOf { InsightsConsentTexts() }
