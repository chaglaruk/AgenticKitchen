package com.agentickitchen.android.ai

import com.agentickitchen.android.CookingProviderSelection
import com.agentickitchen.android.HardwareSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FirebaseManagedDefaultsTest {
    @Test
    fun `fresh settings select managed Firebase without a user API key`() {
        val settings = HardwareSettings()
        assertEquals(CookingProviderSelection.Firebase, settings.aiProvider)
        assertEquals(CookingProviderSelection.Firebase, CookingProviderSelection.normalize(settings.aiProvider))
        assertFalse(CookingProviderSelection.needsApiKey(settings))
    }

    @Test
    fun `managed provider uses the audited stable model`() {
        assertEquals("gemini-3.7-flash", FirebaseAiProvider.MODEL)
    }
}
