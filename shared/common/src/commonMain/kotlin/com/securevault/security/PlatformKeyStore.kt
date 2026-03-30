package com.securevault.security

expect class PlatformKeyStore() {
    fun storeDeviceKey(key: ByteArray)
    fun getDeviceKey(): DeviceKeyLoadResult
    fun deleteDeviceKey()
    fun hasDeviceKey(): Boolean
    fun isHardwareBacked(): Boolean
}