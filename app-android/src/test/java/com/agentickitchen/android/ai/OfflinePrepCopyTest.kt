package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.StructuredRecipeParser
import com.agentickitchen.shared.ai.prompt.PromptFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePrepCopyTest {
    @Test
    fun `turkish rice and produce prep is ingredient aware`() = runBlocking {
        val ingredients = listOf("Pirinç", "Domates", "Mantar")
        val equipment = setOf("elec", "pan")
        val provider = LocalRecipeProvider { }

        val optionsJson = provider.generateContent(
            PromptFactory.recipeOptionsPrompt(
                ingredients = ingredients,
                equipment = equipment,
                dietType = "none",
                allergies = emptySet(),
                language = "Türkçe"
            )
        )
        val recipeName = requireNotNull(StructuredRecipeParser.recipeOptions(optionsJson).getOrNull())
            .options.first().name

        val planJson = provider.generateContent(
            PromptFactory.cookingPlanPrompt(
                recipeName = recipeName,
                ingredients = ingredients,
                equipment = equipment,
                servings = 2,
                stoveType = "electric",
                stoveMaxLevel = 9,
                ovenAvailable = false,
                ovenHasFan = false,
                airfryerAvailable = false,
                dietType = "none",
                allergies = emptySet(),
                language = "Türkçe"
            )
        )
        val prep = requireNotNull(StructuredRecipeParser.cookingPlan(planJson).getOrNull())
            .steps.first().instruction

        assertTrue(prep.contains("Pirinç:"))
        assertTrue(prep.contains("süzgeçte"))
        assertTrue(prep.contains("süz"))
        assertTrue(prep.contains("Domates"))
        assertTrue(prep.contains("Mantar"))
        assertTrue(prep.contains("temizle"))
        assertFalse(prep.contains("Pirinç için gereken doğrama"))
        assertFalse(prep.contains("malzemelerini yıka; Pirinç"))
        assertFalse(prep.contains("Pirinç: temizle"))
    }
}
