package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

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
    var showPassword by remember { mutableStateOf(false) }
    var biometricAutoTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(biometricAvailable, isLoading) {
        if (biometricAvailable && !isLoading && !biometricAutoTriggered) {
            biometricAutoTriggered = true
            onBiometricLogin()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "登录", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("主密码") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onLogin(password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotBlank() && !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isLoading) "登录中..." else "登录")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "—— 或 ——", style = MaterialTheme.typography.bodySmall)

        TextButton(
            onClick = onBiometricLogin,
            enabled = biometricAvailable && !isLoading,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("生物识别登录")
        }

        TextButton(onClick = onGoRegister, enabled = !isLoading, shape = RoundedCornerShape(16.dp)) {
            Text("没有帐号？注册")
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
