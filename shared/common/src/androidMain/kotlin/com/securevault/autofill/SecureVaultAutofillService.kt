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
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class SecureVaultAutofillService : AutofillService() {
    private val repository: PasswordRepository by lazy { GlobalContext.get().get() }
    private val keyManager: KeyManager by lazy { GlobalContext.get().get() }
    private val configRepository: ConfigRepository by lazy { GlobalContext.get().get() }

    private val parser = AutofillParser()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val log = Logger.withTag("SvAutofillSvc")

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
            log.i {
                "onFillRequest: pkg=$packageName locked=$isLocked userFields=${parsed.usernameFields.size} " +
                    "passFields=${parsed.passwordFields.size} matches=${matches.size}"
            }
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val packageName = request.fillContexts.latestOrNull()?.structure?.activityComponent?.packageName
        if (packageName == null) {
            log.w { "onSaveRequest: missing client package" }
            return callback.onSuccess()
        }
        if (AutofillBlocklist.isBlocked(packageName)) {
            log.d { "onSaveRequest: blocked package=$packageName" }
            return callback.onSuccess()
        }

        scope.launch {
            runCatching {
                val askToSave = configRepository.get(VaultConfigKeys.AutofillAskToSaveOnLogin)?.toBooleanStrictOrNull() ?: true
                if (!askToSave) {
                    log.d { "onSaveRequest: ask-to-save disabled in settings" }
                    callback.onSuccess()
                    return@launch
                }

                val candidate = parser.parseSaveCandidate(request.fillContexts, packageName)
                if (candidate == null) {
                    val parsed = parser.parseFillContexts(request.fillContexts, packageName)
                    log.d {
                        "onSaveRequest: no parseable username/password " +
                            "contexts=${request.fillContexts.size} userFields=${parsed.usernameFields.size} " +
                            "passFields=${parsed.passwordFields.size} domain=${parsed.webDomain}"
                    }
                    callback.onSuccess()
                    return@launch
                }

                val dataKey = keyManager.getDataKey()
                val action = if (dataKey != null) {
                    SaveDetector(repository).detect(candidate, dataKey)
                } else {
                    log.i { "onSaveRequest: session locked — still opening save flow (treat as new)" }
                    SaveDetector.SaveAction(
                        type = SaveDetector.ActionType.SaveNew,
                        existingEntryId = null,
                    )
                }

                if (action == null) {
                    log.d { "onSaveRequest: no action (e.g. password unchanged)" }
                    callback.onSuccess()
                    return@launch
                }

                val wasLocked = dataKey == null
                AutofillPendingSaveStore.persistFromCandidate(
                    this@SecureVaultAutofillService,
                    candidate,
                )
                val builder = FillResponseBuilder(this@SecureVaultAutofillService)
                val handoff = builder.buildMainActivityAutofillDraftPendingIntentForSaveCallback(
                    candidate,
                    wasLocked,
                )
                // Per SaveCallback.onSuccess(IntentSender): start from the autofilled client activity’s
                // context — not applicationContext.startActivity (blocked as background launch on many devices).
                withContext(Dispatchers.Main) {
                    callback.onSuccess(handoff.intentSender)
                    log.i {
                        "onSaveRequest: SaveCallback.onSuccess(IntentSender) action=${action.type} " +
                            "wasLocked=$wasLocked"
                    }
                }
            }.onFailure { e ->
                log.e(e) { "onSaveRequest: unexpected error" }
                callback.onSuccess()
            }
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        scope.cancel()
    }
}
