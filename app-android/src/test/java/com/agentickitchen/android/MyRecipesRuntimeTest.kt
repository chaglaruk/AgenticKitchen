package com.agentickitchen.android

import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.recipes.SavedRecipeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyRecipesRuntimeTest {
    @Test
    fun preparedPlanBecomesStructuredSavedRecipe() {
        val active = activePlan(sourceLabel = AiProviderId.FIREBASE.label)
        val saved = preparedRecipeForSaving(active)

        assertEquals("Tomato Rice", saved?.name)
        assertEquals(2, saved?.servings)
        assertEquals("Rice", saved?.ingredients?.first()?.displayName)
        assertEquals(200.0, saved?.ingredients?.first()?.quantity ?: 0.0, 0.0)
        assertEquals(listOf("Boil the rice", "Fold in tomato"), saved?.instructions)
    }

    @Test
    fun savedOptionIdsRoundTripAcrossActiveSessionPersistence() {
        assertEquals("abc-123", savedRecipeIdFromOptionId("saved:abc-123"))
        assertNull(savedRecipeIdFromOptionId("import-abc-123"))
        assertNull(savedRecipeIdFromOptionId("saved:"))
    }

    @Test
    fun stableIdsDeduplicateEquivalentPreparedRecipes() {
        val first = preparedRecipeForSaving(activePlan(sourceLabel = AiProviderId.FIREBASE.label))!!
        val second = preparedRecipeForSaving(activePlan(sourceLabel = AiProviderId.FIREBASE.label))!!
        assertEquals(stableSavedRecipeId(first), stableSavedRecipeId(second))

        val changed = second.copy(name = "Different")
        assertNotEquals(stableSavedRecipeId(first), stableSavedRecipeId(changed))
    }

    @Test
    fun sourceClassificationKeepsOfflineAndImportedRecipesDistinct() {
        assertEquals(SavedRecipeSource.GENERATED_OFFLINE, savedRecipeSourceFor(activePlan(AiProviderId.FREE.label)))
        assertEquals(SavedRecipeSource.GENERATED_AI, savedRecipeSourceFor(activePlan(AiProviderId.FIREBASE.label)))
        assertEquals(
            SavedRecipeSource.IMPORTED,
            savedRecipeSourceFor(activePlan(AiProviderId.FIREBASE.label, type = "imported"))
        )
    }

    private fun activePlan(sourceLabel: String, type: String = "generated") = PlanState.RecipeActive(
        sessionId = "session-1",
        recipe = RecipeOption(
            id = "option-1",
            type = type,
            name = "Tomato Rice",
            description = "Simple",
            sourceLabel = sourceLabel
        ),
        events = emptyList(),
        servings = 2,
        cookingPlan = CookingPlanResponse(
            recipeName = "Tomato Rice",
            servings = 2,
            ingredients = listOf(
                PlannedIngredientDto("Rice", 200.0, "g", "rice"),
                PlannedIngredientDto("Tomato", 2.0, "piece", "tomato")
            ),
            steps = listOf(
                CookingStepDto("1", "boil", "Boil the rice", "stovetop", 600),
                CookingStepDto("2", "mix", "Fold in tomato", "counter", 60, dependsOn = listOf("1"))
            ),
            safetyNotes = emptyList()
        )
    )
}
