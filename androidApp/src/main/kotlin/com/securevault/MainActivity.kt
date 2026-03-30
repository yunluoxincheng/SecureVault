package com.securevault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.securevault.autofill.AutofillPendingSaveStore
import com.securevault.autofill.EXTRA_AUTOFILL_PASSWORD
import com.securevault.autofill.EXTRA_AUTOFILL_TITLE
import com.securevault.autofill.EXTRA_AUTOFILL_URL
import com.securevault.autofill.EXTRA_AUTOFILL_USERNAME
import com.securevault.autofill.EXTRA_AUTOFILL_WAS_LOCKED
import com.securevault.autofill.EXTRA_FROM_AUTOFILL_SAVE
import com.securevault.security.AndroidActivityProvider
import com.securevault.security.KeyManager
import com.securevault.security.ScreenSecurity
import co.touchlab.kermit.Logger
import com.securevault.ui.navigation.AutofillDraft
import com.securevault.ui.navigation.SecureVaultApp
import com.securevault.ui.theme.ProvideAndroidThemeBindings
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val screenSecurity: ScreenSecurity by inject()
    private val keyManager: KeyManager by inject()
    private var pendingAutofillDraft by mutableStateOf<AutofillDraft?>(null)
    private var isAppForeground by mutableStateOf(true)
    private val log = Logger.withTag("SvMain")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingAutofillDraft = resolveAutofillDraftFromIntentAndStore(intent, "onCreate")
        AndroidActivityProvider.set(this)
        screenSecurity.enableScreenshotProtection()
        enableEdgeToEdge()
        setContent {
            ProvideAndroidThemeBindings {
                SecureVaultApp(
                    initialAutofillDraft = pendingAutofillDraft,
                    isAppForeground = isAppForeground,
                    onAutofillDraftConsumed = {
                        pendingAutofillDraft = null
                        AutofillPendingSaveStore.clear(applicationContext)
                        clearAutofillSaveExtrasFromIntent()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAutofillDraft = resolveAutofillDraftFromIntentAndStore(intent, "onNewIntent")
    }

    override fun onResume() {
        super.onResume()
        isAppForeground = true
        AndroidActivityProvider.set(this)
        keyManager.onAppForeground()
        resolveAutofillDraftFromIntentAndStore(intent, "onResume")?.let { pendingAutofillDraft = it }
    }

    override fun onStop() {
        isAppForeground = false
        keyManager.onAppBackground()
        super.onStop()
    }

    override fun onDestroy() {
        AndroidActivityProvider.clear(this)
        super.onDestroy()
    }

    /**
     * Resolves an autofill "save draft" for the add-entry flow.
     *
     * **Order (do not reorder without updating docs/tests):**
     * 1. Valid autofill extras on [intent] ([EXTRA_FROM_AUTOFILL_SAVE], [EXTRA_AUTOFILL_TITLE], …) — if
     *    present, the store is cleared so Intent remains authoritative when both paths were written.
     * 2. Else a non-expired payload from [AutofillPendingSaveStore] (encrypted prefs + legacy migration).
     *
     * Intent extras are still populated by [com.securevault.autofill.ui.AutofillSaveActivity] so single-task
     * launches see credentials immediately; the store covers cold start / OEM-dropped extras. An optional
     * in-memory-only handoff was not adopted here to avoid breaking OEM retention behavior.
     */
    private fun resolveAutofillDraftFromIntentAndStore(
        intent: android.content.Intent,
        source: String,
    ): AutofillDraft? {
        val fromIntent = intent.toAutofillDraftOrNull()
        if (fromIntent != null) {
            AutofillPendingSaveStore.clear(applicationContext)
            logAutofillIntent(intent, source)
            return fromIntent
        }
        val payload = AutofillPendingSaveStore.peekValidPayload(applicationContext)
        if (payload != null) {
            log.i {
                "$source: autofill draft from PendingSaveStore (extras missing / process restarted)"
            }
            return AutofillDraft(
                title = payload.title,
                username = payload.username,
                password = payload.password,
                url = payload.url,
            )
        }
        logAutofillIntent(intent, source)
        return null
    }

    private fun clearAutofillSaveExtrasFromIntent() {
        if (!intent.getBooleanExtra(EXTRA_FROM_AUTOFILL_SAVE, false)) return
        intent.removeExtra(EXTRA_FROM_AUTOFILL_SAVE)
        intent.removeExtra(EXTRA_AUTOFILL_TITLE)
        intent.removeExtra(EXTRA_AUTOFILL_USERNAME)
        intent.removeExtra(EXTRA_AUTOFILL_PASSWORD)
        intent.removeExtra(EXTRA_AUTOFILL_URL)
        intent.removeExtra(EXTRA_AUTOFILL_WAS_LOCKED)
    }

    private fun logAutofillIntent(intent: android.content.Intent, source: String) {
        if (!intent.getBooleanExtra(EXTRA_FROM_AUTOFILL_SAVE, false)) {
            log.d { "$source: no autofill save extra" }
            return
        }
        val title = intent.getStringExtra(EXTRA_AUTOFILL_TITLE)?.length ?: 0
        val user = intent.getStringExtra(EXTRA_AUTOFILL_USERNAME)?.length ?: 0
        val pwd = intent.getStringExtra(EXTRA_AUTOFILL_PASSWORD)?.length ?: 0
        val url = intent.hasExtra(EXTRA_AUTOFILL_URL)
        log.i {
            "$source: autofill payload titleLen=$title userLen=$user pwdLen=$pwd hasUrl=$url " +
                "wasLocked=${intent.getBooleanExtra(EXTRA_AUTOFILL_WAS_LOCKED, false)}"
        }
    }
}

private fun android.content.Intent?.toAutofillDraftOrNull(): AutofillDraft? {
    if (this == null) return null
    if (!getBooleanExtra(EXTRA_FROM_AUTOFILL_SAVE, false)) return null
    val title: String = getStringExtra(EXTRA_AUTOFILL_TITLE)?.trim() ?: ""
    val username: String = getStringExtra(EXTRA_AUTOFILL_USERNAME)?.trim() ?: ""
    val password: String = getStringExtra(EXTRA_AUTOFILL_PASSWORD) ?: ""
    if (title.isBlank() || password.isBlank()) return null
    return AutofillDraft(
        title = title,
        username = username,
        password = password,
        url = getStringExtra(EXTRA_AUTOFILL_URL)?.trim()?.ifBlank { null },
    )
}