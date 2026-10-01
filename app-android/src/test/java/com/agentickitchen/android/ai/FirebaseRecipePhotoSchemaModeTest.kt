package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeImportSource
import com.agentickitchen.shared.ai.RecipePhotoImportRequest
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.Schema
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class FirebaseRecipePhotoSchemaModeTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun silenceDiagnosticsLogcat() {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    private fun GenerationConfig.getResponseSchema(): Schema? {
        val field = GenerationConfig::class.java.getDeclaredField("responseSchema")
        field.isAccessible = true
        return field.get(this) as? Schema
    }

    private fun GenerationConfig.getResponseMimeType(): String? {
        val field = GenerationConfig::class.java.getDeclaredField("responseMimeType")
        field.isAccessible = true
        return field.get(this) as? String
    }

    @Test
    fun `STRICT_SCHEMA requests pass their schema into generation configuration`() {
        val strictKinds = listOf(
            FirebaseResponseKind.RECIPE_OPTIONS,
            FirebaseResponseKind.SUBSTITUTION_PLAN,
            FirebaseResponseKind.SHOPPING_IMPORT,
            FirebaseResponseKind.RECIPE_IMPORT_TEXT,
            FirebaseResponseKind.RECIPE_IMPORT_PHOTO,
            FirebaseResponseKind.COOKING_PHOTO,
            FirebaseResponseKind.COOKING_CHAT,
            FirebaseResponseKind.CONNECTION_TEST
        )
        for (kind in strictKinds) {
            assertEquals("kind ${kind.name} must be STRICT_SCHEMA", FirebaseSchemaMode.STRICT_SCHEMA, kind.schemaMode)
            val config = buildGenerationConfig(kind)
            assertEquals("application/json", config.getResponseMimeType())
            assertEquals("kind ${kind.name} must pass schema into config", kind.schema, config.getResponseSchema())
            assertNotNull("kind ${kind.name} schema must not be null", config.getResponseSchema())
        }
    }

    @Test
    fun `JSON_ONLY requests do not send a responseSchema`() {
        val jsonOnlyKinds = listOf(
            FirebaseResponseKind.COOKING_PLAN
        )
        for (kind in jsonOnlyKinds) {
            assertEquals("kind ${kind.name} must be JSON_ONLY", FirebaseSchemaMode.JSON_ONLY, kind.schemaMode)
            val config = buildGenerationConfig(kind)
            assertEquals("application/json", config.getResponseMimeType())
            assertNull("kind ${kind.name} must not have responseSchema in generationConfig", config.getResponseSchema())
        }
    }

    @Test
    fun `RECIPE_IMPORT_PHOTO is STRICT_SCHEMA with recipeImport schema`() {
        assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, FirebaseResponseKind.RECIPE_IMPORT_PHOTO.schemaMode)
        assertNotNull(FirebaseResponseKind.RECIPE_IMPORT_PHOTO.schema)
        val config = buildGenerationConfig(FirebaseResponseKind.RECIPE_IMPORT_PHOTO)
        assertEquals("application/json", config.getResponseMimeType())
        assertEquals(FirebaseResponseKind.RECIPE_IMPORT_PHOTO.schema, config.getResponseSchema())
    }

    @Test
    fun `COOKING_PLAN remains JSON_ONLY with null responseSchema`() {
        assertEquals(FirebaseSchemaMode.JSON_ONLY, FirebaseResponseKind.COOKING_PLAN.schemaMode)
        assertNull(FirebaseResponseKind.COOKING_PLAN.schema)
        val config = buildGenerationConfig(FirebaseResponseKind.COOKING_PLAN)
        assertEquals("application/json", config.getResponseMimeType())
        assertNull(config.getResponseSchema())
    }

    @Test
    fun `RECIPE_IMPORT_TEXT remains STRICT_SCHEMA`() {
        assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, FirebaseResponseKind.RECIPE_IMPORT_TEXT.schemaMode)
        assertNotNull(FirebaseResponseKind.RECIPE_IMPORT_TEXT.schema)
    }

    @Test
    fun `SHOPPING_IMPORT remains STRICT_SCHEMA`() {
        assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, FirebaseResponseKind.SHOPPING_IMPORT.schemaMode)
        assertNotNull(FirebaseResponseKind.SHOPPING_IMPORT.schema)
    }

    @Test
    fun `COOKING_PHOTO remains STRICT_SCHEMA`() {
        assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, FirebaseResponseKind.COOKING_PHOTO.schemaMode)
        assertNotNull(FirebaseResponseKind.COOKING_PHOTO.schema)
    }

    @Test
    fun `Recipe Photo success still decodes RecipeImportResponse, runs validation, and normalizes`() = runBlocking {
        val rawJson = """
            {
              "recipe": {
                "name": "  Muzlu Yogurt Bardagi  ",
                "servings": 2,
                "ingredients": [
                  {"displayName": "Muz", "quantity": 2.0, "unit": "adet"},
                  {"displayName": "Yogurt", "quantity": 200.0, "unit": "g"},
                  {"displayName": "Sut", "quantity": 100.0, "unit": "ml"}
                ],
                "instructions": ["Muzlari dogra.", "Karistir."]
              },
              "confidence": 0.95,
              "source": "AI_PHOTO"
            }
        """.trimIndent()
        val provider = FirebaseAiProvider(FirebaseModelGateway { kind, _, _ ->
            assertEquals(FirebaseResponseKind.RECIPE_IMPORT_PHOTO, kind)
            assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, kind.schemaMode)
            FirebaseGatewayResponse(rawJson, "gemini-3.5-flash-lite")
        })
        val result = provider.scanRecipePhoto(
            RecipePhotoImportRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "Türkçe", "Tarif fotoğrafı")
        )
        assertTrue("result is Success", result is AiResult.Success)
        val response = (result as AiResult.Success).value
        assertEquals(RecipeImportSource.AI_PHOTO, response.source)
        assertEquals("Tarif fotoğrafı", response.recipe.sourceLabel)
        assertEquals("Muzlu Yogurt Bardagi", response.recipe.name) // trimmed by normalizer
        assertEquals(2, response.recipe.servings)
        assertEquals(3, response.recipe.ingredients.size)
        assertEquals(2, response.recipe.instructions.size)
    }

    @Test
    fun `recipe photo empty response maps to EMPTY_RESPONSE`() = runBlocking {
        val emittedLines = mutableListOf<String>()
        FirebaseAiDiagnostics.emit = { _, msg -> emittedLines.add(msg) }
        try {
            val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
                FirebaseGatewayResponse("", "gemini-3.5-flash-lite")
            })
            val result = provider.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
            assertTrue(result is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
            assertEquals("empty_response", result.technicalMessage)
            val failureLine = emittedLines.firstOrNull { it.contains("result=FAILURE") }
            assertNotNull(failureLine)
            assertTrue(failureLine!!.contains("category=EMPTY_RESPONSE"))
            assertTrue(failureLine.contains("retryable=true"))
            assertTrue(failureLine.contains("schemaMode=STRICT_SCHEMA"))
        } finally {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    @Test
    fun `recipe photo malformed JSON maps to JSON_DECODE_FAILURE`() = runBlocking {
        val emittedLines = mutableListOf<String>()
        FirebaseAiDiagnostics.emit = { _, msg -> emittedLines.add(msg) }
        try {
            val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
                FirebaseGatewayResponse("{not-valid-json", "gemini-3.5-flash-lite")
            })
            val result = provider.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "Türkçe"))
            assertTrue(result is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
            assertEquals("json_decode_failure", result.technicalMessage)
            val failureLine = emittedLines.firstOrNull { it.contains("result=FAILURE") }
            assertNotNull(failureLine)
            assertTrue(failureLine!!.contains("category=JSON_DECODE_FAILURE"))
            assertTrue(failureLine.contains("retryable=true"))
            assertTrue(failureLine.contains("schemaMode=STRICT_SCHEMA"))
        } finally {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    @Test
    fun `recipe photo structurally invalid recipe responses map to RESPONSE_VALIDATION_FAILURE`() = runBlocking {
        val emittedLines = mutableListOf<String>()
        FirebaseAiDiagnostics.emit = { _, msg -> emittedLines.add(msg) }
        try {
            // Blank recipe name
            val blankNameJson = """{"recipe":{"name":"","servings":1,"ingredients":[{"displayName":"A","quantity":1.0,"unit":"g"}],"instructions":["Cook"]},"confidence":0.9,"source":"AI_PHOTO"}"""
            val provider1 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
                FirebaseGatewayResponse(blankNameJson, "gemini-3.5-flash-lite")
            })
            val result1 = provider1.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
            assertTrue(result1 is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result1 as AiResult.Failure).type)
            assertEquals("response_validation_failure", result1.technicalMessage)
            val failureLine1 = emittedLines.lastOrNull { it.contains("result=FAILURE") }
            assertNotNull(failureLine1)
            assertTrue(failureLine1!!.contains("category=RESPONSE_VALIDATION_FAILURE"))
            assertTrue(failureLine1.contains("retryable=false"))
            assertTrue(failureLine1.contains("schemaMode=STRICT_SCHEMA"))

            // Empty ingredients
            val emptyIngredientsJson = """{"recipe":{"name":"Soup","servings":1,"ingredients":[],"instructions":["Cook"]},"confidence":0.9,"source":"AI_PHOTO"}"""
            val provider2 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
                FirebaseGatewayResponse(emptyIngredientsJson, "gemini-3.5-flash-lite")
            })
            val result2 = provider2.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
            assertTrue(result2 is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result2 as AiResult.Failure).type)
            assertEquals("response_validation_failure", result2.technicalMessage)

            // Empty instructions
            val emptyInstructionsJson = """{"recipe":{"name":"Soup","servings":1,"ingredients":[{"displayName":"A","quantity":1.0,"unit":"g"}],"instructions":[]},"confidence":0.9,"source":"AI_PHOTO"}"""
            val provider3 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
                FirebaseGatewayResponse(emptyInstructionsJson, "gemini-3.5-flash-lite")
            })
            val result3 = provider3.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
            assertTrue(result3 is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result3 as AiResult.Failure).type)
            assertEquals("response_validation_failure", result3.technicalMessage)
        } finally {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    @Test
    fun `diagnostics report kind=RECIPE_IMPORT_PHOTO schemaMode=STRICT_SCHEMA without logging sensitive content`() {
        val emittedLines = mutableListOf<String>()
        FirebaseAiDiagnostics.emit = { _, msg -> emittedLines.add(msg) }
        try {
            FirebaseAiDiagnostics.logRequest(FirebaseResponseKind.RECIPE_IMPORT_PHOTO, "gemini-3.5-flash-lite")
            FirebaseAiDiagnostics.logOutcome(
                FirebaseResponseKind.RECIPE_IMPORT_PHOTO,
                AiResult.Success(
                    value = "dummy",
                    provider = AiProviderId.FIREBASE,
                    model = "gemini-3.5-flash-lite"
                )
            )
            assertEquals(2, emittedLines.size)
            val reqLine = emittedLines[0]
            val outcomeLine = emittedLines[1]
            assertTrue(reqLine.contains("kind=RECIPE_IMPORT_PHOTO"))
            assertTrue(reqLine.contains("schemaMode=STRICT_SCHEMA"))
            assertTrue(reqLine.contains("result=REQUEST"))
            assertTrue(reqLine.contains("model=gemini-3.5-flash-lite"))

            assertTrue(outcomeLine.contains("kind=RECIPE_IMPORT_PHOTO"))
            assertTrue(outcomeLine.contains("schemaMode=STRICT_SCHEMA"))
            assertTrue(outcomeLine.contains("result=SUCCESS"))

            for (line in emittedLines) {
                assertFalse("no prompt text leaked", line.contains("prompt", ignoreCase = true))
                assertFalse("no image data leaked", line.contains("image", ignoreCase = true))
                assertFalse("no recipe name leaked", line.contains("Muzlu", ignoreCase = true))
            }
        } finally {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }
}
