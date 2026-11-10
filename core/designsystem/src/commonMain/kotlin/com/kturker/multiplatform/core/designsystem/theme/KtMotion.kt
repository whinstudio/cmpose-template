package com.kturker.multiplatform.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/** Motion tokens: exits are shorter than entrances. */
object KtMotion {
    private val EmphasizedEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Small state changes: toggles, pressed states. */
    fun <T> fast(): TweenSpec<T> = tween(durationMillis = 120, easing = FastOutSlowInEasing)

    /** Default entrance: content swaps, fades in. */
    fun <T> base(): TweenSpec<T> = tween(durationMillis = 240, easing = FastOutSlowInEasing)

    /** Large or emphasized entrances. */
    fun <T> slow(): TweenSpec<T> = tween(durationMillis = 450, easing = EmphasizedEasing)

    /** Leaving content. */
    fun <T> exit(): TweenSpec<T> = tween(durationMillis = 120, easing = FastOutLinearInEasing)
}
