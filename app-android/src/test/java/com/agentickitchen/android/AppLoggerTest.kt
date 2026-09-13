package com.agentickitchen.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppLoggerTest {
    @Test
    fun providerDiagnosticRetainsOnlySafePrimaryMetadata() {
        val event = AppLogger.providerDiagnosticEventCode(
            "provider=GEMINI feature=shopping_photo status=200 category=SUCCESS responseLength=1234 elapsedMs=987"
        )

        assertEquals("SHOPPING_PHOTO_200_SUCCESS", event)
    }

    @Test
    fun providerDiagnosticDistinguishesGenerateContentFallbackFailure() {
        val event = AppLogger.providerDiagnosticEventCode(
            "provider=GEMINI feature=shopping_photo_generate_content status=400 category=InvalidResponse responseLength=999"
        )

        assertEquals("SHOPPING_PHOTO_FALLBACK_400_INVALID_RESPONSE", event)
    }

    @Test
    fun providerDiagnosticDoesNotLeakUnknownMetadata() {
        val event = AppLogger.providerDiagnosticEventCode(
            "provider=GEMINI feature=secret_prompt status=999 category=api_key_value responseLength=777"
        )

        assertEquals("PROVIDER_EVENT_NONE_EVENT", event)
        assertFalse(event.contains("secret", ignoreCase = true))
        assertFalse(event.contains("key", ignoreCase = true))
    }
}
