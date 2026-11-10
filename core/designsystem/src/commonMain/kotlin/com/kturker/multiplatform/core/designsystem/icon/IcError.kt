@file:Suppress("MagicNumber")

package com.kturker.multiplatform.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IcError: ImageVector?
    get() {
        if (icError != null) {
            return icError
        }
        icError = ImageVector.Builder(
            name = "icError",
            defaultWidth = 800.dp,
            defaultHeight = 800.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
                arcToRelative(9f, 9f, 0f, true, true, 18f, 0f)
                close()
                moveTo(12f, 7.5f)
                lineTo(12f, 13f)
                moveTo(12f, 16.5f)
                lineTo(12.01f, 16.5f)
            }
        }.build()

        return icError
    }

private var icError: ImageVector? = null
