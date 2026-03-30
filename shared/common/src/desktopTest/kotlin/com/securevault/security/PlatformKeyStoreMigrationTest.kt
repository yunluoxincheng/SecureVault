package com.securevault.security

import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlatformKeyStoreMigrationTest {
    private val prefs = Preferences.userRoot().node("com/securevault/keystore")

    @BeforeTest
    fun setUp() {
        clearPreferences()
        clearFlags()
    }

    @AfterTest
    fun tearDown() {
        clearPreferences()
        clearFlags()
    }

    @Test
    fun storeAndLoad_withSecureStoreEnabled_roundTrips() {
        System.setProperty(PROP_SECURE_STORE_ENABLED, "true")
        val keyStore = PlatformKeyStore()
        val key = byteArrayOf(1, 3, 5, 7, 9)

        keyStore.storeDeviceKey(key)
        val load = keyStore.getDeviceKey()

        assertIs<DeviceKeyLoadResult.Success>(load)
        assertContentEquals(key, load.key)
        assertTrue(keyStore.hasDeviceKey())
    }

    @Test
    fun getDeviceKey_migratesLegacyXorBlob_whenSecureStoreEnabled() {
        val key = byteArrayOf(11, 22, 33, 44)
        System.setProperty(PROP_SECURE_STORE_ENABLED, "false")
        System.setProperty(PROP_LEGACY_READ_ENABLED, "true")
        val legacyStore = PlatformKeyStore()
        legacyStore.storeDeviceKey(key)
        assertTrue(legacyStore.hasDeviceKey())

        System.setProperty(PROP_SECURE_STORE_ENABLED, "true")
        System.clearProperty(PROP_LEGACY_READ_ENABLED)
        val migratedStore = PlatformKeyStore()
        val migratedLoad = migratedStore.getDeviceKey()

        assertIs<DeviceKeyLoadResult.Success>(migratedLoad)
        assertContentEquals(key, migratedLoad.key)
        assertTrue(prefs.get("device_key_dpapi_b64", null) != null)
    }

    @Test
    fun rollbackDrill_legacyReadPath_requiresExplicitFlag() {
        val key = byteArrayOf(9, 8, 7, 6)
        System.setProperty(PROP_SECURE_STORE_ENABLED, "false")
        val writer = PlatformKeyStore()
        writer.storeDeviceKey(key)

        System.clearProperty(PROP_LEGACY_READ_ENABLED)
        val blocked = writer.getDeviceKey()
        assertIs<DeviceKeyLoadResult.KeystoreError>(blocked)

        System.setProperty(PROP_LEGACY_READ_ENABLED, "true")
        val rollbackLoad = writer.getDeviceKey()
        assertIs<DeviceKeyLoadResult.Success>(rollbackLoad)
        assertContentEquals(key, rollbackLoad.key)
    }

    private fun clearPreferences() {
        prefs.remove("device_key_dpapi_b64")
        prefs.remove("device_key_migrated_to_dpapi_v1")
        prefs.remove("encrypted_device_key")
        prefs.remove("master_key")
    }

    private fun clearFlags() {
        System.clearProperty(PROP_SECURE_STORE_ENABLED)
        System.clearProperty(PROP_LEGACY_READ_ENABLED)
    }

    private companion object {
        const val PROP_SECURE_STORE_ENABLED = "securevault.keystore.desktop.dpapi.enabled"
        const val PROP_LEGACY_READ_ENABLED = "securevault.keystore.desktop.legacy_read_enabled"
    }
}
