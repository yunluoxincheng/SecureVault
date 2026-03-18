package com.securevault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.securevault.util.PasswordGeneratorConfig
import com.securevault.util.PasswordPreset
import com.securevault.util.PasswordStrengthLevel
import com.securevault.viewmodel.GeneratorUiState
import kotlinx.coroutines.delay

@Composable
fun GeneratorScreen(
    uiState: GeneratorUiState,
    onBack: (() -> Unit)? = null,
    onGeneratePreset: (PasswordPreset) -> Unit,
    onGenerateCustom: (PasswordGeneratorConfig) -> Unit,
    onCopyGenerated: () -> Unit
) {
    var length by remember(uiState.config.length) { mutableStateOf(uiState.config.length.toFloat()) }
    var includeUppercase by remember(uiState.config.includeUppercase) { mutableStateOf(uiState.config.includeUppercase) }
    var includeLowercase by remember(uiState.config.includeLowercase) { mutableStateOf(uiState.config.includeLowercase) }
    var includeDigits by remember(uiState.config.includeDigits) { mutableStateOf(uiState.config.includeDigits) }
    var includeSymbols by remember(uiState.config.includeSymbols) { mutableStateOf(uiState.config.includeSymbols) }
    var digitCount by remember(uiState.config.digitCount) { mutableStateOf(uiState.config.digitCount.coerceAtLeast(1)) }
    var symbolCount by remember(uiState.config.symbolCount) { mutableStateOf(uiState.config.symbolCount.coerceAtLeast(1)) }
    var lengthChangeSignal by remember { mutableStateOf(0) }
    val lengthInt = length.toInt()

    fun normalizeCounts(targetLength: Int, rawDigit: Int = digitCount, rawSymbol: Int = symbolCount): Pair<Int, Int> {
        var normalizedDigit = if (includeDigits) rawDigit.coerceIn(1, 9) else 0
        var normalizedSymbol = if (includeSymbols) rawSymbol.coerceIn(1, 9) else 0

        if (includeDigits && includeSymbols) {
            while (normalizedDigit + normalizedSymbol > targetLength) {
                if (normalizedDigit >= normalizedSymbol && normalizedDigit > 1) {
                    normalizedDigit -= 1
                } else if (normalizedSymbol > 1) {
                    normalizedSymbol -= 1
                } else {
                    break
                }
            }
        } else if (includeDigits) {
            normalizedDigit = normalizedDigit.coerceAtMost(targetLength.coerceAtLeast(1))
        } else if (includeSymbols) {
            normalizedSymbol = normalizedSymbol.coerceAtMost(targetLength.coerceAtLeast(1))
        }

        return normalizedDigit to normalizedSymbol
    }

    fun regenerateWithCurrentConfig() {
        onGenerateCustom(
            PasswordGeneratorConfig(
                length = lengthInt,
                includeUppercase = includeUppercase,
                includeLowercase = includeLowercase,
                includeDigits = includeDigits,
                includeSymbols = includeSymbols,
                digitCount = if (includeDigits) digitCount else 0,
                symbolCount = if (includeSymbols) symbolCount else 0
            )
        )
    }

    LaunchedEffect(lengthChangeSignal) {
        if (lengthChangeSignal > 0) {
            delay(180)
            regenerateWithCurrentConfig()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                    Text("生成器", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.generatedPassword.ifBlank { "点击生成密码" },
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onCopyGenerated),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { regenerateWithCurrentConfig() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新生成")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onGeneratePreset(PasswordPreset.Strong) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("强") }
                Button(
                    onClick = { onGeneratePreset(PasswordPreset.Medium) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("中") }
                Button(
                    onClick = { onGeneratePreset(PasswordPreset.PinLike) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("PIN") }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("长度", style = MaterialTheme.typography.titleMedium)
                    Text(text = length.toInt().toString(), style = MaterialTheme.typography.headlineSmall)
                    Slider(
                        value = length,
                        onValueChange = {
                            length = it
                            val (normalizedDigit, normalizedSymbol) = normalizeCounts(it.toInt())
                            digitCount = normalizedDigit
                            symbolCount = normalizedSymbol
                            lengthChangeSignal += 1
                        },
                        valueRange = 4f..128f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }
        }

        item {
            OptionSwitchRow("A-Z", includeUppercase) {
                includeUppercase = it
                regenerateWithCurrentConfig()
            }
        }
        item {
            OptionSwitchRow("a-z", includeLowercase) {
                includeLowercase = it
                regenerateWithCurrentConfig()
            }
        }
        item {
            OptionSwitchRow("0-9", includeDigits) {
                includeDigits = it
                if (!it) digitCount = 0
                if (it && digitCount < 1) digitCount = 1
                val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                digitCount = normalizedDigit
                symbolCount = normalizedSymbol
                regenerateWithCurrentConfig()
            }
        }
        item {
            OptionSwitchRow("!@#$%^&*", includeSymbols) {
                includeSymbols = it
                if (!it) symbolCount = 0
                if (it && symbolCount < 1) symbolCount = 1
                val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                digitCount = normalizedDigit
                symbolCount = normalizedSymbol
                regenerateWithCurrentConfig()
            }
        }

        if (includeDigits) {
            item {
                CountStepperRow(
                    label = "最少数字个数",
                    value = digitCount,
                    min = 1,
                    max = minOf(9, lengthInt - if (includeSymbols) symbolCount else 0).coerceAtLeast(1),
                    onDecrease = {
                        digitCount = (digitCount - 1).coerceAtLeast(1)
                        val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                        digitCount = normalizedDigit
                        symbolCount = normalizedSymbol
                        regenerateWithCurrentConfig()
                    },
                    onIncrease = {
                        val maxDigit = minOf(9, lengthInt - if (includeSymbols) symbolCount else 0).coerceAtLeast(1)
                        digitCount = (digitCount + 1).coerceAtMost(maxDigit)
                        val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                        digitCount = normalizedDigit
                        symbolCount = normalizedSymbol
                        regenerateWithCurrentConfig()
                    }
                )
            }
        }

        if (includeSymbols) {
            item {
                CountStepperRow(
                    label = "最少符号个数",
                    value = symbolCount,
                    min = 1,
                    max = minOf(9, lengthInt - if (includeDigits) digitCount else 0).coerceAtLeast(1),
                    onDecrease = {
                        symbolCount = (symbolCount - 1).coerceAtLeast(1)
                        val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                        digitCount = normalizedDigit
                        symbolCount = normalizedSymbol
                        regenerateWithCurrentConfig()
                    },
                    onIncrease = {
                        val maxSymbol = minOf(9, lengthInt - if (includeDigits) digitCount else 0).coerceAtLeast(1)
                        symbolCount = (symbolCount + 1).coerceAtMost(maxSymbol)
                        val (normalizedDigit, normalizedSymbol) = normalizeCounts(lengthInt)
                        digitCount = normalizedDigit
                        symbolCount = normalizedSymbol
                        regenerateWithCurrentConfig()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun OptionSwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun CountStepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val safeMax = max.coerceAtLeast(min)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    IconButton(
                        onClick = onDecrease,
                        enabled = value > min,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "减少")
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .widthIn(min = 36.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    IconButton(
                        onClick = onIncrease,
                        enabled = value < safeMax,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "增加")
                    }
                }
            }
        }
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
