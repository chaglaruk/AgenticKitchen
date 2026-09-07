package com.agentickitchen.android.data.preferences

import android.content.SharedPreferences
import com.agentickitchen.android.security.CredentialStore
import com.agentickitchen.android.ui.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesManagerPersistenceTest {

    @Test
    fun saveThemePreferencePersistsAcrossManagerRecreation() {
        val backingStorage = FakeSharedPreferences()
        val credentialStore = TestCredentialStore()

        // 1. Initial state defaults to FOLLOW_SYSTEM
        val initialManager = PreferencesManager(backingStorage, credentialStore)
        assertEquals(ThemePreference.FOLLOW_SYSTEM.storageValue, initialManager.theme())

        // 2. Save each explicit theme preference and recreate manager to verify persistence
        ThemePreference.entries.forEach { preference ->
            val activeManager = PreferencesManager(backingStorage, credentialStore)
            activeManager.saveTheme(preference.storageValue)

            // Recreate PreferencesManager instance from the exact same backing storage
            val recreatedManager = PreferencesManager(backingStorage, credentialStore)
            assertEquals(
                "Theme ${preference.name} must persist across manager recreation",
                preference.storageValue,
                recreatedManager.theme()
            )
        }
    }

    @Test
    fun legacyPreferencesMigrateSafelyAcrossManagerRecreation() {
        val backingStorage = FakeSharedPreferences()
        val credentialStore = TestCredentialStore()

        val migrations = mapOf(
            "editorial" to ThemePreference.FOLLOW_SYSTEM.storageValue,
            "editorial-light" to ThemePreference.MODERN_MINIMAL_A.storageValue,
            "editorial_light" to ThemePreference.MODERN_MINIMAL_A.storageValue,
            "editorial-dark" to ThemePreference.LUXE_APPLIANCE_DARK_K.storageValue,
            "editorial_dark" to ThemePreference.LUXE_APPLIANCE_DARK_K.storageValue,
            "corrupt-value" to ThemePreference.FOLLOW_SYSTEM.storageValue
        )

        migrations.forEach { (legacyValue, expectedCanonical) ->
            backingStorage.edit().putString("theme", legacyValue).commit()

            val recreatedManager = PreferencesManager(backingStorage, credentialStore)
            assertEquals(
                "Legacy value '$legacyValue' should migrate to $expectedCanonical",
                expectedCanonical,
                recreatedManager.theme()
            )
        }
    }

    private class TestCredentialStore : CredentialStore {
        private val map = mutableMapOf<String, String>()
        override fun saveCredential(key: String, value: String) { map[key] = value }
        override fun getCredential(key: String): String? = map[key]
        override fun hasCredential(key: String): Boolean = key in map
        override fun removeCredential(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = HashMap(data)
        override fun getString(key: String?, defValue: String?): String? =
            data[key] as? String ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            (data[key] as? Set<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) {}

        private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) { pending[key] = value; removed.remove(key) }
                return this
            }

            override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
                if (key != null) { pending[key] = values?.toSet(); removed.remove(key) }
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) { pending[key] = value; removed.remove(key) }
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) { pending[key] = value; removed.remove(key) }
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) { pending[key] = value; removed.remove(key) }
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) { pending[key] = value; removed.remove(key) }
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) { removed.add(key); pending.remove(key) }
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                if (clearAll) storage.clear()
                removed.forEach { storage.remove(it) }
                pending.forEach { (k, v) ->
                    if (v == null) storage.remove(k) else storage[k] = v
                }
                return true
            }

            override fun apply() {
                commit()
            }
        }
    }
}
