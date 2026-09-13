package com.agentickitchen.shared.ai.prompt

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
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

    @Test
    fun pantryAllowanceIsNotAnUpstreamHardFilterInNonStrictMode() {
        val context = PromptFactory.inventoryRecipeOptionsContext(
            inventoryLines = listOf("300 g tomato", "2 piece onion"),
            strictStock = false,
            maxMissingStaples = 2,
            servings = 3,
            prioritizedIngredients = listOf("tomato")
        )

        assertContains(context, "authoritative for Ready Now / Missing 1 / Missing 2 / AI Ideas classification")
        assertContains(context, "at most 2 missing item(s)")
        assertContains(context, "NOT as a hard generation filter")
        assertContains(context, "option 3 may exceed the preparation allowance")
        assertContains(context, "Servings: 3")
        assertContains(context, "Prioritize: tomato")
    }

    @Test
    fun strictPantryModeRemainsAHardStockConstraint() {
        val context = PromptFactory.inventoryRecipeOptionsContext(
            inventoryLines = listOf("300 g tomato"),
            strictStock = true,
            maxMissingStaples = 0,
            servings = 2,
            prioritizedIngredients = emptyList()
        )

        assertContains(context, "Strict stock mode is ON")
        assertContains(context, "Every proposed option must be fully preparable")
        assertFalse(context.contains("option 3 may exceed the preparation allowance"))
    }

    @Test
    fun pantryContextIsEmptyWithoutInventorySnapshot() {
        assertEquals(
            "",
            PromptFactory.inventoryRecipeOptionsContext(
                inventoryLines = emptyList(),
                strictStock = false,
                maxMissingStaples = 2,
                servings = 2,
                prioritizedIngredients = emptyList()
            )
        )
    }
}
