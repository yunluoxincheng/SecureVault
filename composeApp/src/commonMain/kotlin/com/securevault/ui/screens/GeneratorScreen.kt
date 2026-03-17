package com.securevault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.util.PasswordGeneratorConfig
import com.securevault.util.PasswordPreset
import com.securevault.util.PasswordStrengthLevel
import com.securevault.viewmodel.GeneratorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    uiState: GeneratorUiState,
    onBack: () -> Unit,
    onGeneratePreset: (PasswordPreset) -> Unit,
    onGenerateCustom: (PasswordGeneratorConfig) -> Unit,
    onCopyGenerated: () -> Unit
) {
    var lengthText by remember(uiState.config.length) { mutableStateOf(uiState.config.length.toString()) }
    var includeUppercase by remember(uiState.config.includeUppercase) { mutableStateOf(uiState.config.includeUppercase) }
    var includeLowercase by remember(uiState.config.includeLowercase) { mutableStateOf(uiState.config.includeLowercase) }
    var includeDigits by remember(uiState.config.includeDigits) { mutableStateOf(uiState.config.includeDigits) }
    var includeSymbols by remember(uiState.config.includeSymbols) { mutableStateOf(uiState.config.includeSymbols) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("密码生成器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("预设", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onGeneratePreset(PasswordPreset.Strong) }) { Text("强") }
                    Button(onClick = { onGeneratePreset(PasswordPreset.Medium) }) { Text("中") }
                    Button(onClick = { onGeneratePreset(PasswordPreset.PinLike) }) { Text("PIN") }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.generatedPassword,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    label = { Text("生成结果") }
                )
            }

            item {
                Button(
                    onClick = onCopyGenerated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("复制并 30 秒自动清除")
                }
            }

            item {
                Text(
                    text = "强度：${strengthLabel(uiState.strength)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            uiState.infoMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Text("自定义", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("长度 (4-64)") }
                )
            }

            item { ToggleRow("包含大写字母", includeUppercase) { includeUppercase = it } }
            item { ToggleRow("包含小写字母", includeLowercase) { includeLowercase = it } }
            item { ToggleRow("包含数字", includeDigits) { includeDigits = it } }
            item { ToggleRow("包含符号", includeSymbols) { includeSymbols = it } }

            item {
                Button(
                    onClick = {
                        val length = lengthText.toIntOrNull() ?: uiState.config.length
                        onGenerateCustom(
                            PasswordGeneratorConfig(
                                length = length,
                                includeUppercase = includeUppercase,
                                includeLowercase = includeLowercase,
                                includeDigits = includeDigits,
                                includeSymbols = includeSymbols
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("生成自定义密码")
                }
            }

            item {
                Text("历史", style = MaterialTheme.typography.titleMedium)
            }

            items(uiState.history) { password ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = password,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

private fun strengthLabel(level: PasswordStrengthLevel): String {
    return when (level) {
        PasswordStrengthLevel.VeryWeak -> "非常弱"
        PasswordStrengthLevel.Weak -> "弱"
        PasswordStrengthLevel.Medium -> "中"
        PasswordStrengthLevel.Strong -> "强"
        PasswordStrengthLevel.VeryStrong -> "非常强"
    }
}
