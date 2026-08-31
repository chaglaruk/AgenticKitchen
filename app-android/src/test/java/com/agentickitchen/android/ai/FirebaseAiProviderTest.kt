package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.RecipePhotoImportRequest
import com.agentickitchen.shared.ai.RecipeTextImportRequest
import com.agentickitchen.shared.ai.SubstitutionPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAiProviderTest {
    @Test
    fun `recipe options use reasoning schema and preserve managed model attribution`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(recipeOptionsJson, "reasoning-test-model")
        })

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
        assertEquals(FirebaseResponseKind.RECIPE_OPTIONS, responseKind)
        assertEquals(FirebaseAiTask.REASONING, responseKind?.task)
        assertEquals(3, result.getOrNull()?.options?.size)
        assertEquals("Pirinç ve Soğan Tavası", result.getOrNull()?.options?.first()?.name)
        val success = result as AiResult.Success<*>
        assertEquals(AiProviderId.FIREBASE, success.provider)
        assertEquals("reasoning-test-model", success.model)
    }

    @Test
    fun `cooking plan uses reasoning response kind`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(cookingPlanJson, "reasoning-test-model")
        })

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
        assertEquals(FirebaseResponseKind.COOKING_PLAN, responseKind)
        assertEquals(FirebaseAiTask.REASONING, responseKind?.task)
        assertEquals("Pirinç ve Soğan Tavası", result.getOrNull()?.recipeName)
        assertEquals("Pirinç", result.getOrNull()?.ingredients?.first()?.name)
    }

    @Test
    fun `substitution plan uses reasoning response kind`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(substitutionJson, "reasoning-test-model")
        })
        val plan = CookingPlanResponse(
            "Rice", 2,
            listOf(PlannedIngredientDto("Rice", 160.0, "g", "rice"), PlannedIngredientDto("Onion", 1.0, "piece", "onion")),
            listOf(CookingStepDto("s1", "prep", "Prep", "counter", 60)),
            emptyList()
        )
        val result = provider.generateSubstitution(
            SubstitutionPlanRequest(plan, "Onion", listOf("2 clove Garlic"), setOf("pan"), "electric", 9, false, false, false, "none", emptySet(), "English")
        )
        assertTrue(result is AiResult.Success)
        assertEquals(FirebaseResponseKind.SUBSTITUTION_PLAN, responseKind)
        assertEquals(FirebaseAiTask.REASONING, responseKind?.task)
        assertEquals("Garlic", result.getOrNull()?.replacementIngredient?.name)
    }

    @Test
    fun `shopping photo uses extraction model class and forwards image`() = runBlocking {
        var imageSeen = false
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, image ->
            responseKind = kind
            imageSeen = image?.bytes?.contentEquals(byteArrayOf(1, 2, 3)) == true
            FirebaseGatewayResponse(
                """{"items":[{"displayName":"Domates","quantity":2.0,"unit":"adet","unitDimension":"count","confidence":0.9,"estimated":false}]}""",
                "extraction-test-model"
            )
        })

        val result = provider.scanShoppingPhoto(
            com.agentickitchen.shared.ai.ShoppingPhotoRequest(
                KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"),
                "Türkçe"
            )
        )

        assertTrue(imageSeen)
        assertEquals(FirebaseResponseKind.SHOPPING_IMPORT, responseKind)
        assertEquals(FirebaseAiTask.EXTRACTION, responseKind?.task)
        assertTrue(result is AiResult.Success)
    }

    @Test
    fun `recipe text import uses extraction class and normalizes source`() = runBlocking {
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            responseKind = kind
            FirebaseGatewayResponse(recipeImportJson, "extraction-test-model")
        })
        val result = provider.parseRecipeText(RecipeTextImportRequest("ambiguous recipe", "English", "shared"))
        assertEquals(FirebaseResponseKind.RECIPE_IMPORT_TEXT, responseKind)
        assertEquals(FirebaseAiTask.EXTRACTION, responseKind?.task)
        assertTrue(result is AiResult.Success)
        assertEquals(com.agentickitchen.shared.ai.RecipeImportSource.AI_TEXT, result.getOrNull()?.source)
        assertEquals("shared", result.getOrNull()?.recipe?.sourceLabel)
    }

    @Test
    fun `recipe photo import uses vision class and forwards image`() = runBlocking {
        var imageSeen = false
        var responseKind: FirebaseResponseKind? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, image ->
            responseKind = kind
            imageSeen = image?.bytes?.contentEquals(byteArrayOf(4, 5, 6)) == true
            FirebaseGatewayResponse(recipeImportJson.replace("AI_TEXT", "AI_PHOTO"), "vision-test-model")
        })
        val result = provider.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(4, 5, 6), "image/jpeg"), "English"))
        assertTrue(imageSeen)
        assertEquals(FirebaseResponseKind.RECIPE_IMPORT_PHOTO, responseKind)
        assertEquals(FirebaseAiTask.VISION, responseKind?.task)
        assertTrue(result is AiResult.Success)
    }

    @Test
    fun `cooking photo uses vision model class`() {
        assertEquals(FirebaseAiTask.VISION, FirebaseResponseKind.COOKING_PHOTO.task)
    }

    @Test
    fun `malformed JSON maps to invalid response`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse("not-json", "test-model")
        })

        val result = provider.generateRecipeOptions(
            RecipeOptionsRequest(emptyList(), emptySet(), "none", emptySet(), "Türkçe")
        )

        assertEquals(AiFailureType.InvalidResponse, result.failureOrNull()?.type)
    }

    @Test
    fun `blank managed response maps to invalid response`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse("", "test-model")
        })
        val result = provider.testConnection()
        assertEquals(AiFailureType.InvalidResponse, result.failureOrNull()?.type)
    }

    private companion object {
        val recipeOptionsJson = """
            {"options":[
              {"id":"r1","name":"Pirinç ve Soğan Tavası","summary":"Pratik tava yemeği","difficulty":"easy","estimatedMinutes":20,"requiredEquipment":["pan"],"missingIngredients":[],"proposedIngredients":[]},
              {"id":"r2","name":"Sebzeli Pirinç","summary":"Sebzeli sıcak kase","difficulty":"easy","estimatedMinutes":25,"requiredEquipment":["pan"],"missingIngredients":[],"proposedIngredients":[]},
              {"id":"r3","name":"Soğanlı Pirinç","summary":"Sade ve hızlı","difficulty":"easy","estimatedMinutes":18,"requiredEquipment":["pan"],"missingIngredients":[],"proposedIngredients":[]}
            ]}
        """.trimIndent()

        val substitutionJson = """
            {"originalIngredientName":"Onion","replacementIngredient":{"name":"Garlic","quantity":2.0,"unit":"clove","canonicalIngredientId":"garlic"},"reason":"Available aromatic substitute","confidence":0.8,
             "mutatedPlan":{"recipeName":"Rice","servings":2,
             "ingredients":[{"name":"Rice","quantity":160.0,"unit":"g","canonicalIngredientId":"rice"},{"name":"Garlic","quantity":2.0,"unit":"clove","canonicalIngredientId":"garlic"}],
             "steps":[{"id":"s1","type":"prep","instruction":"Prep garlic","resource":"counter","durationSeconds":60,"dependsOn":[],"visionCheckpointRecommended":false}],"safetyNotes":[]}}
        """.trimIndent()

        val recipeImportJson = """
            {"recipe":{"name":"Tomato Rice","servings":2,"ingredients":[{"displayName":"Rice","quantity":200.0,"unit":"g","confidence":0.98},{"displayName":"Tomato","quantity":2.0,"unit":"adet","confidence":0.95}],"instructions":["Cook rice with tomato."]},"confidence":0.95,"source":"AI_TEXT"}
        """.trimIndent()

        val cookingPlanJson = """
            {"recipeName":"Pirinç ve Soğan Tavası","servings":2,
             "ingredients":[{"name":"Pirinç","quantity":100.0,"unit":"g"},{"name":"Soğan","quantity":1.0,"unit":"adet"}],
             "steps":[{"id":"step_1","type":"prep","instruction":"Pirinci yıka ve süz.","resource":"counter","durationSeconds":120,"dependsOn":[],"visionCheckpointRecommended":false}],
             "safetyNotes":[]}
        """.trimIndent()
    }
}
