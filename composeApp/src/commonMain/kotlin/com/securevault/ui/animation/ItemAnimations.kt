package com.securevault.ui.animation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

fun Modifier.animateItemEntrance(
    index: Int = 0,
    durationMillis: Int = AnimationTokens.cardAppearDuration,
    initialOffsetPx: Int = AnimationTokens.itemEntranceOffsetPx,
    alphaEasing: Easing = AnimationTokens.easeOut,
    translationEasing: Easing = AnimationTokens.easeOut,
    enabled: Boolean = true,
    resetKey: Any? = Unit,
    onAnimationStarted: (() -> Unit)? = null,
): Modifier = composed {
    var visible by remember(resetKey, enabled) { mutableStateOf(!enabled) }
    var animationStarted by remember(resetKey, enabled) { mutableStateOf(false) }

    LaunchedEffect(resetKey, enabled, index) {
        if (!enabled) {
            visible = true
            return@LaunchedEffect
        }
        if (animationStarted) return@LaunchedEffect

        animationStarted = true
        delay((index * AnimationTokens.staggerItemDelay).toLong())
        visible = true
        onAnimationStarted?.invoke()
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = alphaEasing
        ),
        label = "itemAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else initialOffsetPx.toFloat(),
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = translationEasing
        ),
        label = "itemTranslationY"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
