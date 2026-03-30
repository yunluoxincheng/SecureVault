package com.securevault.autofill

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists a save draft when [onSaveRequest] runs. Some OEMs kill the app or drop intent extras when
 * the autofill service starts [com.securevault.MainActivity]; reading from here survives cold start.
 *
 * Payloads are stored with [EncryptedSharedPreferences] (AndroidX Security). Legacy plaintext
 * preferences (`sv_autofill_pending_save`) are read once when present, written back encrypted, then removed.
 *
 * Cleared when the user completes the in-app add flow ([clear]).
 *
 * **Dual path with MainActivity** (must stay aligned with [com.securevault.MainActivity.resolveAutofillDraftFromIntentAndStore]):
 * 1. If the launcher Intent carries valid autofill extras ([EXTRA_FROM_AUTOFILL_SAVE] and related
 *    keys in [AutofillIntentKeys]), that draft wins and the pending store is cleared.
 * 2. Otherwise, if this store holds a non-expired payload, it is used (covers process restart /
 *    OEM-dropped extras).
 */
object AutofillPendingSaveStore {

    /** Legacy plaintext file name; kept for one-time migration read + delete. */
    private const val PREFS_LEGACY = "sv_autofill_pending_save"

    private const val PREFS_ENCRYPTED = "sv_autofill_pending_save_enc"
    private const val KEY_HAS = "has"
    private const val KEY_SAVED_AT_MS = "saved_at_ms"
    private const val KEY_TITLE = "title"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_URL = "url"

    /** Pending entries older than this are discarded on read. */
    private const val MAX_AGE_MS = 5 * 60 * 1000L

    @Volatile
    private var cachedEncryptedPrefs: SharedPreferences? = null

    data class Payload(
        val title: String,
        val username: String,
        val password: String,
        val url: String?,
    )

    /** Used by [com.securevault.autofill.ui.AutofillSaveActivity] before launching MainActivity. */
    fun persistForLauncher(
        context: Context,
        title: String,
        username: String,
        password: String,
        webDomain: String?,
    ) {
        val url = webDomain?.let { "https://$it" }
        persistPayload(
            context,
            Payload(
                title = title.ifBlank { "未命名站点" },
                username = username,
                password = password,
                url = url,
            ),
        )
    }

    fun persistFromCandidate(context: Context, candidate: SaveCandidate) {
        val webDomain = candidate.webDomain?.trim()?.ifBlank { null }
        val normalizedPackage = AutofillAppIdentity.normalizePackageName(candidate.packageName)
        val title = if (webDomain != null) {
            webDomain
        } else {
            resolveAppLabel(context, normalizedPackage)
                ?: AutofillAppIdentity.inferTitle(webDomain = null, packageName = normalizedPackage)
        }.ifBlank { "未命名站点" }
        val url = if (webDomain != null) {
            "https://$webDomain"
        } else {
            AutofillAppIdentity.appUrlForPackage(normalizedPackage)
        }
        persistPayload(
            context,
            Payload(
                title = title,
                username = candidate.username,
                password = candidate.password,
                url = url,
            ),
        )
    }

    private fun resolveAppLabel(context: Context, packageName: String): String? {
        return runCatching {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString().trim()
        }.getOrNull()?.ifBlank { null }
    }

    private fun persistPayload(context: Context, payload: Payload) {
        val app = context.applicationContext
        encryptedPrefs(app).edit().run {
            putBoolean(KEY_HAS, true)
            putLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            putString(KEY_TITLE, payload.title)
            putString(KEY_USERNAME, payload.username)
            putString(KEY_PASSWORD, payload.password)
            if (payload.url != null) putString(KEY_URL, payload.url) else remove(KEY_URL)
            commit()
        }
        wipeLegacyPlaintext(app)
    }

    /**
     * Returns a non-expired payload if present; does not remove it (caller clears after UI consumes).
     */
    fun peekValidPayload(context: Context): Payload? {
        val app = context.applicationContext
        val fromEncrypted = readValidPayload(encryptedPrefs(app)) {
            encryptedPrefs(app).edit().clear().commit()
        }
        if (fromEncrypted != null) {
            wipeLegacyPlaintext(app)
            return fromEncrypted
        }
        val legacyPrefs = app.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
        val fromLegacy = readValidPayload(legacyPrefs) {
            legacyPrefs.edit().clear().commit()
        } ?: return null
        persistPayload(app, fromLegacy)
        return fromLegacy
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        encryptedPrefs(app).edit().clear().commit()
        wipeLegacyPlaintext(app)
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        cachedEncryptedPrefs?.let { return it }
        synchronized(this) {
            cachedEncryptedPrefs?.let { return it }
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_ENCRYPTED,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            cachedEncryptedPrefs = prefs
            return prefs
        }
    }

    private fun readValidPayload(
        prefs: SharedPreferences,
        onDiscard: () -> Unit,
    ): Payload? {
        if (!prefs.getBoolean(KEY_HAS, false)) return null
        val savedAt = prefs.getLong(KEY_SAVED_AT_MS, 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_AGE_MS) {
            onDiscard()
            return null
        }
        val title = prefs.getString(KEY_TITLE, "")?.trim().orEmpty()
        val password = prefs.getString(KEY_PASSWORD, "").orEmpty()
        if (title.isBlank() || password.isBlank()) {
            onDiscard()
            return null
        }
        val username = prefs.getString(KEY_USERNAME, "").orEmpty()
        val url = prefs.getString(KEY_URL, null)?.trim()?.ifBlank { null }
        return Payload(title = title, username = username, password = password, url = url)
    }

    private fun wipeLegacyPlaintext(context: Context) {
        runCatching {
            context.deleteSharedPreferences(PREFS_LEGACY)
        }
    }
}
