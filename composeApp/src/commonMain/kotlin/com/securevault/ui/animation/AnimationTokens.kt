package com.securevault.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AnimationTokens {
    const val pageEnterDuration = 280
    const val pageExitDuration = 220
    const val cardAppearDuration = 220
    const val dialogDuration = 240
    const val crossFadeDuration = 160
    const val copyFeedbackDuration = 260
    const val strengthBarDuration = 360
    const val unlockDuration = 500
    const val staggerItemDelay = 36
    const val itemEntranceOffsetPx = 16

    val easeOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val easeIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val easeInOut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val easeOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    fun <T> pageEnterTween() = tween<T>(durationMillis = pageEnterDuration, easing = easeOut)
    fun <T> pageExitTween() = tween<T>(durationMillis = pageExitDuration, easing = easeIn)
    fun <T> cardAppearTween() = tween<T>(durationMillis = cardAppearDuration, easing = easeOut)
    fun <T> dialogTween() = tween<T>(durationMillis = dialogDuration, easing = easeOutBack)
    fun <T> crossFadeTween() = tween<T>(durationMillis = crossFadeDuration)

    val buttonPressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}
