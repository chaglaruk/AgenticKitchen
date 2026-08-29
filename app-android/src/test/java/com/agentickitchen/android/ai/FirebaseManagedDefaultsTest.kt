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
    fun `managed provider defaults route extraction to lower cost model`() {
        val config = StaticFirebaseAiModelConfig()
        assertEquals("gemini-3.5-flash-lite", config.modelFor(FirebaseAiTask.EXTRACTION))
    }

    @Test
    fun `managed provider defaults keep reasoning and vision on audited flash model`() {
        val config = StaticFirebaseAiModelConfig()
        assertEquals("gemini-3.7-flash", config.modelFor(FirebaseAiTask.REASONING))
        assertEquals("gemini-3.7-flash", config.modelFor(FirebaseAiTask.VISION))
    }
}
