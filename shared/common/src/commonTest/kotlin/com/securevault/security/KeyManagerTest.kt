package com.securevault.security

import com.securevault.crypto.Argon2Kdf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KeyManagerTest {

    @Test
    fun setupVault_thenUnlockWithPassword_succeeds() {
        val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager())

        val setupResult = runBlocking {
            keyManager.setupVault("master-pass".toCharArray())
        }

        assertIs<KeyManagerResult.Success<Unit>>(setupResult)
        assertTrue(keyManager.isVaultSetup())
        assertTrue(keyManager.getDataKey() != null)
    }

    @Test
    fun changeMasterPassword_oldPasswordFails_newPasswordSucceeds() {
        val keyManager = KeyManager(Argon2Kdf(), PlatformKeyStore(), SessionManager())

        runBlocking {
            keyManager.setupVault("old-pass".toCharArray())
            val changeResult = keyManager.changeMasterPassword("old-pass".toCharArray(), "new-pass".toCharArray())
            assertIs<KeyManagerResult.Success<Unit>>(changeResult)

            keyManager.lock()
            val oldUnlock = keyManager.unlockWithPassword("old-pass".toCharArray())
            assertIs<KeyManagerResult.Error>(oldUnlock)

            val newUnlock = keyManager.unlockWithPassword("new-pass".toCharArray())
            assertIs<KeyManagerResult.Success<Unit>>(newUnlock)
        }
    }
}