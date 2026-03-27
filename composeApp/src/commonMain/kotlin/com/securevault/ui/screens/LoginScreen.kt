package com.securevault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.icons.LOGO_SCREEN_SCALE
import com.securevault.ui.icons.SecureVaultLogoIcon
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    biometricAvailable: Boolean,
    autoBiometricOnEnter: Boolean = true,
    isAppForeground: Boolean = true,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String) -> Unit,
    onBiometricLogin: () -> Unit,
    onGoRegister: () -> Unit,
    onImportUserData: () -> Unit = {},
    showImportUserDataPasswordDialog: Boolean = false,
    onConfirmImportUserDataPassword: (String) -> Unit = {},
    onDismissImportUserDataPasswordDialog: () -> Unit = {},
) {
    var password by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var biometricAutoTriggered by remember(autoBiometricOnEnter) { mutableStateOf(!autoBiometricOnEnter) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    LaunchedEffect(biometricAvailable, isLoading, autoBiometricOnEnter, isAppForeground) {
        if (!autoBiometricOnEnter) return@LaunchedEffect
        if (!isAppForeground) return@LaunchedEffect
        if (biometricAvailable && !isLoading && !biometricAutoTriggered) {
            // Delay auto biometric until Login screen is visually stable.
            // Triggering too early during route/lifecycle transition can hang prompt flow.
            delay(AUTO_BIOMETRIC_TRIGGER_DELAY_MS)
            if (!isAppForeground || isLoading || !biometricAvailable || biometricAutoTriggered) return@LaunchedEffect
            biometricAutoTriggered = true
            onBiometricLogin()
        }
    }

    if (showImportUserDataPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                importPassword = ""
                onDismissImportUserDataPasswordDialog()
            },
            title = { Text("导入用户数据") },
            text = {
                MyAppInput(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = "主密码",
                    isPassword = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmImportUserDataPassword(importPassword)
                        importPassword = ""
                    },
                    enabled = importPassword.isNotBlank() && !isLoading,
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        importPassword = ""
                        onDismissImportUserDataPasswordDialog()
                    },
                    enabled = !isLoading,
                ) {
                    Text("取消")
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.sectionSpacing),
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(AnimationTokens.pageEnterDuration)) +
                    slideInVertically(
                        initialOffsetY = { -it / 4 },
                        animationSpec = tween(AnimationTokens.pageEnterDuration, easing = AnimationTokens.easeOut)
                    ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SecureVaultLogoIcon(
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.layout.heroIconSize * LOGO_SCREEN_SCALE),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Text(
                        text = "SecureVault",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "输入主密码解锁",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                    )
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(AnimationTokens.pageEnterDuration, delayMillis = 80)) +
                    slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = tween(AnimationTokens.pageEnterDuration, easing = AnimationTokens.easeOut)
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = MaterialTheme.layout.authFormMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
                ) {
                    MyAppInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "主密码",
                        leadingIcon = Icons.Default.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        isPassword = true,
                    )

                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn(tween(AnimationTokens.crossFadeDuration)) +
                            expandVertically(animationSpec = tween(AnimationTokens.crossFadeDuration)),
                        exit = fadeOut(tween(AnimationTokens.crossFadeDuration)) +
                            shrinkVertically(animationSpec = tween(AnimationTokens.crossFadeDuration)),
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    MyAppButton(
                        text = if (isLoading) "解锁中…" else "解锁",
                        onClick = { onLogin(password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = password.isNotBlank(),
                        isLoading = isLoading,
                    )

                    Text(
                        text = "或使用其他方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )

                    if (biometricAvailable) {
                        MyAppButton(
                            text = "使用生物识别",
                            onClick = onBiometricLogin,
                            enabled = !isLoading,
                            leadingIcon = Icons.Default.Fingerprint,
                            variant = MyAppButtonVariant.Text,
                        )
                    }

                    MyAppButton(
                        text = "没有帐号？注册",
                        onClick = onGoRegister,
                        enabled = !isLoading,
                        variant = MyAppButtonVariant.Text,
                    )

                    MyAppButton(
                        text = "导入用户数据",
                        onClick = onImportUserData,
                        enabled = !isLoading,
                        variant = MyAppButtonVariant.Text,
                    )
                }
            }
        }
    }
}

private const val AUTO_BIOMETRIC_TRIGGER_DELAY_MS = 450L
