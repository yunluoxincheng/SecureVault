package com.securevault.security

expect class PlatformKeyStore() {
    fun storeDeviceKey(key: ByteArray)
    fun getDeviceKey(): ByteArray?
    fun deleteDeviceKey()
    fun hasDeviceKey(): Boolean
    fun isHardwareBacked(): Boolean
}