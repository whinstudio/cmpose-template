package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class PaletteContrastTest {

    private val palettes = listOf("light" to OnLightColorsPalette, "dark" to OnDarkColorsPalette)

    private fun contrast(foreground: Color, background: Color): Float {
        val fg = foreground.compositeOver(background).luminance()
        val bg = background.luminance()
        return (max(fg, bg) + 0.05f) / (min(fg, bg) + 0.05f)
    }

    // Every failing pair in one message, so one run shows them all
    private fun assertPairs(minimum: Float, pairs: (CustomColorsPalette) -> Map<String, Pair<Color, Color>>) {
        val failures = palettes.flatMap { (mode, palette) ->
            pairs(palette).mapNotNull { (name, colors) ->
                val ratio = contrast(colors.first, colors.second)
                "$mode $name is $ratio".takeIf { ratio < minimum }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun textReachesFourPointFive() = assertPairs(4.5f) { p ->
        mapOf(
            "textPrimary" to (p.textPrimary to p.backgroundColor),
            "textSecondary" to (p.textSecondary to p.backgroundColor),
            "textFieldLabelColor" to (p.textFieldLabelColor to p.backgroundColor),
            "textFieldTextColor" to (p.textFieldTextColor to p.backgroundColor),
            "toggleSelectedText" to (p.toggleSelectedText to p.toggleSelectedBackground),
            "toggleUnSelectedText" to (p.toggleUnSelectedText to p.toggleBackground),
            "onPrimary" to (p.backgroundColor to p.tintPrimary)
        )
    }

    @Test
    fun inputEdgesAndIconsReachThree() = assertPairs(3f) { p ->
        mapOf(
            "textFieldUnFocusedBorder" to (p.textFieldUnFocusedBorder to p.backgroundColor),
            "textFieldFocusedBorder" to (p.textFieldFocusedBorder to p.backgroundColor),
            "tintSecondary" to (p.tintSecondary to p.backgroundColor)
        )
    }

    @Test
    fun aFocusedFieldDiffersFromAnUnfocusedOne() = assertPairs(3f) { p ->
        mapOf("focused against unfocused" to (p.textFieldFocusedBorder to p.textFieldUnFocusedBorder))
    }

    @Test
    fun textStaysReadableOnEveryRaisedSurface() {
        val failures = palettes.flatMap { (mode, palette) ->
            val scheme = colorSchemeFor(palette, darkTheme = mode == "dark")
            val surfaces = mapOf(
                "surfaceContainerLowest" to scheme.surfaceContainerLowest,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer,
                "surfaceContainerHigh" to scheme.surfaceContainerHigh,
                "surfaceContainerHighest" to scheme.surfaceContainerHighest
            )
            val texts = mapOf("onSurface" to scheme.onSurface, "onSurfaceVariant" to scheme.onSurfaceVariant)
            surfaces.flatMap { (surfaceName, surface) ->
                texts.mapNotNull { (textName, text) ->
                    val ratio = contrast(text, surface)
                    "$mode $textName on $surfaceName is $ratio".takeIf { ratio < 4.5f }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
