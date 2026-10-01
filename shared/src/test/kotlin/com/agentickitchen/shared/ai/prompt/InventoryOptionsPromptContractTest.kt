package com.agentickitchen.shared.ai.prompt

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFails

/**
 * The pantry-backed Recipe Options context must teach the model a structured
 * proposedIngredients contract that the local matcher can compare deterministically.
 */
class InventoryOptionsPromptContractTest {

    private fun context(strictStock: Boolean) = PromptFactory.inventoryRecipeOptionsContext(
        inventoryLines = listOf("500 ml zeytinyagi", "4 adet yumurta"),
        strictStock = strictStock,
        maxMissingStaples = 1,
        servings = 2,
        prioritizedIngredients = listOf("tereyagi")
    )

    @Test
    fun `proposedIngredients is declared the authoritative bill of ingredients`() {
        assertContains(context(false), "proposedIngredients is the application's authoritative structured bill of ingredients")
        assertContains(context(false), "The app recomputes pantry coverage and shortages locally from it")
    }

    @Test
    fun `every required ingredient must be listed`() {
        assertContains(context(false), "Every ingredient actually required by the recipe MUST appear in proposedIngredients")
        assertContains(context(false), "Never mention an ingredient in the recipe name or summary while omitting it from proposedIngredients")
        assertContains(context(false), "Never return an empty proposedIngredients list")
    }

    @Test
    fun `unit dimensions must stay locally comparable`() {
        assertContains(context(false), "use a quantity and unit dimension compatible with the supplied pantry line")
        assertContains(context(false), "Do not arbitrarily turn a pantry weight into a count requirement or vice versa")
    }

    @Test
    fun `pantry overuse is only allowed as an intentional shortage`() {
        assertContains(context(false), "Do not require more of a pantry-covered ingredient than the supplied available amount unless that ingredient is intentionally one of the shortages")
    }

    @Test
    fun `missingIngredients is advisory only`() {
        assertContains(context(false), "missingIngredients is advisory only; the app recomputes shortages locally from proposedIngredients")
    }

    @Test
    fun `non strict allowance guidance and strict stock satisfiability`() {
        assertContains(context(false), "options 1 and 2 should stay within the user's missing-item allowance")
        assertContains(context(false), "option 3 may remain a broader AI idea")
        assertContains(context(true), "In strict-stock mode, every proposed ingredient must be satisfiable from the pantry quantity/unit data")
    }

    @Test
    fun `context carries the pantry lines servings and priority`() {
        val context = context(false)
        assertContains(context, "500 ml zeytinyagi")
        assertContains(context, "4 adet yumurta")
        assertContains(context, "Servings: 2")
        assertContains(context, "Prioritize: tereyagi")
    }
}
