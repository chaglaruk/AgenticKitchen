package com.agentickitchen.android

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeSharePayloadTest {
    @Test fun acceptsOnlyNonBlankTextSendPayloads() {
        assertEquals(
            "https://example.com/recipe",
            recipeSharePayload(Intent.ACTION_SEND, "text/plain", "  https://example.com/recipe  ")
        )
        assertNull(recipeSharePayload(Intent.ACTION_VIEW, "text/plain", "https://example.com/recipe"))
        assertNull(recipeSharePayload(Intent.ACTION_SEND, "image/jpeg", "https://example.com/recipe"))
        assertNull(recipeSharePayload(Intent.ACTION_SEND, "text/plain", "   "))
    }
}
