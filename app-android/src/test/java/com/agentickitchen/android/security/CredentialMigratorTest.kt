package com.agentickitchen.android.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMigratorTest {
    @Test
    fun plaintextCredentialMovesToSecureStoreAndIsRemoved() {
        val legacy = FakeLegacySource(mutableMapOf("gemini_api_key" to "secret"))
        val secure = FakeCredentialStore()

        val migrated = CredentialMigrator(secure, legacy).migrate(setOf("gemini_api_key"))

        assertEquals(setOf("gemini_api_key"), migrated)
        assertEquals("secret", secure.getCredential("gemini_api_key"))
        assertFalse(legacy.values.containsKey("gemini_api_key"))
    }

    @Test
    fun existingSecureCredentialWinsAndPlaintextDuplicateIsRemoved() {
        val legacy = FakeLegacySource(mutableMapOf("gemini_api_key" to "old-secret"))
        val secure = FakeCredentialStore().apply {
            saveCredential("gemini_api_key", "secure-secret")
        }

        val migrated = CredentialMigrator(secure, legacy).migrate(setOf("gemini_api_key"))

        assertTrue(migrated.isEmpty())
        assertEquals("secure-secret", secure.getCredential("gemini_api_key"))
        assertFalse(legacy.values.containsKey("gemini_api_key"))
    }

    @Test
    fun plaintextIsPreservedWhenSecureRoundTripFails() {
        val legacy = FakeLegacySource(mutableMapOf("gemini_api_key" to "secret"))
        val secure = FakeCredentialStore(persistWrites = false)

        val migrated = CredentialMigrator(secure, legacy).migrate(setOf("gemini_api_key"))

        assertTrue(migrated.isEmpty())
        assertEquals("secret", legacy.values["gemini_api_key"])
    }

    @Test
    fun keystoreFailureDoesNotCrashOrDeletePlaintext() {
        val legacy = FakeLegacySource(mutableMapOf("gemini_api_key" to "secret"))
        val secure = FakeCredentialStore(throwOnWrite = true)

        val migrated = CredentialMigrator(secure, legacy).migrate(setOf("gemini_api_key"))

        assertTrue(migrated.isEmpty())
        assertEquals("secret", legacy.values["gemini_api_key"])
    }

    private class FakeLegacySource(
        val values: MutableMap<String, String>
    ) : LegacyCredentialSource {
        override fun read(key: String): String? = values[key]

        override fun remove(keys: Set<String>): Boolean {
            keys.forEach(values::remove)
            return true
        }
    }

    private class FakeCredentialStore(
        private val persistWrites: Boolean = true,
        private val throwOnWrite: Boolean = false
    ) : CredentialStore {
        private val values = mutableMapOf<String, String>()

        override fun saveCredential(key: String, value: String) {
            if (throwOnWrite) error("Keystore unavailable")
            if (persistWrites) values[key] = value
        }

        override fun getCredential(key: String): String? = values[key]

        override fun hasCredential(key: String): Boolean = key in values

        override fun removeCredential(key: String) {
            values.remove(key)
        }

        override fun clearAll() {
            values.clear()
        }
    }
}
