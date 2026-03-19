package com.securevault.ui.animation

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

fun Modifier.animateItemEntrance(index: Int = 0): Modifier = composed {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * AnimationTokens.staggerItemDelay).toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationTokens.cardAppearDuration,
            easing = AnimationTokens.easeOut
        ),
        label = "itemAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else AnimationTokens.itemEntranceOffsetPx.toFloat(),
        animationSpec = tween(
            durationMillis = AnimationTokens.cardAppearDuration,
            easing = AnimationTokens.easeOut
        ),
        label = "itemTranslationY"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
