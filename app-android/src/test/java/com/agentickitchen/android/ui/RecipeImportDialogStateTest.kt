package com.agentickitchen.android.ui

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.ai.RecipeImportResponse
import com.agentickitchen.shared.ai.RecipeImportSource
import com.agentickitchen.shared.ai.recipeImportAmountReviewCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeImportDialogStateTest {
    @Test
    fun `recipe draft saver preserves edited fields`() {
        val edited = ImportedRecipe(
            name = "Edited tomato rice",
            servings = 3,
            ingredients = listOf(
                ImportedRecipeIngredient("Rice", 250.0, "g", "rice"),
                ImportedRecipeIngredient("Tomato", 3.0, "adet", "tomato")
            ),
            instructions = listOf("Rinse the rice.", "Cook with tomatoes."),
            sourceLabel = "Shared recipe",
            sourceUrl = "https://example.com/recipe"
        )

        val restored = decodeRecipeDraftFromSave(encodeRecipeDraftForSave(edited))

        assertEquals(edited, restored)
    }

    @Test
    fun `malformed saved draft is rejected safely`() {
        assertNull(decodeRecipeDraftFromSave("not-json"))
    }

    @Test
    fun `deterministic uncertainty localizes to Turkish`() {
        val response = uncertainResponse(2, recipeImportAmountReviewCode(2))
        assertEquals("2 malzeme miktarı kontrol edilmeli.", recipeImportUncertaintyText(response, isTurkish = true))
    }

    @Test
    fun `deterministic uncertainty uses natural English singular and plural`() {
        assertEquals(
            "1 ingredient amount needs review.",
            recipeImportUncertaintyText(uncertainResponse(1, recipeImportAmountReviewCode(1)), isTurkish = false)
        )
        assertEquals(
            "2 ingredient amounts need review.",
            recipeImportUncertaintyText(uncertainResponse(2, recipeImportAmountReviewCode(2)), isTurkish = false)
        )
    }

    @Test
    fun `provider uncertainty is preserved instead of rewritten`() {
        val response = uncertainResponse(1, "The photo is partly obscured")
        assertEquals("The photo is partly obscured", recipeImportUncertaintyText(response, isTurkish = true))
    }

    private fun uncertainResponse(count: Int, uncertainty: String?): RecipeImportResponse =
        RecipeImportResponse(
            recipe = ImportedRecipe(
                name = "Test recipe",
                servings = 2,
                ingredients = List(count) { index ->
                    ImportedRecipeIngredient(
                        displayName = "Ingredient $index",
                        quantity = null,
                        unit = null,
                        uncertaintyReason = "amount_not_explicit"
                    )
                },
                instructions = listOf("Cook safely.")
            ),
            confidence = 0.9,
            uncertainty = uncertainty,
            source = RecipeImportSource.URL_JSON_LD
        )
}