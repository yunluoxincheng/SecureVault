package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.securevault.ui.components.PasswordStrengthBar

@Composable
fun RegisterScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRegister: (String) -> Unit,
    onGoLogin: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    val valid = password.length >= 8 && password == confirmPassword
    val passwordHints = remember(password) { buildPasswordHints(password) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("注册", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("设置主密码") },
            visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        PasswordStrengthBar(password = password, modifier = Modifier.fillMaxWidth())
        if (passwordHints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "建议：${passwordHints.joinToString("，")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("确认主密码") },
            visualTransformation = if (showConfirmPassword) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(
                        imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirmPassword) "隐藏密码" else "显示密码"
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (confirmPassword.isNotBlank() && !valid) {
            Text("密码至少 8 位且两次输入一致", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { onRegister(password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = valid && !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isLoading) "创建中..." else "创建保险库")
        }

        TextButton(
            onClick = onGoLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("已有帐号？登录")
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
