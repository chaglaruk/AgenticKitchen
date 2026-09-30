package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeImportDraftPolicyTest {
    @Test fun culinaryVolumeUnitsNormalizeDeterministically() {
        assertEquals(240.0, InventoryUnits.normalize(1.0, "cup").quantity)
        assertEquals(30.0, InventoryUnits.normalize(2.0, "tbsp").quantity)
        assertEquals(5.0, InventoryUnits.normalize(1.0, "tsp").quantity)
        assertEquals(UnitDimension.VOLUME, InventoryUnits.normalize(1.0, "cup").dimension)
    }

    @Test fun completeImportedRecipeCanPrepare() {
        val recipe = ImportedRecipe(
            name = "Soup",
            servings = 2,
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "cup")),
            instructions = listOf("Warm the milk.")
        )
        assertTrue(RecipeImportDraftPolicy.canPrepare(recipe))
        assertTrue(RecipeImportDraftPolicy.issues(recipe).isEmpty())
    }

    @Test fun savedRecipeCountStyleSliceUnitsNormalizeDeterministically() {
        assertEquals(UnitDimension.COUNT, InventoryUnits.normalize(2.0, "slice").dimension)
        assertEquals(UnitDimension.COUNT, InventoryUnits.normalize(2.0, "dilim").dimension)
        val recipe = ImportedRecipe(
            name = "Toast",
            servings = 1,
            ingredients = listOf(ImportedRecipeIngredient("Bread", 2.0, "slice")),
            instructions = listOf("Toast the bread.")
        )
        assertTrue(RecipeImportDraftPolicy.canPrepare(recipe))
    }

    @Test fun missingServingsOrUnknownUnitFailsClosed() {
        val recipe = ImportedRecipe(
            name = "Soup",
            servings = null,
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "ladle")),
            instructions = listOf("Warm the milk.")
        )
        val issues = RecipeImportDraftPolicy.issues(recipe)
        assertFalse(RecipeImportDraftPolicy.canPrepare(recipe))
        assertTrue("servings_missing" in issues)
        assertTrue(issues.any { it.startsWith("ingredient_unit_") })
    }
}
