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
            FirebaseResponseKind.COOKING_PLAN,
            FirebaseResponseKind.RECIPE_IMPORT_PHOTO
        )
        for (kind in jsonOnlyKinds) {
            assertEquals("kind ${kind.name} must be JSON_ONLY", FirebaseSchemaMode.JSON_ONLY, kind.schemaMode)
            val config = buildGenerationConfig(kind)
            assertEquals("application/json", config.getResponseMimeType())
            assertNull("kind ${kind.name} must not have responseSchema in generationConfig", config.getResponseSchema())
        }
    }

    @Test
    fun `RECIPE_IMPORT_PHOTO is JSON_ONLY`() {
        assertEquals(FirebaseSchemaMode.JSON_ONLY, FirebaseResponseKind.RECIPE_IMPORT_PHOTO.schemaMode)
        assertNull(FirebaseResponseKind.RECIPE_IMPORT_PHOTO.schema)
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
            assertEquals(FirebaseSchemaMode.JSON_ONLY, kind.schemaMode)
            FirebaseGatewayResponse(rawJson, "gemini-3.7-flash")
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
    fun `recipe photo malformed JSON still fails closed`() = runBlocking {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse("{not-valid-json", "gemini-3.7-flash")
        })
        val result = provider.scanRecipePhoto(
            RecipePhotoImportRequest(KitchenImage(byteArrayOf(1, 2, 3), "image/jpeg"), "Türkçe")
        )
        assertTrue("result is Failure", result is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
    }

    @Test
    fun `recipe photo structurally invalid recipe responses still fail closed`() = runBlocking {
        // Blank recipe name
        val blankNameJson = """{"recipe":{"name":"","servings":1,"ingredients":[{"displayName":"A","quantity":1.0,"unit":"g"}],"instructions":["Cook"]},"confidence":0.9}"""
        val provider1 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse(blankNameJson, "gemini-3.7-flash")
        })
        val result1 = provider1.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
        assertTrue(result1 is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result1 as AiResult.Failure).type)

        // Empty ingredients
        val emptyIngredientsJson = """{"recipe":{"name":"Soup","servings":1,"ingredients":[],"instructions":["Cook"]},"confidence":0.9}"""
        val provider2 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse(emptyIngredientsJson, "gemini-3.7-flash")
        })
        val result2 = provider2.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
        assertTrue(result2 is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result2 as AiResult.Failure).type)

        // Empty instructions
        val emptyInstructionsJson = """{"recipe":{"name":"Soup","servings":1,"ingredients":[{"displayName":"A","quantity":1.0,"unit":"g"}],"instructions":[]},"confidence":0.9}"""
        val provider3 = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse(emptyInstructionsJson, "gemini-3.7-flash")
        })
        val result3 = provider3.scanRecipePhoto(RecipePhotoImportRequest(KitchenImage(byteArrayOf(1), "image/jpeg"), "Türkçe"))
        assertTrue(result3 is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result3 as AiResult.Failure).type)
    }

    @Test
    fun `diagnostics report kind=RECIPE_IMPORT_PHOTO schemaMode=JSON_ONLY without logging sensitive content`() {
        val emittedLines = mutableListOf<String>()
        FirebaseAiDiagnostics.emit = { _, msg -> emittedLines.add(msg) }
        try {
            FirebaseAiDiagnostics.logRequest(FirebaseResponseKind.RECIPE_IMPORT_PHOTO, "gemini-3.7-flash")
            FirebaseAiDiagnostics.logOutcome(
                FirebaseResponseKind.RECIPE_IMPORT_PHOTO,
                AiResult.Success(
                    value = "dummy",
                    provider = AiProviderId.FIREBASE,
                    model = "gemini-3.7-flash"
                )
            )
            assertEquals(2, emittedLines.size)
            val reqLine = emittedLines[0]
            val outcomeLine = emittedLines[1]
            assertTrue(reqLine.contains("kind=RECIPE_IMPORT_PHOTO"))
            assertTrue(reqLine.contains("schemaMode=JSON_ONLY"))
            assertTrue(reqLine.contains("result=REQUEST"))
            assertTrue(reqLine.contains("model=gemini-3.7-flash"))

            assertTrue(outcomeLine.contains("kind=RECIPE_IMPORT_PHOTO"))
            assertTrue(outcomeLine.contains("schemaMode=JSON_ONLY"))
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
