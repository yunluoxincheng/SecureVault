package com.securevault.autofill.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import com.securevault.autofill.SaveDetector
import com.securevault.data.PasswordEntry
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class AutofillSaveActivity : Activity() {
    private val repository: PasswordRepository by lazy { GlobalContext.get().get() }
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
        val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE)
        val existingEntryId = intent.getLongExtra(EXTRA_EXISTING_ENTRY_ID, -1L).takeIf { it > 0L }

        val title = when {
            !webDomain.isNullOrBlank() -> webDomain
            else -> packageName
        }
        val actionLabel = if (actionType == SaveDetector.ActionType.UpdateExisting.name) "更新密码" else "保存密码"
        val actionMessage = if (actionType == SaveDetector.ActionType.UpdateExisting.name) {
            "检测到该站点密码可能已变化，是否更新这条凭证？"
        } else {
            "检测到新登录凭证，是否保存到保险库？"
        }

        AlertDialog.Builder(this)
            .setTitle(actionLabel)
            .setMessage("$actionMessage\n\n站点：$title\n账号：${username.ifBlank { "(未识别)" }}")
            .setNegativeButton("取消") { _, _ -> finish() }
            .setPositiveButton(actionLabel) { _, _ ->
                persistCredential(
                    username = username,
                    password = password,
                    title = title,
                    webDomain = webDomain,
                    actionType = actionType,
                    existingEntryId = existingEntryId,
                )
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun persistCredential(
        username: String,
        password: String,
        title: String,
        webDomain: String?,
        actionType: String?,
        existingEntryId: Long?,
    ) {
        scope.launch {
            val dataKey = keyManager.getDataKey()
            if (dataKey == null) {
                runOnUiThread { finish() }
                return@launch
            }
            val now = System.currentTimeMillis()
            if (actionType == SaveDetector.ActionType.UpdateExisting.name && existingEntryId != null) {
                val existing = repository.getById(existingEntryId, dataKey)
                if (existing != null) {
                    repository.update(
                        existing.copy(
                            username = username.ifBlank { existing.username },
                            password = password,
                            updatedAt = now,
                        ),
                        dataKey,
                    )
                }
            } else {
                repository.create(
                    PasswordEntry(
                        title = title.ifBlank { "未命名站点" },
                        username = username,
                        password = password,
                        url = webDomain?.let { "https://$it" },
                        createdAt = now,
                        updatedAt = now,
                    ),
                    dataKey,
                )
            }
            runOnUiThread { finish() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_WEB_DOMAIN = "extra_web_domain"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val EXTRA_EXISTING_ENTRY_ID = "extra_existing_entry_id"
    }
}
