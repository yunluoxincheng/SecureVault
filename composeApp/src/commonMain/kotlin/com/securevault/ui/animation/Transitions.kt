package com.securevault.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object NavTransitions {
    val enterForward: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = AnimationTokens.pageEnterDuration,
            easing = AnimationTokens.easeOut
        )
    ) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = tween(
            durationMillis = AnimationTokens.pageEnterDuration,
            easing = AnimationTokens.easeOut
        )
    )

    val exitForward: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = AnimationTokens.pageExitDuration,
            easing = AnimationTokens.easeIn
        )
    ) + slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(
            durationMillis = AnimationTokens.pageExitDuration,
            easing = AnimationTokens.easeIn
        )
    )

    val enterBackward: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = AnimationTokens.pageEnterDuration,
            easing = AnimationTokens.easeOut
        )
    ) + slideInHorizontally(
        initialOffsetX = { -it / 4 },
        animationSpec = tween(
            durationMillis = AnimationTokens.pageEnterDuration,
            easing = AnimationTokens.easeOut
        )
    )

    val exitBackward: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = AnimationTokens.pageExitDuration,
            easing = AnimationTokens.easeIn
        )
    ) + slideOutHorizontally(
        targetOffsetX = { it / 4 },
        animationSpec = tween(
            durationMillis = AnimationTokens.pageExitDuration,
            easing = AnimationTokens.easeIn
        )
    )

    val enterTab: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = AnimationTokens.crossFadeDuration,
            easing = AnimationTokens.easeOut
        )
    )

    val exitTab: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = AnimationTokens.crossFadeDuration,
            easing = AnimationTokens.easeIn
        )
    )
}
