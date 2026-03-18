package com.securevault.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.securevault.data.PasswordEntry
import com.securevault.ui.components.PasswordStrengthBar
import com.securevault.ui.screens.AddEditPasswordScreen
import com.securevault.ui.screens.GeneratorScreen
import com.securevault.ui.screens.LoginScreen
import com.securevault.ui.screens.OnboardingScreen
import com.securevault.ui.screens.PasswordDetailScreen
import com.securevault.ui.screens.RegisterScreen
import com.securevault.ui.screens.SettingsScreen
import com.securevault.ui.screens.VaultScreen
import com.securevault.ui.theme.SecureVaultTheme
import com.securevault.ui.theme.ThemeMode
import com.securevault.util.PasswordPreset
import com.securevault.util.PasswordStrengthLevel
import com.securevault.viewmodel.GeneratorUiState

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LightDarkPreview

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    SecureVaultTheme(themeMode = ThemeMode.System) {
        content()
    }
}

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
            onQueryChange = {},
            onCategoryChange = {},
            onFavoritesOnlyChange = {},
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
            errorMessage = null,
            onThemeChange = {},
            onBiometricChange = {},
            onScreenshotAllowedChange = {},
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
            errorMessage = "系统暂不支持生物识别",
            onThemeChange = {},
            onBiometricChange = {},
            onScreenshotAllowedChange = {},
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
            onQueryChange = {},
            onCategoryChange = {},
            onFavoritesOnlyChange = {},
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