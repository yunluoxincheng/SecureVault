package com.securevault.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.MyAppButton
import com.securevault.ui.components.MyAppButtonVariant
import com.securevault.ui.components.MyAppCard
import com.securevault.ui.components.MyAppCardVariant
import com.securevault.ui.components.MyAppInput
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.GeneratorScreen
import com.securevault.ui.screens.LoginScreen
import com.securevault.ui.screens.OnboardingScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.RegisterScreen
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.VaultScreen
import com.securevault.ui.theme.AppTheme
import com.securevault.ui.theme.ThemeMode
import com.securevault.util.PasswordPreset
import com.securevault.util.PasswordStrengthLevel
import com.securevault.viewmodel.GeneratorUiState

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LightDarkPreview

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    AppTheme(themeMode = ThemeMode.System) {
        content()
    }
}

// ---------------------------------------------------------------------------
// MyAppButton previews
// ---------------------------------------------------------------------------

@LightDarkPreview
@Composable
private fun MyAppButtonAllVariantsPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppButton(
                text = "Unlock Vault",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Primary,
            )
            MyAppButton(
                text = "Edit Entry",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Secondary,
            )
            MyAppButton(
                text = "Use Biometrics",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Ghost,
            )
            MyAppButton(
                text = "Delete Entry",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Danger,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppButtonWithIconPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppButton(
                text = "Unlock",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Primary,
                leadingIcon = Icons.Outlined.Lock,
            )
            MyAppButton(
                text = "Secure Mode",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.Lock,
            )
            MyAppButton(
                text = "Use Biometrics",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Ghost,
                leadingIcon = Icons.Outlined.Lock,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppButtonDisabledStatePreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppButton(
                text = "Unlock Vault",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Primary,
                enabled = false,
            )
            MyAppButton(
                text = "Edit Entry",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Secondary,
                enabled = false,
            )
            MyAppButton(
                text = "Use Biometrics",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Ghost,
                enabled = false,
            )
            MyAppButton(
                text = "Delete Entry",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Danger,
                enabled = false,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppButtonLoadingStatePreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppButton(
                text = "Unlocking…",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Primary,
                isLoading = true,
            )
            MyAppButton(
                text = "Saving…",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppButtonVariant.Danger,
                isLoading = true,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MyAppInput previews
// ---------------------------------------------------------------------------

@LightDarkPreview
@Composable
private fun MyAppInputStatesPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppInput(
                value = "alice@example.com",
                onValueChange = {},
                label = "Account",
                placeholder = "Enter account",
                modifier = Modifier.fillMaxWidth(),
            )
            MyAppInput(
                value = "bad_input",
                onValueChange = {},
                label = "Master Password",
                placeholder = "Enter master password",
                modifier = Modifier.fillMaxWidth(),
                isError = true,
                errorMessage = "Password must be at least 12 characters",
                isPassword = true,
            )
            MyAppInput(
                value = "S3cure#Pass2026",
                onValueChange = {},
                label = "Vault Password",
                placeholder = "Enter vault password",
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Use uppercase, lowercase, number and symbol",
                isPassword = true,
            )
            MyAppInput(
                value = "Visible#Pass2026",
                onValueChange = {},
                label = "Vault Password (Visible)",
                placeholder = "Enter vault password",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true,
                showPassword = true,
                onTogglePasswordVisibility = {},
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppInputInteractivePreview() {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppInput(
                value = account,
                onValueChange = { account = it },
                label = "Account",
                placeholder = "you@securevault.dev",
                modifier = Modifier.fillMaxWidth(),
            )
            MyAppInput(
                value = password,
                onValueChange = { password = it },
                label = "Master Password",
                placeholder = "Enter your password",
                modifier = Modifier.fillMaxWidth(),
                isPassword = true,
                showPassword = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword },
                isError = password.isNotEmpty() && password.length < 12,
                errorMessage = "Minimum length is 12 characters",
                supportingText = "Longer passwords are safer",
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppInputFocusStatePreview() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppInput(
                value = "focused.user@securevault.dev",
                onValueChange = {},
                label = "Focused Input",
                placeholder = "Focus ring and glow demo",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                supportingText = "This preview auto-requests focus",
            )
            MyAppInput(
                value = "",
                onValueChange = {},
                label = "Unfocused Input",
                placeholder = "Default resting state",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MyAppCard previews
// ---------------------------------------------------------------------------

@LightDarkPreview
@Composable
private fun MyAppCardVariantsPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Filled,
            ) {
                Text(text = "Filled card")
                Text(text = "Soft surface layer with default padding")
            }
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Elevated,
            ) {
                Text(text = "Elevated card")
                Text(text = "Modern layered depth and rounded corners")
            }
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Outlined,
            ) {
                Text(text = "Outlined card")
                Text(text = "Subtle border for secondary grouping")
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppCardClickablePreview() {
    var taps by remember { mutableStateOf(0) }

    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Elevated,
                onClick = { taps++ },
            ) {
                Text(text = "Clickable elevated card")
                Text(text = "Tap to preview press shadow animation")
                Text(text = "Tap count: $taps")
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun MyAppCardZeroPaddingPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MyAppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = MyAppCardVariant.Elevated,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "Zero padding card",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------

@LightDarkPreview
@Composable
private fun BaseLoginScreenPreview() {
    PreviewContainer {
        LoginScreen(
            biometricAvailable = false,
            isLoading = false,
            errorMessage = null,
            onLogin = {},
            onBiometricLogin = {},
            onGoRegister = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun BaseVaultScreenPreview() {
    val entries = listOf(
        PasswordEntry(
            id = 1L,
            title = "GitHub",
            username = "dev@securevault.com",
            password = "PreviewPassword!123",
            url = "https://github.com",
            category = "work",
            isFavorite = true,
            securityMode = true,
            createdAt = 0L,
            updatedAt = 0L
        ),
        PasswordEntry(
            id = 2L,
            title = "银行",
            username = "13800000000",
            password = "StrongPass#2026",
            url = "https://bank.example.com",
            category = "finance",
            isFavorite = false,
            securityMode = false,
            createdAt = 0L,
            updatedAt = 0L
        )
    )

    PreviewContainer {
        VaultScreen(
            entries = entries,
            categories = listOf("work", "finance"),
            selectedCategory = null,
            favoritesOnly = false,
            query = "",
            securityModeEnabled = false,
            hasLoadedAtLeastOnce = true,
            onQueryChange = {},
            onFiltersChange = { _, _ -> },
            onEntryClick = {},
            onAddClick = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun BasePasswordStrengthBarPreview() {
    PreviewContainer {
        PasswordStrengthBar(password = "Preview@Passw0rd2026")
    }
}

@LightDarkPreview
@Composable
private fun BaseRegisterScreenPreview() {
    PreviewContainer {
        RegisterScreen(
            isLoading = false,
            errorMessage = null,
            onRegister = {},
            onGoLogin = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun BaseSettingsScreenPreview() {
    PreviewContainer {
        SettingsScreen(
            currentTheme = ThemeMode.System,
            biometricEnabled = true,
            screenshotAllowed = false,
            sessionTimeoutMs = 300_000L,
            errorMessage = null,
            onThemeChange = {},
            onBiometricChange = {},
            onScreenshotAllowedChange = {},
            onSessionTimeoutChange = {},
            onBack = {},
            onLock = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun BaseGeneratorScreenPreview() {
    PreviewContainer {
        GeneratorScreen(
            uiState = GeneratorUiState(
                generatedPassword = "uQ2!ks8V@p3L",
                config = PasswordPreset.Strong.config,
                history = listOf("9V!k2sA8", "7q@1LmZ3"),
                strength = PasswordStrengthLevel.Strong,
                errorMessage = null,
                infoMessage = "已复制，30 秒后自动清除"
            ),
            onBack = {},
            onGeneratePreset = {},
            onGenerateCustom = {},
            onCopyGenerated = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun BaseOnboardingScreenPreview() {
    PreviewContainer {
        OnboardingScreen(onFinish = {})
    }
}

@LightDarkPreview
@Composable
private fun BaseAddEditPasswordScreenPreview() {
    val sampleEntry = PasswordEntry(
        id = 3L,
        title = "邮箱",
        username = "alice@example.com",
        password = "M@ilPass#2026",
        url = "https://mail.example.com",
        notes = "预览备注",
        category = "personal",
        isFavorite = true,
        securityMode = false,
        createdAt = 0L,
        updatedAt = 0L
    )

    PreviewContainer {
        AddEditPasswordScreen(
            entry = sampleEntry,
            onSave = {},
            onCancel = {},
            onGeneratePassword = { "Generated!Pass123" }
        )
    }
}

@LightDarkPreview
@Composable
private fun BasePasswordDetailScreenPreview() {
    val sampleEntry = PasswordEntry(
        id = 4L,
        title = "工作 VPN",
        username = "vpn-user",
        password = "Vpn#Secure2026",
        url = "https://vpn.example.com",
        notes = "仅工作设备使用",
        category = "work",
        isFavorite = false,
        securityMode = true,
        createdAt = 0L,
        updatedAt = 0L
    )

    PreviewContainer {
        PasswordDetailScreen(
            entry = sampleEntry,
            securityModeEnabled = false,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onCopyUsername = {},
            onCopyPassword = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun LoadingLoginScreenPreview() {
    PreviewContainer {
        LoginScreen(
            biometricAvailable = false,
            isLoading = true,
            errorMessage = null,
            onLogin = {},
            onBiometricLogin = {},
            onGoRegister = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun ErrorLoginScreenPreview() {
    PreviewContainer {
        LoginScreen(
            biometricAvailable = false,
            isLoading = false,
            errorMessage = "主密码错误，请重试",
            onLogin = {},
            onBiometricLogin = {},
            onGoRegister = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun LoadingRegisterScreenPreview() {
    PreviewContainer {
        RegisterScreen(
            isLoading = true,
            errorMessage = null,
            onRegister = {},
            onGoLogin = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun ErrorRegisterScreenPreview() {
    PreviewContainer {
        RegisterScreen(
            isLoading = false,
            errorMessage = "创建失败：密码不符合策略",
            onRegister = {},
            onGoLogin = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun ErrorSettingsScreenPreview() {
    PreviewContainer {
        SettingsScreen(
            currentTheme = ThemeMode.System,
            biometricEnabled = true,
            screenshotAllowed = false,
            sessionTimeoutMs = 300_000L,
            errorMessage = "系统暂不支持生物识别",
            onThemeChange = {},
            onBiometricChange = {},
            onScreenshotAllowedChange = {},
            onSessionTimeoutChange = {},
            onBack = {},
            onLock = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun ErrorGeneratorScreenPreview() {
    PreviewContainer {
        GeneratorScreen(
            uiState = GeneratorUiState(
                generatedPassword = "",
                config = PasswordPreset.Strong.config,
                history = emptyList(),
                strength = PasswordStrengthLevel.VeryWeak,
                errorMessage = "至少选择一种字符类型",
                infoMessage = null
            ),
            onBack = {},
            onGeneratePreset = {},
            onGenerateCustom = {},
            onCopyGenerated = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun EmptyVaultScreenPreview() {
    PreviewContainer {
        VaultScreen(
            entries = emptyList(),
            categories = emptyList(),
            selectedCategory = null,
            favoritesOnly = false,
            query = "",
            securityModeEnabled = false,
            hasLoadedAtLeastOnce = true,
            onQueryChange = {},
            onFiltersChange = { _, _ -> },
            onEntryClick = {},
            onAddClick = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun EmptyAddEditPasswordScreenPreview() {
    PreviewContainer {
        AddEditPasswordScreen(
            entry = null,
            onSave = {},
            onCancel = {},
            onGeneratePassword = { "Temp#Pass2026" }
        )
    }
}

@LightDarkPreview
@Composable
private fun EdgePasswordDetailScreenNormalModePreview() {
    val sampleEntry = PasswordEntry(
        id = 5L,
        title = "社交账号",
        username = "user.social",
        password = "S0cial#Pwd2026",
        url = "https://social.example.com",
        notes = "普通模式可查看密码",
        category = "personal",
        isFavorite = true,
        securityMode = false,
        createdAt = 0L,
        updatedAt = 0L
    )

    PreviewContainer {
        PasswordDetailScreen(
            entry = sampleEntry,
            securityModeEnabled = false,
            onBack = {},
            onEdit = {},
            onDelete = {},
            onCopyUsername = {},
            onCopyPassword = {}
        )
    }
}

@LightDarkPreview
@Composable
private fun EdgeGeneratorScreenPinPresetPreview() {
    PreviewContainer {
        GeneratorScreen(
            uiState = GeneratorUiState(
                generatedPassword = "482901",
                config = PasswordPreset.PinLike.config,
                history = listOf("935771", "261048"),
                strength = PasswordStrengthLevel.Medium,
                errorMessage = null,
                infoMessage = null
            ),
            onBack = {},
            onGeneratePreset = {},
            onGenerateCustom = {},
            onCopyGenerated = {}
        )
    }
}