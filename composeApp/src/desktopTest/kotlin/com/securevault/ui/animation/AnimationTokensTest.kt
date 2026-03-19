package com.securevault.ui.animation

import kotlin.test.Test
import kotlin.test.assertTrue

class AnimationTokensTest {

    @Test
    fun primaryUiAnimationDurations_stayWithinStandardRange() {
        val standardizedDurations = listOf(
            AnimationTokens.pageEnterDuration,
            AnimationTokens.pageExitDuration,
            AnimationTokens.cardAppearDuration,
            AnimationTokens.dialogDuration,
            AnimationTokens.crossFadeDuration,
            AnimationTokens.copyFeedbackDuration,
            AnimationTokens.strengthBarDuration,
        )

        standardizedDurations.forEach { duration ->
            assertTrue(
                actual = duration in 150..250,
                message = "Expected standardized UI duration in 150..250ms, got ${duration}ms",
            )
        }
    }
}
