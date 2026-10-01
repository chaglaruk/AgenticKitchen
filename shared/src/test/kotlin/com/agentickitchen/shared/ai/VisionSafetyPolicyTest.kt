package com.agentickitchen.shared.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisionSafetyPolicyTest {
    @Test
    fun `low confidence shopping candidates are removed`() {
        val filtered = VisionSafetyPolicy.filterShoppingCandidates(
            ShoppingImportResponse(
                listOf(
                    candidate("Certain tomato", 0.9),
                    candidate("Maybe onion", 0.4),
                    candidate("", 0.99)
                )
            )
        )

        assertEquals(listOf("Certain tomato"), filtered.items.map { it.displayName })
    }

    @Test
    fun `cooking photo always requires manual confirmation`() {
        val safe = VisionSafetyPolicy.requireUserConfirmation(
            CookingPhotoResponse(
                assessment = "Browning",
                visibleObservation = "Surface looks brown",
                immediateAction = "Turn off heat",
                recheckAfterSeconds = 2,
                uncertainty = "Lighting may affect colour"
            ),
            language = "English"
        )

        assertTrue(safe.immediateAction.contains("Do not change heat"))
        assertTrue(safe.safetyWarning.orEmpty().contains("food thermometer"))
        assertEquals(15, safe.recheckAfterSeconds)
        assertFalse(safe.immediateAction.contains("Turn off heat"))
    }

    @Test
    fun `blank structured cooking photo response fails validation`() {
        assertFalse(
            VisionSafetyPolicy.validateCookingPhoto(
                CookingPhotoResponse("", "visible", "action", uncertainty = "uncertain")
            )
        )
    }

    private fun candidate(name: String, confidence: Double) = ShoppingCandidate(
        displayName = name,
        confidence = confidence,
        estimated = false
    )
}
