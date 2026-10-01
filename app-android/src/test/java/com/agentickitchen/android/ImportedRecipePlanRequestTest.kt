package com.agentickitchen.android

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedRecipePlanRequestTest {
    private val recipe = ImportedRecipe(
        name = "Edited soup",
        servings = 3,
        ingredients = listOf(
            ImportedRecipeIngredient("Milk", 175.0, "ml", "milk", rawText = "100 ml milk"),
            ImportedRecipeIngredient("Egg", 2.0, "adet", "egg")
        ),
        instructions = listOf("Mix.", "Cook. Test edit")
    )
    private val bill = listOf(
        PlannedIngredientDto("Milk", 175.0, "ml", "milk"),
        PlannedIngredientDto("Egg", 2.0, "adet", "egg")
    )
    private val pantry = listOf("800 ml Pantry milk", "400 g Pantry yogurt")

    private fun request() = buildImportedCookingPlanRequest(
        recipe = recipe,
        authoritativeBill = bill,
        equipment = setOf("pan"),
        stoveType = "electric",
        stoveMaxLevel = 9,
        ovenAvailable = true,
        ovenHasFan = false,
        airfryerAvailable = false,
        dietType = "none",
        allergies = emptySet(),
        language = "English",
        inventoryLines = pantry
    )

    @Test
    fun `request uses one authoritative bill for names and selected ingredients`() {
        val request = request()
        assertEquals(bill, request.selectedRecipeIngredients)
        assertEquals(bill.map { it.name }, request.ingredients)
        assertEquals(175.0, request.selectedRecipeIngredients.first().quantity, 0.0)
    }

    @Test
    fun `pantry remains separate stock context`() {
        val request = request()
        assertEquals(pantry, request.inventoryLines)
        assertFalse(request.ingredients.any { it.startsWith("Pantry") })
    }

    @Test
    fun `source ingredient and edited instruction context remain supplied`() {
        val request = request()
        assertEquals("100 ml milk", request.sourceRecipeIngredientLines.first())
        assertEquals(recipe.instructions, request.sourceRecipeInstructions)
        assertTrue(request.sourceRecipeInstructions.last().endsWith("Test edit"))
    }
}
