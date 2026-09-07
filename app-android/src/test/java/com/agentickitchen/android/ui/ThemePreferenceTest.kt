package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {
    @Test
    fun defaultAndLegacyValuesMigrateSafely() {
        assertEquals(ThemePreference.FOLLOW_SYSTEM, ThemePreference.fromStored(null))
        assertEquals(ThemePreference.FOLLOW_SYSTEM, ThemePreference.fromStored("editorial"))
        assertEquals(ThemePreference.MODERN_MINIMAL_A, ThemePreference.fromStored("editorial-light"))
        assertEquals(ThemePreference.LUXE_APPLIANCE_DARK_K, ThemePreference.fromStored("editorial-dark"))
        assertEquals(ThemePreference.FOLLOW_SYSTEM, ThemePreference.fromStored("corrupt-value"))
    }

    @Test
    fun followSystemResolvesToAOrK() {
        assertEquals(
            ThemePreference.MODERN_MINIMAL_A,
            ThemePreference.resolve("FOLLOW_SYSTEM", systemDark = false)
        )
        assertEquals(
            ThemePreference.LUXE_APPLIANCE_DARK_K,
            ThemePreference.resolve("FOLLOW_SYSTEM", systemDark = true)
        )
    }

    @Test
    fun explicitThemesIgnoreSystemMode() {
        ThemePreference.entries
            .filterNot { it == ThemePreference.FOLLOW_SYSTEM }
            .forEach { preference ->
                assertEquals(preference, ThemePreference.resolve(preference.storageValue, systemDark = false))
                assertEquals(preference, ThemePreference.resolve(preference.storageValue, systemDark = true))
            }
    }

    @Test
    fun preferenceStorageValuesRoundTrip() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, ThemePreference.fromStored(preference.storageValue))
        }
    }
}
