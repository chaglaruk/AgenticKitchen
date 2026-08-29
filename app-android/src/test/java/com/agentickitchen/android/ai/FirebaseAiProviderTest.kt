package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAiProviderTest {
    @Test
    fun `recipe options are parsed from managed model JSON`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _ -> recipeOptionsJson })

        val result = provider.generateRecipeOptions(
            RecipeOptionsRequest(
                ingredients = listOf("Pirinç", "Soğan"),
                equipment = setOf("pan"),
                dietType = "none",
                allergies = emptySet(),
                language = "Türkçe"
            )
        )

        assertTrue(result is AiResult.Success)
        assertEquals(3, result.getOrNull()?.options?.size)
        assertEquals("Pirinç ve Soğan Tavası", result.getOrNull()?.options?.first()?.name)
    }

    @Test
    fun `cooking plan is parsed from managed model JSON`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _ -> cookingPlanJson })

        val result = provider.generateCookingPlan(
            CookingPlanRequest(
                recipeName = "Pirinç ve Soğan Tavası",
                ingredients = listOf("Pirinç", "Soğan"),
                equipment = setOf("pan"),
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

        assertTrue(result is AiResult.Success)
        assertEquals("Pirinç ve Soğan Tavası", result.getOrNull()?.recipeName)
        assertEquals("Pirinç", result.getOrNull()?.ingredients?.first()?.name)
    }

    @Test
    fun `photo request is forwarded to managed gateway`() = runBlocking {
        var imageSeen = false
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, image ->
            imageSeen = image?.bytes?.contentEquals(byteArrayOf(1, 2, 3)) == true
            """{"items":[{"displayName":"Domates","quantity":2.0,"unit":"adet","confidence":0.9,"estimated":false}]}"""
        })

        val result = provider.scanShoppingPhoto(
            com.agentickitchen.shared.ai.ShoppingPhotoRequest(
                KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"),
                "Türkçe"
            )
        )

        assertTrue(imageSeen)
        assertTrue(result is AiResult.Success)
    }

    @Test
    fun `malformed JSON maps to invalid response`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _ -> "not-json" })

        val result = provider.generateRecipeOptions(
            RecipeOptionsRequest(emptyList(), emptySet(), "none", emptySet(), "Türkçe")
        )

        assertEquals(AiFailureType.InvalidResponse, result.failureOrNull()?.type)
    }

    @Test
    fun `blank managed response maps to invalid response`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _ -> "" })
        val result = provider.testConnection()
        assertEquals(AiFailureType.InvalidResponse, result.failureOrNull()?.type)
    }

    private companion object {
        val recipeOptionsJson = """
            {"options":[
              {"id":"r1","name":"Pirinç ve Soğan Tavası","summary":"Pratik tava yemeği","difficulty":"easy","estimatedMinutes":20,"requiredEquipment":["pan"],"missingIngredients":[]},
              {"id":"r2","name":"Sebzeli Pirinç","summary":"Sebzeli sıcak kase","difficulty":"easy","estimatedMinutes":25,"requiredEquipment":["pan"],"missingIngredients":[]},
              {"id":"r3","name":"Soğanlı Pirinç","summary":"Sade ve hızlı","difficulty":"easy","estimatedMinutes":18,"requiredEquipment":["pan"],"missingIngredients":[]}
            ]}
        """.trimIndent()

        val cookingPlanJson = """
            {"recipeName":"Pirinç ve Soğan Tavası","servings":2,
             "ingredients":[{"name":"Pirinç","quantity":100.0,"unit":"g"},{"name":"Soğan","quantity":1.0,"unit":"adet"}],
             "steps":[{"id":"step_1","type":"prep","instruction":"Pirinci yıka ve süz.","resource":"counter","durationSeconds":120}],
             "safetyNotes":[]}
        """.trimIndent()
    }
}
