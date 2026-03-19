package com.securevault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.securevault.ui.animation.AnimationTokens

@Composable
fun MyAppDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(AnimationTokens.dialogDuration)) +
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(AnimationTokens.dialogDuration, easing = AnimationTokens.easeOutBack)
                ),
        exit = fadeOut(tween(AnimationTokens.crossFadeDuration)) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(AnimationTokens.crossFadeDuration))
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        confirmText,
                        color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissText, style = MaterialTheme.typography.labelLarge)
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
fun SvConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String = "确认",
    dismissText: String = "取消",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false,
) {
    MyAppDialog(
        visible = visible,
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDanger = isDanger,
    )
}
