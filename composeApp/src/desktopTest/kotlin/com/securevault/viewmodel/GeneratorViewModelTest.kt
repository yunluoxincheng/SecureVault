package com.securevault.viewmodel

import com.securevault.security.SecureClipboard
import com.securevault.util.PasswordGenerator
import com.securevault.util.PasswordPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeneratorViewModelTest {

    @Test
    fun copyGeneratedPassword_whenEmpty_setsErrorMessage() {
        val viewModel = GeneratorViewModel(
            passwordGenerator = PasswordGenerator(),
            secureClipboard = SecureClipboard()
        )

        viewModel.copyGeneratedPassword()

        val state = viewModel.uiState.value
        assertEquals("暂无可复制的密码", state.errorMessage)
        assertNull(state.infoMessage)
    }

    @Test
    fun copyGeneratedPassword_whenGenerated_setsInfoMessage() {
        val viewModel = GeneratorViewModel(
            passwordGenerator = PasswordGenerator(),
            secureClipboard = SecureClipboard()
        )

        val generated = viewModel.generateWithPreset(PasswordPreset.Strong)
        viewModel.copyGeneratedPassword()

        val state = viewModel.uiState.value
        assertTrue(generated.isNotBlank())
        assertEquals("已复制，30 秒后自动清除", state.infoMessage)
        assertNull(state.errorMessage)
    }
}
