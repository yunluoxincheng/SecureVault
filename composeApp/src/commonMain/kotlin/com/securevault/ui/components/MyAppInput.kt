package com.securevault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.radius
import com.securevault.ui.theme.spacing

private const val INPUT_ANIM_MS = 180

/**
 * Primary input field for the app.
 *
 * Features:
 * - Floating label + optional placeholder
 * - Animated border color + subtle glow halo on focus
 * - Error state with animated message reveal
 * - Password mode with crossfade visibility toggle
 * - Supports both self-managed and externally-controlled password visibility
 */
@Composable
fun MyAppInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isPassword: Boolean = false,
    showPassword: Boolean? = null,
    onTogglePasswordVisibility: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var internalPasswordVisible by remember { mutableStateOf(false) }
    val isPasswordVisible = showPassword ?: internalPasswordVisible

    // --- Color tokens ---
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorError = MaterialTheme.colorScheme.error
    val colorOutlineVariant = MaterialTheme.colorScheme.outlineVariant
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val restingContainer = MaterialTheme.colorScheme.surfaceContainerLow
    val focusedContainer = MaterialTheme.colorScheme.surfaceContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.16f)

    // Border: error > focused > resting
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colorError
            isFocused -> colorPrimary.copy(alpha = 0.92f)
            else -> colorOutlineVariant.copy(alpha = 0.9f)
        },
        animationSpec = tween(INPUT_ANIM_MS),
        label = "inputBorder",
    )

    // Label: matches border state
    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> colorError
            isFocused -> colorPrimary.copy(alpha = 0.92f)
            else -> colorOnSurfaceVariant
        },
        animationSpec = tween(INPUT_ANIM_MS),
        label = "inputLabel",
    )

    // Leading icon tint follows focus / error
    val leadingTint by animateColorAsState(
        targetValue = when {
            isError -> colorError
            isFocused -> colorPrimary.copy(alpha = 0.92f)
            else -> colorOnSurfaceVariant
        },
        animationSpec = tween(INPUT_ANIM_MS),
        label = "inputLeadingIcon",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isError -> errorContainer
            isFocused -> focusedContainer
            else -> restingContainer
        },
        animationSpec = tween(INPUT_ANIM_MS),
        label = "inputContainer",
    )

    // Glow halo alpha — subtle, security-appropriate
    val glowAlpha by animateFloatAsState(
        targetValue = when {
            isError -> 0.05f
            isFocused -> 0.07f
            else -> 0f
        },
        animationSpec = tween(INPUT_ANIM_MS),
        label = "inputGlow",
    )

    val glowColor = if (isError) colorError else colorPrimary
    val glowSpread = 4.dp
    val cornerRadius = MaterialTheme.radius.lg

    // --- Password support ---
    val visualTransformation =
        if (isPassword && !isPasswordVisible) PasswordVisualTransformation()
        else VisualTransformation.None

    val resolvedKeyboardOptions =
        if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)
        else keyboardOptions

    val resolvedTrailingIcon: @Composable (() -> Unit)? = when {
        isPassword -> {
            {
                val toggleAction = onTogglePasswordVisibility
                    ?: { internalPasswordVisible = !internalPasswordVisible }
                IconButton(onClick = toggleAction) {
                    Crossfade(
                        targetState = isPasswordVisible,
                        animationSpec = tween(INPUT_ANIM_MS),
                        label = "passwordEye",
                    ) { visible ->
                        Icon(
                            imageVector = if (visible) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (visible) "Hide password" else "Show password",
                            tint = leadingTint,
                        )
                    }
                }
            }
        }
        trailingIcon != null -> trailingIcon
        else -> null
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder != null) {
                { Text(placeholder, style = MaterialTheme.typography.bodyLarge) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MaterialTheme.layout.inputHeight)
                .onFocusChanged { isFocused = it.isFocused }
                // Glow halo drawn behind the field boundary
                .drawBehind {
                    val spread = glowSpread.toPx()
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + spread * 2f, size.height + spread * 2f),
                        cornerRadius = CornerRadius((cornerRadius + glowSpread).toPx()),
                    )
                },
            shape = MaterialTheme.shapes.large,
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = leadingTint) }
            } else null,
            trailingIcon = resolvedTrailingIcon,
            keyboardOptions = resolvedKeyboardOptions,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                errorContainerColor = containerColor,
                disabledContainerColor = restingContainer,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                errorBorderColor = borderColor,
                focusedLabelColor = labelColor,
                unfocusedLabelColor = labelColor,
                errorLabelColor = labelColor,
                focusedLeadingIconColor = leadingTint,
                unfocusedLeadingIconColor = leadingTint,
                errorLeadingIconColor = leadingTint,
            ),
        )

        // Animated error message below the field
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn(tween(INPUT_ANIM_MS)) + expandVertically(tween(INPUT_ANIM_MS)),
            exit = fadeOut(tween(INPUT_ANIM_MS)) + shrinkVertically(tween(INPUT_ANIM_MS)),
        ) {
            Text(
                text = errorMessage.orEmpty(),
                color = colorError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = MaterialTheme.spacing.md, top = MaterialTheme.spacing.xs),
            )
        }

        AnimatedVisibility(
            visible = !isError && supportingText != null,
            enter = fadeIn(tween(INPUT_ANIM_MS)) + expandVertically(tween(INPUT_ANIM_MS)),
            exit = fadeOut(tween(INPUT_ANIM_MS)) + shrinkVertically(tween(INPUT_ANIM_MS)),
        ) {
            Text(
                text = supportingText.orEmpty(),
                color = colorOnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = MaterialTheme.spacing.md, top = MaterialTheme.spacing.xs),
            )
        }
    }
}
