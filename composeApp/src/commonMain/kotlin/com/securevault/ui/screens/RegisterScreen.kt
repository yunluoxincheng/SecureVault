package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.components.SvButton
import com.securevault.ui.components.SvPasswordTextField
import com.securevault.ui.components.SvTextButton
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.md)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(
            text = "创建保险库",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xl),
        )
        Text(
            text = "设置一个强主密码，这是保护你所有密码的钥匙",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SvPasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = "设置主密码",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )

        PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())

        if (passwordHints.isNotEmpty()) {
            Text(
                text = "建议：${passwordHints.joinToString("、")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SvPasswordTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "确认主密码",
            modifier = Modifier.fillMaxWidth(),
            isError = confirmPassword.isNotBlank() && !valid,
            supportingText = if (confirmPassword.isNotBlank() && password != confirmPassword) "两次密码不一致" else null,
            enabled = !isLoading,
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        SvButton(
            text = if (isLoading) "创建中…" else "创建保险库",
            onClick = { onRegister(password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = valid,
            isLoading = isLoading,
        )

        SvTextButton(
            text = "已有帐号？登录",
            onClick = onGoLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        )
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
