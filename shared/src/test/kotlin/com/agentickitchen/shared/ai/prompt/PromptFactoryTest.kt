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

    @Test
    fun noStovePromptProhibitsStoveHeating() {
        val prompt = PromptFactory.cookingPlanPrompt(
            "Salad", listOf("tomato"), setOf("knife", "bowl"), 2, "none", 9,
            false, false, false, "none", emptySet(), "English"
        )

        assertContains(prompt, "Stove type: none")
        assertContains(prompt, "Do not include stove-heating steps")
        assertContains(prompt, "\"powerLevel\": null")
        assertFalse(prompt.contains("Stove type: electric"))
    }

    @Test
    fun allergiesArePropagatedToProviderPrompts() {
        val prompt = PromptFactory.cookingPlanPrompt(
            "Soup", listOf("tomato"), setOf("elec"), 2, "electric", 9,
            false, false, false, "none", setOf("milk", "sesame"), "English"
        )

        assertContains(prompt, "Allergies: milk, sesame")
    }
}
