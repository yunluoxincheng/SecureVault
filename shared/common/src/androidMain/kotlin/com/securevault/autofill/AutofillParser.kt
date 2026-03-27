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
        val sourceContexts = if (latest != null) listOf(latest) else emptyList()
        return parseFieldsFromContexts(
            contexts = sourceContexts,
            packageName = packageName,
        )
    }

    fun parseSaveCandidate(
        contexts: List<FillContext>,
        packageName: String,
    ): SaveCandidate? {
        val parsed = parseFieldsFromContexts(
            contexts = contexts.asReversed(),
            packageName = packageName,
        )
        val username = parsed.usernameFields.firstNotNullOfOrNull { it.value?.takeIf(String::isNotBlank) } ?: ""
        val password = parsed.passwordFields.firstNotNullOfOrNull { it.value?.takeIf(String::isNotBlank) } ?: ""
        if (password.isBlank()) return null
        return SaveCandidate(
            username = username,
            password = password,
            packageName = parsed.packageName,
            webDomain = parsed.webDomain,
        )
    }

    private fun parseFieldsFromContexts(
        contexts: List<FillContext>,
        packageName: String,
    ): ParsedAutofillRequest {
        val normalizedPackage = AutofillAppIdentity.normalizePackageName(packageName)
        val allFields = mutableListOf<ParsedField>()
        val submitIds = LinkedHashSet<AutofillId>()
        var webDomain: String? = null

        contexts.forEach { fillContext ->
            val structure = fillContext.structure
            for (i in 0 until structure.windowNodeCount) {
                val root = structure.getWindowNodeAt(i).rootViewNode
                if (webDomain == null) webDomain = extractFirstWebDomain(root)
                collectFields(root, allFields, submitIds)
            }
        }

        return ParsedAutofillRequest(
            packageName = normalizedPackage,
            webDomain = webDomain,
            usernameFields = allFields.filter { it.type == AutofillFieldType.Username },
            passwordFields = allFields.filter { it.type == AutofillFieldType.Password },
            submitIds = submitIds.toList(),
        )
    }

    private fun collectFields(
        node: AssistStructure.ViewNode,
        output: MutableList<ParsedField>,
        submitIds: MutableSet<AutofillId>,
    ) {
        val autofillId: AutofillId = node.autofillId ?: return
        val type = identifyType(node)
        if (type != AutofillFieldType.Unknown) {
            output += ParsedField(
                id = autofillId,
                type = type,
                value = extractFieldText(node),
            )
        }
        if (looksLikeSubmitTrigger(node)) {
            submitIds += autofillId
        }

        for (i in 0 until node.childCount) {
            collectFields(node.getChildAt(i), output, submitIds)
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
            append(node.hint ?: "")
            append(' ')
            append(node.idEntry.orEmpty())
            append(' ')
            append(node.contentDescription ?: "")
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
            textSignals.contains("邮箱") ||
            textSignals.contains("账号名") ||
            textSignals.contains("用户id") ||
            textSignals.contains("userid") ||
            textSignals.contains("账号id")
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
            hint == "phonenumber" ||
            hint == "userid" ||
            hint.endsWith("user_id")
    }

    private fun looksLikeSubmitTrigger(node: AssistStructure.ViewNode): Boolean {
        val signal = buildString {
            append(node.hint ?: "")
            append(' ')
            append(node.idEntry.orEmpty())
            append(' ')
            append(node.contentDescription ?: "")
            append(' ')
            append(node.text ?: "")
            append(' ')
            append(node.className ?: "")
        }.lowercase()
        return signal.contains("login") ||
            signal.contains("sign in") ||
            signal.contains("signin") ||
            signal.contains("submit") ||
            signal.contains("continue") ||
            signal.contains("next") ||
            signal.contains("登录") ||
            signal.contains("立即登录") ||
            signal.contains("确认") ||
            signal.contains("下一步")
    }
}
