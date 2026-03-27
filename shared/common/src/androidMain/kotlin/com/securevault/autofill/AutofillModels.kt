package com.securevault.autofill

import android.service.autofill.FillContext
import android.view.autofill.AutofillId

enum class AutofillFieldType {
    Username,
    Password,
    Unknown,
}

data class ParsedField(
    val id: AutofillId,
    val type: AutofillFieldType,
    val value: String?,
)

data class ParsedAutofillRequest(
    val packageName: String,
    val webDomain: String?,
    val usernameFields: List<ParsedField>,
    val passwordFields: List<ParsedField>,
    val submitIds: List<AutofillId>,
)

data class MatchedCredential(
    val entryId: Long,
    val title: String,
    val username: String,
    val password: String,
    val securityMode: Boolean,
)

data class SaveCandidate(
    val username: String,
    val password: String,
    val packageName: String,
    val webDomain: String?,
)

fun List<FillContext>.latestOrNull(): FillContext? = lastOrNull()
