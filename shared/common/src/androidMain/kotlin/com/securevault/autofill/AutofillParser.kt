package com.securevault.autofill

import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

class AutofillParser {
    fun parseFillContexts(
        contexts: List<FillContext>,
        packageName: String,
    ): ParsedAutofillRequest {
        val latest = contexts.latestOrNull()
        val allFields = mutableListOf<ParsedField>()
        var webDomain: String? = null

        latest?.structure?.let { structure ->
            for (i in 0 until structure.windowNodeCount) {
                val root = structure.getWindowNodeAt(i).rootViewNode
                if (webDomain == null) {
                    webDomain = extractFirstWebDomain(root)
                }
                collectFields(root, allFields)
            }
        }

        return ParsedAutofillRequest(
            packageName = packageName,
            webDomain = webDomain,
            usernameFields = allFields.filter { it.type == AutofillFieldType.Username },
            passwordFields = allFields.filter { it.type == AutofillFieldType.Password },
        )
    }

    fun parseSaveCandidate(
        contexts: List<FillContext>,
        packageName: String,
    ): SaveCandidate? {
        val parsed = parseFillContexts(contexts = contexts, packageName = packageName)
        val username = parsed.usernameFields.firstNotNullOfOrNull { it.value?.takeIf(String::isNotBlank) } ?: ""
        val password = parsed.passwordFields.firstNotNullOfOrNull { it.value?.takeIf(String::isNotBlank) } ?: ""
        if (password.isBlank()) return null
        return SaveCandidate(
            username = username,
            password = password,
            packageName = packageName,
            webDomain = parsed.webDomain,
        )
    }

    private fun collectFields(node: AssistStructure.ViewNode, output: MutableList<ParsedField>) {
        val autofillId: AutofillId = node.autofillId ?: return
        val type = identifyType(node)
        if (type != AutofillFieldType.Unknown) {
            output += ParsedField(
                id = autofillId,
                type = type,
                value = extractFieldText(node),
            )
        }

        for (i in 0 until node.childCount) {
            collectFields(node.getChildAt(i), output)
        }
    }

    private fun extractFirstWebDomain(node: AssistStructure.ViewNode): String? {
        node.webDomain?.takeIf { it.isNotBlank() }?.let { return it }
        for (i in 0 until node.childCount) {
            extractFirstWebDomain(node.getChildAt(i))?.let { return it }
        }
        return null
    }

    private fun extractFieldText(node: AssistStructure.ViewNode): String? {
        node.autofillValue?.textValue?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun identifyType(node: AssistStructure.ViewNode): AutofillFieldType {
        val hints = node.autofillHints?.map { it.lowercase() }.orEmpty()
        if (hints.any { it.contains("password") }) return AutofillFieldType.Password
        if (hints.any { isUsernameHint(it) }) {
            return AutofillFieldType.Username
        }

        val inputType = node.inputType
        if (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        ) {
            return AutofillFieldType.Password
        }
        if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_PHONE) {
            return AutofillFieldType.Username
        }
        if (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
            return AutofillFieldType.Username
        }

        val textSignals = buildString {
            append(node.hint?.toString().orEmpty())
            append(' ')
            append(node.idEntry.orEmpty())
            append(' ')
            append(node.webDomain.orEmpty())
        }.lowercase()

        if (textSignals.contains("captcha") || textSignals.contains("otp") || textSignals.contains("验证码")) {
            return AutofillFieldType.Unknown
        }
        if (textSignals.contains("password") || textSignals.contains("密码") || textSignals.contains("passwd")) {
            return AutofillFieldType.Password
        }
        if (
            textSignals.contains("username") ||
            textSignals.contains("user") ||
            textSignals.contains("login") ||
            textSignals.contains("email") ||
            textSignals.contains("phone") ||
            textSignals.contains("mobile") ||
            textSignals.contains("tel") ||
            textSignals.contains("账号") ||
            textSignals.contains("用户名") ||
            textSignals.contains("手机") ||
            textSignals.contains("电话") ||
            textSignals.contains("手机号") ||
            textSignals.contains("邮箱")
        ) {
            return AutofillFieldType.Username
        }
        return AutofillFieldType.Unknown
    }

    private fun isUsernameHint(hint: String): Boolean {
        if (hint.contains("password")) return false
        return hint == View.AUTOFILL_HINT_USERNAME.lowercase() ||
            hint == View.AUTOFILL_HINT_EMAIL_ADDRESS.lowercase() ||
            hint == View.AUTOFILL_HINT_PHONE.lowercase() ||
            hint.endsWith("username") ||
            hint.endsWith("email") ||
            hint.endsWith("phone") ||
            hint.contains("login") ||
            hint == "tel" ||
            hint == "mobile" ||
            hint == "phonenumber"
    }
}
