package com.securevault.autofill.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.autofill.AutofillId
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.securevault.autofill.CredentialMatcher
import com.securevault.autofill.FillResponseBuilder
import com.securevault.data.PasswordRepository
import com.securevault.security.AndroidActivityProvider
import com.securevault.security.BiometricAuth
import com.securevault.security.BiometricResult
import com.securevault.security.KeyManager
import com.securevault.security.KeyManagerError
import com.securevault.security.KeyManagerResult
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class AutofillAuthActivity : FragmentActivity() {
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val repository: PasswordRepository by lazy { GlobalContext.get().get() }
    private val biometricAuth: BiometricAuth by lazy { GlobalContext.get().get() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val log = Logger.withTag("SvAutofillAuth")
    private var authFlowStarted = false
    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            setResult(RESULT_OK, result.data)
            log.i { "picker result relayed to autofill framework" }
        } else {
            setResult(RESULT_CANCELED)
            log.w { "picker result canceled/empty, resultCode=${result.resultCode}" }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidActivityProvider.set(this)

        val usernameIds = intent.getParcelableArrayListExtra<AutofillId>(AutofillCredentialPickerActivity.EXTRA_USERNAME_IDS).orEmpty()
        val passwordIds = intent.getParcelableArrayListExtra<AutofillId>(AutofillCredentialPickerActivity.EXTRA_PASSWORD_IDS).orEmpty()
        val packageName = intent.getStringExtra(FillResponseBuilder.EXTRA_PICKER_PACKAGE_NAME).orEmpty()
        val webDomain = intent.getStringExtra(FillResponseBuilder.EXTRA_PICKER_WEB_DOMAIN)
        log.i { "onCreate auth screen pkg=$packageName hasDomain=${!webDomain.isNullOrBlank()} idsU=${usernameIds.size} idsP=${passwordIds.size}" }

        if (!authFlowStarted) {
            authFlowStarted = true
            scope.launch {
                when (val biometricResult = biometricAuth.authenticate("SecureVault", "验证身份后选择要填充的凭证")) {
                    BiometricResult.Success -> {
                        when (val unlockResult = keyManager.unlockWithBiometric()) {
                            is KeyManagerResult.Success -> {
                                log.i { "unlockWithBiometric success, loading matches" }
                                openPickerAfterUnlock(usernameIds, passwordIds, packageName, webDomain)
                            }
                            is KeyManagerResult.Error -> {
                                if (unlockResult.error == KeyManagerError.VaultNotSetup) {
                                    Toast.makeText(this@AutofillAuthActivity, "未检测到保险库", Toast.LENGTH_SHORT).show()
                                    log.w { "unlockWithBiometric failed: vault not setup" }
                                    finish()
                                } else {
                                    log.w { "unlockWithBiometric unavailable, fallback to password" }
                                    showPasswordFallbackDialog(usernameIds, passwordIds, packageName, webDomain)
                                }
                            }
                        }
                    }
                    BiometricResult.Cancelled,
                    BiometricResult.Failed,
                    BiometricResult.NotAvailable,
                    is BiometricResult.Error -> {
                        log.w { "biometric auth not completed, fallback to password: $biometricResult" }
                        showPasswordFallbackDialog(usernameIds, passwordIds, packageName, webDomain)
                    }
                }
            }
        }
    }

    private fun showPasswordFallbackDialog(
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
        packageName: String,
        webDomain: String?,
    ) {
        val dialogView = LayoutInflater.from(this)
            .inflate(
                resources.getIdentifier("autofill_auth_password_dialog", "layout", this@AutofillAuthActivity.packageName),
                null,
                false,
            )
        val input = dialogView.findViewById<EditText>(
            resources.getIdentifier("autofill_auth_password_input", "id", this@AutofillAuthActivity.packageName),
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val cancelView = dialogView.findViewById<android.view.View>(
            resources.getIdentifier("autofill_auth_action_cancel", "id", this@AutofillAuthActivity.packageName),
        )
        val confirmView = dialogView.findViewById<android.view.View>(
            resources.getIdentifier("autofill_auth_action_confirm", "id", this@AutofillAuthActivity.packageName),
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setOnCancelListener { finish() }
            .create()
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        cancelView.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        confirmView.setOnClickListener {
            val raw = input.text?.toString().orEmpty()
            if (raw.isBlank()) {
                Toast.makeText(this, "请输入主密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            scope.launch {
                when (keyManager.unlockWithPassword(raw.toCharArray())) {
                    is KeyManagerResult.Success -> {
                        log.i { "unlockWithPassword success, loading matches" }
                        dialog.dismiss()
                        openPickerAfterUnlock(usernameIds, passwordIds, packageName, webDomain)
                    }
                    is KeyManagerResult.Error -> {
                        Toast.makeText(this@AutofillAuthActivity, "主密码错误", Toast.LENGTH_SHORT).show()
                        log.w { "unlockWithPassword failed" }
                        input.setText("")
                    }
                }
            }
        }
    }

    private suspend fun openPickerAfterUnlock(
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
        packageName: String,
        webDomain: String?,
    ) {
        val dataKey = keyManager.getDataKey()
        if (dataKey == null) {
            Toast.makeText(this, "解锁失败", Toast.LENGTH_SHORT).show()
            log.e { "unlock success but dataKey is null" }
            finish()
            return
        }
        val matches = CredentialMatcher(repository).match(
            packageName = packageName,
            domain = webDomain,
            dataKey = dataKey,
        )
        if (matches.isEmpty()) {
            Toast.makeText(this, "没有可用凭证", Toast.LENGTH_SHORT).show()
            log.w { "no matches after unlock for pkg=$packageName domain=$webDomain" }
            finish()
            return
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
            putExtra(FillResponseBuilder.EXTRA_PICKER_PACKAGE_NAME, packageName)
            putExtra(FillResponseBuilder.EXTRA_PICKER_WEB_DOMAIN, webDomain)
        }
        log.i { "start picker for result after unlock, matches=${matches.size}" }
        pickerLauncher.launch(pickerIntent)
    }

    override fun onDestroy() {
        AndroidActivityProvider.clear(this)
        scope.cancel()
        super.onDestroy()
    }
}
