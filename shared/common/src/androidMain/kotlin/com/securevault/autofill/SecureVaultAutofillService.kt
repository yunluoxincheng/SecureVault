package com.securevault.autofill

import android.os.CancellationSignal
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.service.autofill.AutofillService
import com.securevault.data.ConfigRepository
import com.securevault.data.PasswordRepository
import com.securevault.data.VaultConfigKeys
import com.securevault.security.KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class SecureVaultAutofillService : AutofillService() {
    private val repository: PasswordRepository by lazy { GlobalContext.get().get() }
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val configRepository: ConfigRepository by lazy { GlobalContext.get().get() }

    private val parser = AutofillParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val packageName = request.fillContexts.latestOrNull()?.structure?.activityComponent?.packageName ?: return callback.onSuccess(null)
        if (AutofillBlocklist.isBlocked(packageName)) return callback.onSuccess(null)

        scope.launch {
            val parsed = parser.parseFillContexts(request.fillContexts, packageName)
            if (parsed.usernameFields.isEmpty() && parsed.passwordFields.isEmpty()) {
                callback.onSuccess(null)
                return@launch
            }

            val dataKey = keyManager.getDataKey()
            val isLocked = dataKey == null
            val matches = if (isLocked) {
                emptyList()
            } else {
                CredentialMatcher(repository).match(
                    packageName = parsed.packageName,
                    domain = parsed.webDomain,
                    dataKey = dataKey,
                )
            }

            val response = FillResponseBuilder(this@SecureVaultAutofillService).build(
                request = parsed,
                matches = matches,
                vaultLocked = isLocked,
            )
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val packageName = request.fillContexts.latestOrNull()?.structure?.activityComponent?.packageName
            ?: return callback.onSuccess()
        if (AutofillBlocklist.isBlocked(packageName)) return callback.onSuccess()

        scope.launch {
            val askToSave = configRepository.get(VaultConfigKeys.AutofillAskToSaveOnLogin)?.toBooleanStrictOrNull() ?: true
            if (!askToSave) {
                callback.onSuccess()
                return@launch
            }

            val dataKey = keyManager.getDataKey()
            if (dataKey == null) {
                callback.onSuccess()
                return@launch
            }
            val candidate = parser.parseSaveCandidate(request.fillContexts, packageName)
            if (candidate == null) {
                callback.onSuccess()
                return@launch
            }

            val detector = SaveDetector(repository)
            val action = detector.detect(candidate, dataKey)
            if (action == null) {
                callback.onSuccess()
                return@launch
            }

            FillResponseBuilder(this@SecureVaultAutofillService)
                .buildSaveActionPendingIntent(candidate, action)
                .send()
            callback.onSuccess()
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        scope.cancel()
    }
}
