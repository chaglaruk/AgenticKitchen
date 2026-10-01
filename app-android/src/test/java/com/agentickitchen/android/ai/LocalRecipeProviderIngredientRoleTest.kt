package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.StructuredRecipeParser
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.prompt.PromptFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRecipeProviderIngredientRoleTest {
    @Test
    fun genericChickenAndFishRetainRawProteinSafetyRoles() = runBlocking {
        val chicken = plan("Tavuk", "Türkçe")
        assertTrue(chicken.safetyNotes.any { "74°C" in it })
        assertFalse(chicken.safetyNotes.any { "71°C" in it })

        val fish = plan("Balık", "Türkçe")
        assertTrue(fish.safetyNotes.any { "63°C" in it })
        assertFalse(fish.safetyNotes.any { "74°C" in it || "71°C" in it })
    }

    @Test
    fun allConcretePoultryCatalogEntriesUsePoultrySafetyCue() = runBlocking {
        listOf(
            "Chicken breast",
            "Chicken thighs",
            "Chicken drumsticks",
            "Chicken wings",
            "Whole chicken",
            "Turkey"
        ).forEach { ingredient ->
            val plan = plan(ingredient, "English")
            assertTrue("$ingredient should use poultry safety guidance", plan.safetyNotes.any { "74°C" in it })
            assertFalse("$ingredient must not use red-meat safety guidance", plan.safetyNotes.any { "71°C" in it })
        }
    }

    private suspend fun plan(ingredient: String, language: String): CookingPlanResponse {
        val response = LocalRecipeProvider { }.generateContent(
            PromptFactory.cookingPlanPrompt(
                recipeName = "$ingredient skillet",
                ingredients = listOf(ingredient),
                equipment = setOf("elec", "pan"),
                servings = 2,
                stoveType = "electric",
                stoveMaxLevel = 9,
                ovenAvailable = false,
                ovenHasFan = false,
                airfryerAvailable = false,
                dietType = "none",
                allergies = emptySet(),
                language = language
            )
        )
        return requireNotNull(StructuredRecipeParser.cookingPlan(response).getOrNull())
    }
}
