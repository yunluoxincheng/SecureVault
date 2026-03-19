package com.securevault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.theme.elevation
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Button style variants.
 *
 * [Primary]   – filled, strong CTA (unlock, save, confirm)
 * [Secondary] – outlined, secondary action (cancel, edit)
 * [Ghost]     – borderless, low-emphasis action (biometric login, "forgot?")
 * [Text]      – alias for [Ghost], kept for backward compatibility
 * [Danger]    – filled with error color (delete, revoke)
 */
enum class MyAppButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Text,
    Danger,
}

/**
 * Unified button component for SecureVault.
 *
 * Provides consistent press-scale feedback via [animateFloatAsState] backed
 * by a shared [MutableInteractionSource], so the animation is tied to the
 * actual pointer event rather than a parallel gesture detector.
 *
 * @param text         Label displayed inside the button.
 * @param onClick      Called when the button is tapped (no-op while [isLoading]).
 * @param modifier     Applied to the outer container (scale transform included here).
 * @param variant      Visual style; defaults to [MyAppButtonVariant.Primary].
 * @param enabled      When false the button is non-interactive and dimmed.
 * @param isLoading    Replaces the label with a [CircularProgressIndicator] and
 *                     disables interaction. Only rendered for Primary / Danger.
 * @param leadingIcon  Optional icon drawn before the label.
 */
@Composable
fun MyAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MyAppButtonVariant = MyAppButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactive = enabled && !isLoading

    val scale by animateFloatAsState(
        targetValue = if (isPressed && interactive) 0.985f else 1f,
        animationSpec = AnimationTokens.buttonPressSpring,
        label = "buttonPressScale",
    )

    val scaledModifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    when (variant) {
        MyAppButtonVariant.Primary -> PrimaryButton(
            text = text,
            onClick = onClick,
            modifier = scaledModifier,
            enabled = interactive,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            interactionSource = interactionSource,
        )

        MyAppButtonVariant.Secondary -> SecondaryButton(
            text = text,
            onClick = onClick,
            modifier = scaledModifier,
            enabled = interactive,
            leadingIcon = leadingIcon,
            interactionSource = interactionSource,
        )

        MyAppButtonVariant.Ghost,
        MyAppButtonVariant.Text,
        -> GhostButton(
            text = text,
            onClick = onClick,
            modifier = scaledModifier,
            enabled = interactive,
            leadingIcon = leadingIcon,
            interactionSource = interactionSource,
        )

        MyAppButtonVariant.Danger -> DangerButton(
            text = text,
            onClick = onClick,
            modifier = scaledModifier,
            enabled = interactive,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            interactionSource = interactionSource,
        )
    }
}

// ---------------------------------------------------------------------------
// Variant implementations (internal)
// ---------------------------------------------------------------------------

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    interactionSource: MutableInteractionSource,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(MaterialTheme.layout.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = MaterialTheme.elevation.medium,
            pressedElevation = MaterialTheme.elevation.low,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.md, vertical = 0.dp),
        interactionSource = interactionSource,
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            progressColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    leadingIcon: ImageVector?,
    interactionSource: MutableInteractionSource,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(MaterialTheme.layout.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled)
                MaterialTheme.colorScheme.outlineVariant
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.md, vertical = 0.dp),
        interactionSource = interactionSource,
    ) {
        ButtonContent(
            text = text,
            isLoading = false,
            leadingIcon = leadingIcon,
            progressColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    leadingIcon: ImageVector?,
    interactionSource: MutableInteractionSource,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(MaterialTheme.layout.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.sm, vertical = 0.dp),
        interactionSource = interactionSource,
    ) {
        ButtonContent(
            text = text,
            isLoading = false,
            leadingIcon = leadingIcon,
            progressColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    interactionSource: MutableInteractionSource,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(MaterialTheme.layout.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = MaterialTheme.elevation.medium,
            pressedElevation = MaterialTheme.elevation.low,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.md, vertical = 0.dp),
        interactionSource = interactionSource,
    ) {
        ButtonContent(
            text = text,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            progressColor = MaterialTheme.colorScheme.onError,
        )
    }
}

// ---------------------------------------------------------------------------
// Shared content slot
// ---------------------------------------------------------------------------

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: ImageVector?,
    progressColor: androidx.compose.ui.graphics.Color,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(MaterialTheme.layout.buttonIconSize),
            color = progressColor,
            strokeWidth = MaterialTheme.layout.buttonProgressStrokeWidth,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.layout.buttonIconSize),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
