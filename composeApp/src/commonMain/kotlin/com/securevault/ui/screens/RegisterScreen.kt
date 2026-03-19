package com.securevault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.securevault.ui.animation.AnimationTokens
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRegister: (String) -> Unit,
    onGoLogin: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val valid = password.length >= 8 && password == confirmPassword
    val passwordHints = remember(password) { buildPasswordHints(password) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .verticalScroll(scrollState)
                .padding(bottom = MaterialTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.contentSpacing),
        ) {
            Text(
                text = "创建保险库",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = MaterialTheme.layout.sectionSpacing),
            )
            Text(
                text = "设置一个强主密码，这是保护你所有密码的钥匙",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm),
            )

            MyAppInput(
                value = password,
                onValueChange = { password = it },
                label = "设置主密码",
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isPassword = true,
            )

            PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())

            AnimatedVisibility(
                visible = passwordHints.isNotEmpty(),
                enter = fadeIn(tween(AnimationTokens.crossFadeDuration)) +
                    expandVertically(animationSpec = tween(AnimationTokens.crossFadeDuration)),
                exit = fadeOut(tween(AnimationTokens.crossFadeDuration)) +
                    shrinkVertically(animationSpec = tween(AnimationTokens.crossFadeDuration)),
            ) {
                Text(
                    text = "建议：${passwordHints.joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            MyAppInput(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "确认主密码",
                modifier = Modifier.fillMaxWidth(),
                isError = confirmPassword.isNotBlank() && !valid,
                supportingText = if (confirmPassword.isNotBlank() && password != confirmPassword) "两次密码不一致" else null,
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
                )
            }

            MyAppButton(
                text = if (isLoading) "创建中…" else "创建保险库",
                onClick = { onRegister(password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = valid,
                isLoading = isLoading,
            )

            MyAppButton(
                text = "已有帐号？登录",
                onClick = onGoLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                variant = MyAppButtonVariant.Text,
            )
        }
    }
}

private fun buildPasswordHints(password: String): List<String> {
    if (password.isEmpty()) return listOf("至少 12 位，包含大小写、数字和符号")
    val hints = mutableListOf<String>()
    if (password.length < 12) hints += "长度建议至少 12 位"
    if (!password.any { it.isUpperCase() }) hints += "增加大写字母"
    if (!password.any { it.isLowerCase() }) hints += "增加小写字母"
    if (!password.any { it.isDigit() }) hints += "增加数字"
    if (!password.any { !it.isLetterOrDigit() }) hints += "增加符号"
    return hints
}
