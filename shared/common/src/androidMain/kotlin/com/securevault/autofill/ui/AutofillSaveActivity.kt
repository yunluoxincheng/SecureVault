package com.securevault.autofill.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.securevault.autofill.AutofillPendingSaveStore
import com.securevault.autofill.EXTRA_AUTOFILL_PASSWORD
import com.securevault.autofill.EXTRA_AUTOFILL_TITLE
import com.securevault.autofill.EXTRA_AUTOFILL_URL
import com.securevault.autofill.EXTRA_AUTOFILL_USERNAME
import com.securevault.autofill.EXTRA_AUTOFILL_WAS_LOCKED
import com.securevault.autofill.EXTRA_FROM_AUTOFILL_SAVE
import com.securevault.security.KeyManager
import org.koin.core.context.GlobalContext

class AutofillSaveActivity : Activity() {
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val log = Logger.withTag("SvAutofillSave")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
        val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
        val clientPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val title = when {
            !webDomain.isNullOrBlank() -> webDomain
            else -> clientPackage
        }
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE)
        val actionLabel = if (actionType == "UpdateExisting") "更新密码" else "保存密码"
        val actionMessage = if (actionType == "UpdateExisting") {
            "检测到该站点密码可能已变化，是否更新这条凭证？"
        } else {
            "检测到新登录凭证，是否保存到保险库？"
        }

        log.i {
            "dialog shown clientPkg=$clientPackage web=$webDomain titleLen=${title.length} pwdLen=${password.length} action=$actionType"
        }

        AlertDialog.Builder(this)
            .setTitle(actionLabel)
            .setMessage("$actionMessage\n\n站点：$title\n账号：${username.ifBlank { "(未识别)" }}")
            .setNegativeButton("取消") { _, _ ->
                log.d { "user dismissed save dialog" }
                finish()
            }
            .setPositiveButton(actionLabel) { _, _ ->
                log.i { "user confirmed save, launching main" }
                openAppForDraft(
                    username = username,
                    password = password,
                    title = title,
                    webDomain = webDomain,
                )
            }
            .setOnCancelListener {
                log.d { "dialog cancelled" }
                finish()
            }
            .show()
    }

    private fun openAppForDraft(
        username: String,
        password: String,
        title: String,
        webDomain: String?,
    ) {
        AutofillPendingSaveStore.persistForLauncher(
            this,
            title = title.ifBlank { "未命名站点" },
            username = username,
            password = password,
            webDomain = webDomain,
        )
        val isLocked = keyManager.getDataKey() == null
        val appPkg = applicationContext.packageName
        val mainComponent = ComponentName(appPkg, "com.securevault.MainActivity")

        fun buildMainIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = mainComponent
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
            putExtra(EXTRA_AUTOFILL_TITLE, title.ifBlank { "未命名站点" })
            putExtra(EXTRA_AUTOFILL_USERNAME, username)
            putExtra(EXTRA_AUTOFILL_PASSWORD, password)
            putExtra(EXTRA_AUTOFILL_URL, webDomain?.let { "https://$it" })
            putExtra(EXTRA_FROM_AUTOFILL_SAVE, true)
            putExtra(EXTRA_AUTOFILL_WAS_LOCKED, isLocked)
        }

        val ctx = applicationContext
        var launched = false
        runCatching {
            ctx.startActivity(buildMainIntent())
            launched = true
            log.i { "startActivity(MAIN/LAUNCHER explicit) ok pkg=$appPkg locked=$isLocked" }
        }.onFailure { e ->
            log.e(e) { "startActivity explicit MAIN failed, trying getLaunchIntent" }
        }

        if (!launched) {
            val launcher = packageManager.getLaunchIntentForPackage(appPkg)
            if (launcher != null) {
                launcher.apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                    )
                    component = mainComponent
                    putExtra(EXTRA_AUTOFILL_TITLE, title.ifBlank { "未命名站点" })
                    putExtra(EXTRA_AUTOFILL_USERNAME, username)
                    putExtra(EXTRA_AUTOFILL_PASSWORD, password)
                    putExtra(EXTRA_AUTOFILL_URL, webDomain?.let { "https://$it" })
                    putExtra(EXTRA_FROM_AUTOFILL_SAVE, true)
                    putExtra(EXTRA_AUTOFILL_WAS_LOCKED, isLocked)
                }
                runCatching {
                    ctx.startActivity(launcher)
                    launched = true
                    log.i { "startActivity(launcher+component) ok" }
                }.onFailure { e ->
                    log.e(e) { "startActivity launcher fallback failed" }
                }
            } else {
                log.e { "getLaunchIntentForPackage returned null for $appPkg" }
            }
        }

        if (!launched) {
            log.e { "all start attempts failed — check OriginOS 后台弹出/自启动权限" }
        }

        finish()
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
