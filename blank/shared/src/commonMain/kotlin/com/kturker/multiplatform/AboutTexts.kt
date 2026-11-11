package com.kturker.multiplatform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The About dialog's texts (GuideScreen.kt). English by default; a multilang project provides them in the app's
 * language (MainScreen.kt). Kept out of GuideScreen.kt, so deleting it leaves MainScreen.kt compiling.
 */
internal data class AboutTexts(
    val title: String = "About",
    val version: (String) -> String = { "Version $it" },
    val privacyPolicy: String = "Privacy policy",
    val terms: String = "Terms of use",
    val licenses: String = "Open source licenses",
    val close: String = "Close"
)

internal val LocalAboutTexts = staticCompositionLocalOf { AboutTexts() }
