package com.securevault.security

/**
 * Result of loading the device-wrapped data key from platform storage.
 * Used to distinguish “no key” from decrypt/integrity failures on Android Keystore paths.
 */
sealed class DeviceKeyLoadResult {
    data class Success(val key: ByteArray) : DeviceKeyLoadResult()
    data object NotPresent : DeviceKeyLoadResult()
    data object UnwrapFailed : DeviceKeyLoadResult()
    data class KeystoreError(val diagnosticCode: String) : DeviceKeyLoadResult()
}
