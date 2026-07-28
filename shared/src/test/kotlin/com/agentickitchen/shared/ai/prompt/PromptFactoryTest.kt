package com.agentickitchen.shared.ai.prompt

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class PromptFactoryTest {
    @Test
    fun gasPromptUsesQualitativeFlameGuidance() {
        val prompt = PromptFactory.cookingPlanPrompt(
            "Menemen", listOf("egg"), setOf("gas"), 2, "gas", 9,
            false, false, false, "none", emptySet(), "English"
        )

        assertContains(prompt, "low, medium-low, medium, medium-high, or high")
        assertContains(prompt, "powerLevel\": null")
        assertFalse(prompt.contains("from 1 to 9"))
    }

    @Test
    fun electricPromptUsesConfiguredMaximum() {
        val prompt = PromptFactory.cookingPlanPrompt(
            "Pasta", listOf("pasta"), setOf("elec"), 4, "electric", 11,
            false, false, false, "none", emptySet(), "English"
        )

        assertContains(prompt, "Electric stove maximum level: 11")
        assertContains(prompt, "from 1 to 11")
        assertContains(prompt, "Servings: 4")
    }
}
