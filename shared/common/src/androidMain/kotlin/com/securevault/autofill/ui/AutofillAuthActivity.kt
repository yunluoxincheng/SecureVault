package com.securevault.autofill.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillId
import android.widget.EditText
import android.widget.Toast
import com.securevault.autofill.CredentialMatcher
import com.securevault.autofill.FillResponseBuilder
import com.securevault.data.PasswordRepository
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerResult
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class AutofillAuthActivity : Activity() {
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val repository: PasswordRepository by lazy { GlobalContext.get().get() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val log = Logger.withTag("SvAutofillAuth")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val usernameIds = intent.getParcelableArrayListExtra<AutofillId>(AutofillCredentialPickerActivity.EXTRA_USERNAME_IDS).orEmpty()
        val passwordIds = intent.getParcelableArrayListExtra<AutofillId>(AutofillCredentialPickerActivity.EXTRA_PASSWORD_IDS).orEmpty()
        val packageName = intent.getStringExtra(FillResponseBuilder.EXTRA_PICKER_PACKAGE_NAME).orEmpty()
        val webDomain = intent.getStringExtra(FillResponseBuilder.EXTRA_PICKER_WEB_DOMAIN)
        log.i { "onCreate auth screen pkg=$packageName hasDomain=${!webDomain.isNullOrBlank()} idsU=${usernameIds.size} idsP=${passwordIds.size}" }

        AlertDialog.Builder(this)
            .setTitle("保险库已锁定")
            .setMessage("请先验证身份，再选择要填充的凭证。")
            .setNegativeButton("取消") { _, _ -> finish() }
            .setPositiveButton("验证") { _, _ ->
                val input = EditText(this).apply { hint = "输入主密码" }
                AlertDialog.Builder(this)
                    .setTitle("解锁保险库")
                    .setView(input)
                    .setNegativeButton("取消") { _, _ -> finish() }
                    .setPositiveButton("继续") { _, _ ->
                        val raw = input.text?.toString().orEmpty()
                        if (raw.isBlank()) {
                            Toast.makeText(this, "请输入主密码", Toast.LENGTH_SHORT).show()
                            finish()
                            return@setPositiveButton
                        }
                        val pwd = raw.toCharArray()
                        scope.launch {
                            when (keyManager.unlockWithPassword(pwd)) {
                                is KeyManagerResult.Success -> {
                                    log.i { "unlockWithPassword success, loading matches" }
                                    val dataKey = keyManager.getDataKey()
                                    if (dataKey == null) {
                                        Toast.makeText(this@AutofillAuthActivity, "解锁失败", Toast.LENGTH_SHORT).show()
                                        log.e { "unlockWithPassword success but dataKey is null" }
                                        finish()
                                        return@launch
                                    }
                                    val matches = CredentialMatcher(repository).match(
                                        packageName = packageName,
                                        domain = webDomain,
                                        dataKey = dataKey,
                                    )
                                    if (matches.isEmpty()) {
                                        Toast.makeText(this@AutofillAuthActivity, "没有可用凭证", Toast.LENGTH_SHORT).show()
                                        log.w { "no matches after unlock for pkg=$packageName domain=$webDomain" }
                                        finish()
                                        return@launch
                                    }
                                    val pickerIntent = Intent(this@AutofillAuthActivity, AutofillCredentialPickerActivity::class.java).apply {
                                        putParcelableArrayListExtra(
                                            AutofillCredentialPickerActivity.EXTRA_USERNAME_IDS,
                                            ArrayList(usernameIds),
                                        )
                                        putParcelableArrayListExtra(
                                            AutofillCredentialPickerActivity.EXTRA_PASSWORD_IDS,
                                            ArrayList(passwordIds),
                                        )
                                        putExtra(
                                            AutofillCredentialPickerActivity.EXTRA_TITLES,
                                            matches.map { it.title }.toTypedArray(),
                                        )
                                        putExtra(
                                            AutofillCredentialPickerActivity.EXTRA_USERNAMES,
                                            matches.map { it.username }.toTypedArray(),
                                        )
                                        putExtra(
                                            AutofillCredentialPickerActivity.EXTRA_PASSWORDS,
                                            matches.map { it.password }.toTypedArray(),
                                        )
                                    }
                                    log.i { "start picker after unlock, matches=${matches.size}, no NEW_TASK" }
                                    startActivity(pickerIntent)
                                    finish()
                                }
                                else -> {
                                    Toast.makeText(this@AutofillAuthActivity, "主密码错误", Toast.LENGTH_SHORT).show()
                                    log.w { "unlockWithPassword failed" }
                                    finish()
                                }
                            }
                        }
                    }
                    .setOnCancelListener { finish() }
                    .show()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
