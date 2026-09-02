package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeImportPlanGuardTest {
    private val recipe = ImportedRecipe(
        name = "Tomato Rice",
        servings = 2,
        ingredients = listOf(
            ImportedRecipeIngredient("Rice", 200.0, "g", "rice"),
            ImportedRecipeIngredient("Tomato", 2.0, "adet", "tomato")
        ),
        instructions = listOf("Cook the rice with tomato.")
    )

    private fun plan(ingredients: List<PlannedIngredientDto>, servings: Int = 2, name: String = "Tomato Rice") = CookingPlanResponse(
        recipeName = name,
        servings = servings,
        ingredients = ingredients,
        steps = listOf(CookingStepDto("s1", "cook", "Cook.", "stove", 300)),
        safetyNotes = emptyList()
    )

    @Test fun equivalentUnitsAndExactIdentitiesPass() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", .2, "kg", "rice"), PlannedIngredientDto("Tomato", 2.0, "adet", "tomato")))
        )
        assertTrue(result.valid)
    }

    @Test fun addedOrRemovedIngredientFailsClosed() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", 200.0, "g", "rice"), PlannedIngredientDto("Onion", 1.0, "adet", "onion")))
        )
        assertFalse(result.valid)
        assertTrue("ingredient_missing" in result.reasons || "ingredient_added" in result.reasons)
    }

    @Test fun materialQuantityRewriteFailsClosed() {
        val result = RecipeImportPlanGuard.validate(
            recipe,
            plan(listOf(PlannedIngredientDto("Rice", 300.0, "g", "rice"), PlannedIngredientDto("Tomato", 2.0, "adet", "tomato")))
        )
        assertFalse(result.valid)
        assertTrue("ingredient_amount_changed" in result.reasons)
    }
}
