package com.securevault.autofill

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.widget.RemoteViews
import com.securevault.autofill.ui.AutofillAuthActivity
import com.securevault.autofill.ui.AutofillSaveActivity

class FillResponseBuilder(
    private val context: Context,
) {
    fun build(
        request: ParsedAutofillRequest,
        matches: List<MatchedCredential>,
        vaultLocked: Boolean,
    ): FillResponse {
        val builder = FillResponse.Builder()
        if (vaultLocked) {
            builder.addDataset(createOpenVaultDataset(request))
        } else {
            matches.forEach { credential ->
                builder.addDataset(createCredentialDataset(request, credential))
            }
            builder.addDataset(createOpenVaultDataset(request))
        }

        val saveInfo = createSaveInfo(request)
        if (saveInfo != null) {
            builder.setSaveInfo(saveInfo)
        }
        return builder.build()
    }

    fun buildSaveActionPendingIntent(
        candidate: SaveCandidate,
        action: SaveDetector.SaveAction,
    ): PendingIntent {
        val intent = buildSaveActionIntent(candidate, action)
        return PendingIntent.getActivity(
            context,
            candidate.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutabilityFlag(),
        )
    }

    /**
     * Opens the app main entry with a draft — avoids an intermediate activity (helps some OEM autofill stacks).
     */
    fun buildMainActivityAutofillDraftIntent(
        candidate: SaveCandidate,
        wasLocked: Boolean,
    ): Intent {
        val webDomain = candidate.webDomain
        val title = when {
            !webDomain.isNullOrBlank() -> webDomain
            else -> candidate.packageName
        }.ifBlank { "未命名站点" }
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(context.packageName, "com.securevault.MainActivity")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
            putExtra(EXTRA_AUTOFILL_TITLE, title)
            putExtra(EXTRA_AUTOFILL_USERNAME, candidate.username)
            putExtra(EXTRA_AUTOFILL_PASSWORD, candidate.password)
            putExtra(EXTRA_AUTOFILL_URL, webDomain?.let { "https://$it" })
            putExtra(EXTRA_FROM_AUTOFILL_SAVE, true)
            putExtra(EXTRA_AUTOFILL_WAS_LOCKED, wasLocked)
        }
    }

    fun buildMainActivityAutofillDraftPendingIntent(
        candidate: SaveCandidate,
        wasLocked: Boolean,
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            candidate.hashCode() + 4242,
            buildMainActivityAutofillDraftIntent(candidate, wasLocked),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutabilityFlag(),
        )
    }

    /**
     * For [android.service.autofill.SaveCallback.onSuccess] — must use [PendingIntent.FLAG_IMMUTABLE]
     * when supported; launched from the client app’s context (correct stack / BAL behavior).
     */
    fun buildMainActivityAutofillDraftPendingIntentForSaveCallback(
        candidate: SaveCandidate,
        wasLocked: Boolean,
    ): PendingIntent {
        val mutability = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        return PendingIntent.getActivity(
            context,
            candidate.hashCode() + 9191,
            buildMainActivityAutofillDraftIntent(candidate, wasLocked),
            PendingIntent.FLAG_UPDATE_CURRENT or mutability,
        )
    }

    fun buildSaveActionIntent(
        candidate: SaveCandidate,
        action: SaveDetector.SaveAction,
    ): Intent {
        val intent = Intent(context, AutofillSaveActivity::class.java).apply {
            putExtra(AutofillSaveActivity.EXTRA_USERNAME, candidate.username)
            putExtra(AutofillSaveActivity.EXTRA_PASSWORD, candidate.password)
            putExtra(AutofillSaveActivity.EXTRA_PACKAGE_NAME, candidate.packageName)
            putExtra(AutofillSaveActivity.EXTRA_WEB_DOMAIN, candidate.webDomain)
            putExtra(AutofillSaveActivity.EXTRA_ACTION_TYPE, action.type.name)
            putExtra(AutofillSaveActivity.EXTRA_EXISTING_ENTRY_ID, action.existingEntryId ?: -1L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }

    private fun createCredentialDataset(
        request: ParsedAutofillRequest,
        credential: MatchedCredential,
    ): Dataset {
        val presentation = createDatasetPresentation(
            title = credential.title,
            subtitle = "账号 ${maskUsername(credential.username)}",
            fallbackPrimary = "${credential.title} · ${maskUsername(credential.username)}",
        )
        return Dataset.Builder(presentation).apply {
            request.usernameFields.forEach { setValue(it.id, android.view.autofill.AutofillValue.forText(credential.username)) }
            request.passwordFields.forEach { setValue(it.id, android.view.autofill.AutofillValue.forText(credential.password)) }
        }.build()
    }

    private fun createOpenVaultDataset(request: ParsedAutofillRequest): Dataset {
        val presentation = createDatasetPresentation(
            title = "SafeVault",
            subtitle = "转到我的密码库",
            fallbackPrimary = "打开保险库",
        )
        val authIntent = Intent(context, AutofillAuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val authPendingIntent = PendingIntent.getActivity(
            context,
            10086,
            authIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutabilityFlag(),
        )
        return Dataset.Builder(presentation).apply {
            setAuthentication(authPendingIntent.intentSender)
            request.usernameFields.forEach { setValue(it.id, null) }
            request.passwordFields.forEach { setValue(it.id, null) }
        }.build()
    }

    private fun createSaveInfo(request: ParsedAutofillRequest): SaveInfo? {
        val ids = (request.usernameFields + request.passwordFields).map { it.id }.toTypedArray()
        if (ids.isEmpty()) return null
        val saveType = SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD
        return SaveInfo.Builder(saveType, ids).build()
    }

    private fun pendingIntentMutabilityFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    }

    private fun createDatasetPresentation(
        title: String,
        subtitle: String,
        fallbackPrimary: String,
    ): RemoteViews {
        val layoutId = context.resources.getIdentifier("autofill_dataset_item", "layout", context.packageName)
        val titleId = context.resources.getIdentifier("autofill_item_title", "id", context.packageName)
        val subtitleId = context.resources.getIdentifier("autofill_item_subtitle", "id", context.packageName)

        if (layoutId != 0 && titleId != 0 && subtitleId != 0) {
            return RemoteViews(context.packageName, layoutId).apply {
                setTextViewText(titleId, title)
                setTextViewText(subtitleId, subtitle)
            }
        }

        return RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, fallbackPrimary)
        }
    }

    private fun maskUsername(username: String): String {
        if (username.length <= 4) return "****"
        return "${username.take(2)}${"*".repeat(username.length - 4)}${username.takeLast(2)}"
    }
}
