package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class RecipeImportPantryPlannerTest {
    private fun pantry(id: String, name: String, quantity: Double, unit: String, canonical: String? = null) = PantryStockItem(
        id = id,
        canonicalIngredientId = canonical,
        originalName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = InventoryUnits.normalize(quantity, unit).dimension,
        source = "test",
        createdAt = "2026-08-31T12:00:00Z",
        updatedAt = "2026-08-31T12:00:00Z"
    )

    @Test
    fun `classifies available partial missing and review-required ingredients`() {
        val recipe = ImportedRecipe(
            name = "Dinner",
            ingredients = listOf(
                ImportedRecipeIngredient("Rice", 200.0, "g", "rice"),
                ImportedRecipeIngredient("Tomato", 3.0, "adet", "tomato"),
                ImportedRecipeIngredient("Garlic", 1.0, "adet", "garlic"),
                ImportedRecipeIngredient("Salt", null, null, "salt")
            ),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(
                pantry("rice", "Pirinç", 500.0, "g", "rice"),
                pantry("tomato", "Domates", 2.0, "adet", "tomato")
            )
        )

        assertEquals(RecipeImportAvailability.AVAILABLE, summary.matches[0].availability)
        assertEquals(RecipeImportAvailability.PARTIAL, summary.matches[1].availability)
        assertEquals(RecipeImportAvailability.MISSING, summary.matches[2].availability)
        assertEquals(RecipeImportAvailability.NEEDS_REVIEW, summary.matches[3].availability)
        assertEquals(1, summary.availableCount)
        assertEquals(1, summary.partialCount)
        assertEquals(1, summary.missingCount)
        assertEquals(1, summary.needsReviewCount)
        assertFalse(summary.readyForValidatedPlan)
        assertTrue(summary.matches[2].canCheckSubstitution)
    }

    @Test
    fun `reserved quantities reduce imported recipe availability`() {
        val recipe = ImportedRecipe(
            name = "Rice",
            ingredients = listOf(ImportedRecipeIngredient("Rice", 300.0, "g", "rice")),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(pantry("rice", "Rice", 500.0, "g", "rice")),
            reservedByItem = mapOf("rice" to 250.0)
        )
        assertEquals(RecipeImportAvailability.PARTIAL, summary.matches.single().availability)
        assertEquals(250.0, summary.matches.single().availableQuantity ?: 0.0, 0.0001)
    }

    @Test
    fun `unknown measurement units fail closed to review`() {
        val recipe = ImportedRecipe(
            name = "Soup",
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "ladle", "milk")),
            instructions = listOf("Cook.")
        )
        val summary = RecipeImportPantryPlanner.compare(
            recipe,
            inventory = listOf(pantry("milk", "Milk", 500.0, "ml", "milk"))
        )
        assertEquals(RecipeImportAvailability.NEEDS_REVIEW, summary.matches.single().availability)
    }
}
