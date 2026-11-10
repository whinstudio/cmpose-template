package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeScaleTest {

    @Test
    fun headlineSmallIsTheScreenTitle() {
        assertEquals(24.sp, KtTypography.headlineSmall.fontSize)
        assertEquals(32.sp, KtTypography.headlineSmall.lineHeight)
    }

    @Test
    fun titleMediumStaysUnderTwenty() {
        assertEquals(18.sp, KtTypography.titleMedium.fontSize)
        assertEquals(24.sp, KtTypography.titleMedium.lineHeight)
    }

    @Test
    fun theAppUsesThreeRadii() {
        val radii = setOf(KtShapes.extraSmall, KtShapes.small, KtShapes.medium, KtShapes.large, KtShapes.extraLarge)
        assertEquals(3, radii.size)
    }

    @Test
    fun exitIsShorterThanItsEntrance() {
        assertTrue(KtMotion.exit<Float>().durationMillis < KtMotion.base<Float>().durationMillis)
    }

    @Test
    fun spacingFollowsTheScale() {
        assertEquals(
            listOf(4.dp, 8.dp, 12.dp, 16.dp, 24.dp, 32.dp, 48.dp),
            listOf(Spacing.xs, Spacing.sm, Spacing.md, Spacing.lg, Spacing.xl, Spacing.xxl, Spacing.xxxl)
        )
    }
}
