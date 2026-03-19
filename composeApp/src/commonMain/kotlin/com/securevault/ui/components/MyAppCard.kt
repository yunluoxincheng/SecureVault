package com.securevault.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.theme.elevation
import com.securevault.ui.theme.layout

enum class MyAppCardVariant {
    Filled,
    Elevated,
    Outlined,
}

/**
 * Default content padding sourced from layout tokens.
 * Callers that manage their own padding should pass [PaddingValues(0.dp)].
 */
@Composable
private fun defaultCardPadding(): PaddingValues {
    val lt = MaterialTheme.layout
    return PaddingValues(
        horizontal = lt.cardPaddingHorizontal,
        vertical = lt.cardPaddingVertical,
    )
}

@Composable
fun MyAppCard(
    modifier: Modifier = Modifier,
    variant: MyAppCardVariant = MyAppCardVariant.Filled,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ev = MaterialTheme.elevation
    val shape = MaterialTheme.shapes.medium

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = when (variant) {
        MyAppCardVariant.Filled -> colors.surfaceContainer
        MyAppCardVariant.Elevated -> colors.surface
        MyAppCardVariant.Outlined -> colors.surface
    }

    val tonalElevation: Dp = when (variant) {
        MyAppCardVariant.Elevated -> ev.low
        else -> 0.dp
    }

    val subtleBorder = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f))
    val border: BorderStroke? = when (variant) {
        MyAppCardVariant.Elevated,
        MyAppCardVariant.Outlined,
        -> subtleBorder
        MyAppCardVariant.Filled -> null
    }

    val targetShadow: Dp = when {
        variant == MyAppCardVariant.Elevated && onClick != null && isPressed -> ev.none
        variant == MyAppCardVariant.Elevated -> ev.low
        else -> 0.dp
    }

    val shadowElevation by animateDpAsState(
        targetValue = targetShadow,
        animationSpec = tween(
            durationMillis = AnimationTokens.crossFadeDuration,
            easing = AnimationTokens.easeOut,
        ),
        label = "cardShadow",
    )

    val resolvedPadding = contentPadding ?: defaultCardPadding()

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            interactionSource = interactionSource,
        ) {
            Column(modifier = Modifier.padding(resolvedPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
        ) {
            Column(modifier = Modifier.padding(resolvedPadding), content = content)
        }
    }
}
