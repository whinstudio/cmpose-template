package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.compositeOver

/**
 * The app's theme: the palette for [darkTheme] as [LocalColorsPalette], and a Material color
 * scheme derived from it, so Material components without explicit colors (Text, Button,
 * CircularProgressIndicator, Scaffold) follow light and dark mode too, plus [KtTypography] and
 * [KtShapes]. Wrap the app in it once (shared/.../AppTheme.kt does).
 */
@Composable
fun KtTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) OnDarkColorsPalette else OnLightColorsPalette
    CompositionLocalProvider(LocalColorsPalette provides palette) {
        MaterialTheme(
            colorScheme = colorSchemeFor(palette, darkTheme),
            typography = KtTypography,
            shapes = KtShapes,
            content = content
        )
    }
}

/** The palette's colors in their Material roles; every other role keeps the light or dark baseline. */
internal fun colorSchemeFor(
    palette: CustomColorsPalette,
    darkTheme: Boolean
): ColorScheme = (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
    primary = palette.tintPrimary,
    onPrimary = palette.backgroundColor,
    background = palette.backgroundColor,
    onBackground = palette.textPrimary,
    // surface stays the background so top bars and outlined cards sit flush with it
    surface = palette.backgroundColor,
    // Raised surfaces: small steps of text over the background, lighter as they rise in dark mode
    surfaceContainerLowest = palette.backgroundColor,
    surfaceContainerLow = palette.raised(0.02f),
    surfaceContainer = palette.raised(0.04f),
    surfaceContainerHigh = palette.raised(0.05f),
    surfaceContainerHighest = palette.raised(0.06f),
    surfaceVariant = palette.raised(0.06f),
    onSurface = palette.textPrimary,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.textFieldUnFocusedBorder
)

// Above 6% dark textSecondary drops below 4.5:1 on the raised surface
private fun CustomColorsPalette.raised(alpha: Float) =
    textPrimary.copy(alpha = alpha).compositeOver(backgroundColor)
