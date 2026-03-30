package com.securevault

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.autofill.AutofillPendingSaveStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers legacy plaintext prefs migration to encrypted storage (dual-read, write-back, cleanup).
 * Manual equivalent: persist from Autofill → kill process → launch MainActivity (Intent-only, store-only, combined).
 */
@RunWith(AndroidJUnit4::class)
class AutofillPendingSaveStoreMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        AutofillPendingSaveStore.clear(context)
    }

    @Test
    fun legacyPlaintext_migratesToEncrypted_andClearsLegacy() {
        AutofillPendingSaveStore.clear(context)
        val legacy = context.getSharedPreferences("sv_autofill_pending_save", Context.MODE_PRIVATE)
        legacy.edit().apply {
            putBoolean("has", true)
            putLong("saved_at_ms", System.currentTimeMillis())
            putString("title", "Example")
            putString("username", "user1")
            putString("password", "secret")
            putString("url", "https://example.com")
            commit()
        }

        val payload = AutofillPendingSaveStore.peekValidPayload(context)
        assertNotNull(payload)
        assertEquals("Example", payload!!.title)
        assertEquals("user1", payload.username)
        assertEquals("secret", payload.password)
        assertEquals("https://example.com", payload.url)

        assertFalse(
            context.getSharedPreferences("sv_autofill_pending_save", Context.MODE_PRIVATE)
                .getBoolean("has", false),
        )

        val again = AutofillPendingSaveStore.peekValidPayload(context)
        assertEquals("Example", again?.title)
    }

    @Test
    fun persistForLauncher_writesEncrypted_notPlaintextFile() {
        AutofillPendingSaveStore.clear(context)
        AutofillPendingSaveStore.persistForLauncher(
            context,
            title = "T",
            username = "u",
            password = "p",
            webDomain = "x.example.com",
        )
        assertFalse(
            context.getSharedPreferences("sv_autofill_pending_save", Context.MODE_PRIVATE)
                .getBoolean("has", false),
        )
        val payload = AutofillPendingSaveStore.peekValidPayload(context)
        assertEquals("T", payload?.title)
        assertEquals("p", payload?.password)
    }
}
