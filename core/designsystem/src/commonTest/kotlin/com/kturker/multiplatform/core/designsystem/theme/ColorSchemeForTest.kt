package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSchemeForTest {

    @Test
    fun lightSchemeTakesItsRolesFromTheLightPalette() {
        assertRoles(OnLightColorsPalette, colorSchemeFor(OnLightColorsPalette, darkTheme = false), lightColorScheme())
    }

    @Test
    fun darkSchemeTakesItsRolesFromTheDarkPalette() {
        assertRoles(OnDarkColorsPalette, colorSchemeFor(OnDarkColorsPalette, darkTheme = true), darkColorScheme())
    }

    private fun assertRoles(palette: CustomColorsPalette, scheme: ColorScheme, baseline: ColorScheme) {
        assertEquals(palette.backgroundColor, scheme.background, "background")
        assertEquals(palette.backgroundColor, scheme.surface, "surface")
        assertEquals(palette.textPrimary, scheme.onBackground, "onBackground")
        assertEquals(palette.textPrimary, scheme.onSurface, "onSurface")
        assertEquals(palette.textSecondary, scheme.onSurfaceVariant, "onSurfaceVariant")
        assertEquals(palette.tintPrimary, scheme.primary, "primary")
        assertEquals(palette.backgroundColor, scheme.onPrimary, "onPrimary")
        assertEquals(palette.textFieldUnFocusedBorder, scheme.outline, "outline")
        // Raised surfaces step away from the background; the lowest is the background itself
        assertEquals(palette.backgroundColor, scheme.surfaceContainerLowest, "surfaceContainerLowest")
        assertEquals(scheme.surfaceContainerHighest, scheme.surfaceVariant, "surfaceVariant")
        val steps = listOf(
            scheme.surfaceContainerLowest,
            scheme.surfaceContainerLow,
            scheme.surfaceContainer,
            scheme.surfaceContainerHigh,
            scheme.surfaceContainerHighest
        )
        val distances = steps.map { abs(it.luminance() - palette.backgroundColor.luminance()) }
        assertEquals(distances.sorted(), distances, "each step moves further from the background")
        assertEquals(steps.size, steps.toSet().size, "no two steps are the same")
        // Roles the palette has no colour for keep the light or dark baseline
        assertEquals(baseline.error, scheme.error, "error")
        assertEquals(baseline.secondary, scheme.secondary, "secondary")
    }
}
