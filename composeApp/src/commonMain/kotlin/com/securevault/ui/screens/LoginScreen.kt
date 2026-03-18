package com.securevault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.SvButton
import com.securevault.ui.components.SvPasswordTextField
import com.securevault.ui.components.SvTextButton
import com.securevault.ui.theme.spacing

@Composable
fun LoginScreen(
    biometricAvailable: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (String) -> Unit,
    onBiometricLogin: () -> Unit,
    onGoRegister: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var biometricAutoTriggered by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    LaunchedEffect(biometricAvailable, isLoading) {
        if (biometricAvailable && !isLoading && !biometricAutoTriggered) {
            biometricAutoTriggered = true
            onBiometricLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(AnimationTokens.pageEnterDuration)) +
                    slideInVertically(
                        initialOffsetY = { -it / 3 },
                        animationSpec = tween(AnimationTokens.pageEnterDuration, easing = AnimationTokens.easeOut)
                    ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
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
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(AnimationTokens.pageEnterDuration, delayMillis = 100)) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(AnimationTokens.pageEnterDuration, easing = AnimationTokens.easeOut)
                    ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                SvPasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "主密码",
                    leadingIcon = Icons.Default.Lock,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )

                SvButton(
                    text = if (isLoading) "解锁中…" else "解锁",
                    onClick = { onLogin(password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.isNotBlank(),
                    isLoading = isLoading,
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = "— 或 —",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                if (biometricAvailable) {
                    SvTextButton(
                        text = "使用生物识别",
                        onClick = onBiometricLogin,
                        enabled = !isLoading,
                        leadingIcon = Icons.Default.Fingerprint,
                    )
                }

                SvTextButton(
                    text = "没有帐号？注册",
                    onClick = onGoRegister,
                    enabled = !isLoading,
                )
            }
        }
    }
}
