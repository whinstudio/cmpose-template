@file:Suppress("MagicNumber")

package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomColorsPalette(
    val backgroundColor: Color = Color.Unspecified,
    val textPrimary: Color = Color.Unspecified,
    val textSecondary: Color = Color.Unspecified,
    val tintPrimary: Color = Color.Unspecified,
    val tintSecondary: Color = Color.Unspecified,
    val toggleBackground: Color = Color.Unspecified,
    val toggleSelectedBackground: Color = Color.Unspecified,
    val toggleUnSelectedText: Color = Color.Unspecified,
    val toggleSelectedText: Color = Color.Unspecified,
    val textFieldFocusedBorder: Color = Color.Unspecified,
    val textFieldUnFocusedBorder: Color = Color.Unspecified,
    val textFieldLabelColor: Color = Color.Unspecified,
    val textFieldTextColor: Color = Color.Unspecified,
    val textFieldCursorColor: Color = Color.Unspecified
)

val LocalColorsPalette = staticCompositionLocalOf { CustomColorsPalette() }

val OnLightColorsPalette = CustomColorsPalette(
    backgroundColor = Color(color = 0xFFFFFFFF),
    textPrimary = Color(color = 0xFF080B0E),
    textSecondary = Color(color = 0xFF646E79),
    tintSecondary = Color(color = 0xFF8B959E),
    tintPrimary = Color(color = 0xFF080B0E),
    toggleBackground = Color(color = 0xFFF2F2F2),
    toggleSelectedBackground = Color.Black,
    toggleUnSelectedText = Color.Black,
    toggleSelectedText = Color.White,
    textFieldFocusedBorder = Color(color = 0xFF080B0E),
    textFieldUnFocusedBorder = Color(color = 0xFF888888),
    textFieldLabelColor = Color(color = 0xFF666666),
    textFieldTextColor = Color(color = 0xFF000000),
    textFieldCursorColor = Color(color = 0xFF000000)
)

val OnDarkColorsPalette = CustomColorsPalette(
    backgroundColor = Color(color = 0xFF202326),
    textPrimary = Color(color = 0xFFFFFFFF),
    textSecondary = Color(color = 0x80FFFFFF),
    tintPrimary = Color(color = 0xFFFFFFFF),
    tintSecondary = Color(color = 0xFF8B959E),
    toggleBackground = Color(color = 0xFF2A2A2A),
    toggleSelectedBackground = Color.White,
    toggleUnSelectedText = Color.White,
    toggleSelectedText = Color.Black,
    textFieldFocusedBorder = Color(color = 0xFFFFFFFF),
    textFieldUnFocusedBorder = Color(color = 0xFF7A7A7A),
    textFieldLabelColor = Color(color = 0xFF909090),
    textFieldTextColor = Color(color = 0xFFFFFFFF),
    textFieldCursorColor = Color(color = 0xFFFFFFFF)
)
