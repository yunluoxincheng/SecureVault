package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    biometricEnabled: Boolean,
    screenshotAllowed: Boolean,
    errorMessage: String?,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onScreenshotAllowedChange: (Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    onLock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
            Text("设置", style = MaterialTheme.typography.headlineSmall)
        }

        Text("主题", style = MaterialTheme.typography.titleMedium)

        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = currentTheme == mode, onClick = { onThemeChange(mode) })
                Text(
                    text = when (mode) {
                        ThemeMode.System -> "跟随系统"
                        ThemeMode.Light -> "浅色"
                        ThemeMode.Dark -> "深色"
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用生物识别", modifier = Modifier.weight(1f))
            Switch(checked = biometricEnabled, onCheckedChange = onBiometricChange)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("允许应用内截图", modifier = Modifier.weight(1f))
            Switch(checked = screenshotAllowed, onCheckedChange = onScreenshotAllowedChange)
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onLock,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("立即锁定")
        }
    }
}
