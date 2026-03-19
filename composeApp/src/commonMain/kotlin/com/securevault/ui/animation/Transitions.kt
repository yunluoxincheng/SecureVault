package com.securevault.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object NavTransitions {
    val enterForward: EnterTransition =
        fadeIn(animationSpec = AnimationTokens.pageEnterTween()) +
            slideInHorizontally(
                initialOffsetX = { it / 8 },
                animationSpec = AnimationTokens.pageEnterTween()
            )

    val exitForward: ExitTransition =
        fadeOut(animationSpec = AnimationTokens.pageExitTween()) +
            slideOutHorizontally(
                targetOffsetX = { -it / 8 },
                animationSpec = AnimationTokens.pageExitTween()
            )

    val enterBackward: EnterTransition =
        fadeIn(animationSpec = AnimationTokens.pageEnterTween()) +
            slideInHorizontally(
                initialOffsetX = { -it / 8 },
                animationSpec = AnimationTokens.pageEnterTween()
            )

    val exitBackward: ExitTransition =
        fadeOut(animationSpec = AnimationTokens.pageExitTween()) +
            slideOutHorizontally(
                targetOffsetX = { it / 8 },
                animationSpec = AnimationTokens.pageExitTween()
            )

    val enterTab: EnterTransition = fadeIn(animationSpec = AnimationTokens.crossFadeTween())

    val exitTab: ExitTransition = fadeOut(animationSpec = AnimationTokens.crossFadeTween())
}
