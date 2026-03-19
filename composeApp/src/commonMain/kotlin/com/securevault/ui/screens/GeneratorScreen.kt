package com.securevault.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.securevault.ui.animation.animateItemEntrance
import com.securevault.ui.components.CountStepperRow
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.OptionSwitchRow
import com.securevault.ui.components.SvButton
import com.securevault.ui.components.SvOutlinedButton
import com.securevault.ui.components.SvTopBar
import com.securevault.ui.theme.PasswordFontFamily
import com.securevault.ui.theme.layout
import com.securevault.ui.theme.spacing
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
    var copied by remember { mutableStateOf(false) }
    val lengthInt = length.toInt()

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }

    val copyIconTint by animateColorAsState(
        targetValue = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AnimationTokens.copyFeedbackDuration),
        label = "copyTint"
    )

    fun normalizeCounts(targetLength: Int, rawDigit: Int = digitCount, rawSymbol: Int = symbolCount): Pair<Int, Int> {
        var normalizedDigit = if (includeDigits) rawDigit.coerceIn(1, 9) else 0
        var normalizedSymbol = if (includeSymbols) rawSymbol.coerceIn(1, 9) else 0
        if (includeDigits && includeSymbols) {
            while (normalizedDigit + normalizedSymbol > targetLength) {
                if (normalizedDigit >= normalizedSymbol && normalizedDigit > 1) normalizedDigit -= 1
                else if (normalizedSymbol > 1) normalizedSymbol -= 1
                else break
            }
        } else if (includeDigits) {
            normalizedDigit = normalizedDigit.coerceAtMost(targetLength.coerceAtLeast(1))
        } else if (includeSymbols) {
            normalizedSymbol = normalizedSymbol.coerceAtMost(targetLength.coerceAtLeast(1))
        }
        return normalizedDigit to normalizedSymbol
    }

    fun regenerate() {
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
            regenerate()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = MaterialTheme.layout.pageMaxWidth)
                .padding(horizontal = MaterialTheme.layout.pageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.layout.compactContentSpacing),
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xl),
        ) {
            item {
                SvTopBar(title = "生成器", onBack = onBack)
            }

        // Generated password display
        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(0),
                variant = MyAppCardVariant.Filled,
                contentPadding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.generatedPassword.ifBlank { "点击生成按钮" },
                        modifier = Modifier.weight(1f),
                        style = if (uiState.generatedPassword.isNotBlank())
                            MaterialTheme.typography.bodyLarge.copy(fontFamily = PasswordFontFamily)
                        else
                            MaterialTheme.typography.bodyLarge,
                        color = if (uiState.generatedPassword.isNotBlank())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.outline,
                    )
                    IconButton(onClick = { regenerate() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新生成")
                    }
                    IconButton(onClick = {
                        copied = true
                        onCopyGenerated()
                    }) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = copyIconTint,
                        )
                    }
                }
            }
        }

        // Strength indicator
        item {
            Text(
                text = "强度：${strengthLabel(uiState.strength)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.animateItemEntrance(1),
            )
        }

        // Preset buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(2),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                SvButton(
                    text = "强密码",
                    onClick = { onGeneratePreset(PasswordPreset.Strong) },
                    modifier = Modifier.weight(1f),
                )
                SvButton(
                    text = "中等",
                    onClick = { onGeneratePreset(PasswordPreset.Medium) },
                    modifier = Modifier.weight(1f),
                )
                SvOutlinedButton(
                    text = "PIN",
                    onClick = { onGeneratePreset(PasswordPreset.PinLike) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

            uiState.infoMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

        // Length slider
        item {
            MyAppCard(
                modifier = Modifier.fillMaxWidth().animateItemEntrance(3),
                variant = MyAppCardVariant.Filled,
                contentPadding = PaddingValues(0.dp),
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("长度", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${lengthInt}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = length,
                        onValueChange = {
                            length = it
                            val (nd, ns) = normalizeCounts(it.toInt())
                            digitCount = nd
                            symbolCount = ns
                            lengthChangeSignal += 1
                        },
                        valueRange = 4f..128f,
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                    )
                }
            }
        }

        item { OptionSwitchRow("A-Z 大写字母", includeUppercase, { includeUppercase = it; regenerate() }, modifier = Modifier.animateItemEntrance(4)) }
        item { OptionSwitchRow("a-z 小写字母", includeLowercase, { includeLowercase = it; regenerate() }, modifier = Modifier.animateItemEntrance(5)) }
        item {
            OptionSwitchRow("0-9 数字", includeDigits, {
                includeDigits = it
                if (!it) digitCount = 0 else if (digitCount < 1) digitCount = 1
                val (nd, ns) = normalizeCounts(lengthInt)
                digitCount = nd; symbolCount = ns
                regenerate()
            }, modifier = Modifier.animateItemEntrance(6))
        }
        item {
            OptionSwitchRow("!@# 符号", includeSymbols, {
                includeSymbols = it
                if (!it) symbolCount = 0 else if (symbolCount < 1) symbolCount = 1
                val (nd, ns) = normalizeCounts(lengthInt)
                digitCount = nd; symbolCount = ns
                regenerate()
            }, modifier = Modifier.animateItemEntrance(7))
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
                            val (nd, ns) = normalizeCounts(lengthInt)
                            digitCount = nd; symbolCount = ns
                            regenerate()
                        },
                        onIncrease = {
                            val maxDigit = minOf(9, lengthInt - if (includeSymbols) symbolCount else 0).coerceAtLeast(1)
                            digitCount = (digitCount + 1).coerceAtMost(maxDigit)
                            val (nd, ns) = normalizeCounts(lengthInt)
                            digitCount = nd; symbolCount = ns
                            regenerate()
                        },
                        modifier = Modifier.animateItemEntrance(8),
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
                            val (nd, ns) = normalizeCounts(lengthInt)
                            digitCount = nd; symbolCount = ns
                            regenerate()
                        },
                        onIncrease = {
                            val maxSymbol = minOf(9, lengthInt - if (includeDigits) digitCount else 0).coerceAtLeast(1)
                            symbolCount = (symbolCount + 1).coerceAtMost(maxSymbol)
                            val (nd, ns) = normalizeCounts(lengthInt)
                            digitCount = nd; symbolCount = ns
                            regenerate()
                        },
                        modifier = Modifier.animateItemEntrance(9),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm)) }
        }
    }
}

private fun strengthLabel(level: PasswordStrengthLevel): String = when (level) {
    PasswordStrengthLevel.VeryWeak -> "非常弱"
    PasswordStrengthLevel.Weak -> "弱"
    PasswordStrengthLevel.Medium -> "中"
    PasswordStrengthLevel.Strong -> "强"
    PasswordStrengthLevel.VeryStrong -> "非常强"
}
