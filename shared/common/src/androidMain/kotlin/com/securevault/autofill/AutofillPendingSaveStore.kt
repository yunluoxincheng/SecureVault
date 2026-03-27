package com.securevault.autofill

import android.content.Context

/**
 * Persists a save draft when [onSaveRequest] runs. Some OEMs kill the app or drop intent extras when
 * the autofill service starts [com.securevault.MainActivity]; reading from here survives cold start.
 *
 * Cleared when the user completes the in-app add flow ([clear]).
 */
object AutofillPendingSaveStore {
    private const val PREFS = "sv_autofill_pending_save"
    private const val KEY_HAS = "has"
    private const val KEY_SAVED_AT_MS = "saved_at_ms"
    private const val KEY_TITLE = "title"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_URL = "url"

    /** Pending entries older than this are discarded on read. */
    private const val MAX_AGE_MS = 5 * 60 * 1000L

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
        val webDomain = candidate.webDomain
        val title = when {
            !webDomain.isNullOrBlank() -> webDomain
            else -> candidate.packageName
        }.ifBlank { "未命名站点" }
        val url = webDomain?.let { "https://$it" }
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

    private fun persistPayload(context: Context, payload: Payload) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().run {
            putBoolean(KEY_HAS, true)
            putLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            putString(KEY_TITLE, payload.title)
            putString(KEY_USERNAME, payload.username)
            putString(KEY_PASSWORD, payload.password)
            if (payload.url != null) putString(KEY_URL, payload.url) else remove(KEY_URL)
            commit()
        }
    }

    /**
     * Returns a non-expired payload if present; does not remove it (caller clears after UI consumes).
     */
    fun peekValidPayload(context: Context): Payload? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS, false)) return null
        val savedAt = prefs.getLong(KEY_SAVED_AT_MS, 0L)
        if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_AGE_MS) {
            clear(context)
            return null
        }
        val title = prefs.getString(KEY_TITLE, "")?.trim().orEmpty()
        val password = prefs.getString(KEY_PASSWORD, "").orEmpty()
        if (title.isBlank() || password.isBlank()) {
            clear(context)
            return null
        }
        val username = prefs.getString(KEY_USERNAME, "").orEmpty()
        val url = prefs.getString(KEY_URL, null)?.trim()?.ifBlank { null }
        return Payload(title = title, username = username, password = password, url = url)
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
