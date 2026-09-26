package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Fail-closed contract for the managed Recipe Options response: every option must carry a
 * usable complete proposedIngredients bill (nonempty, positive finite quantities, nonblank
 * names/units) or the provider result is InvalidResponse. An unusable option must never be
 * silently downgraded to an AI idea.
 */
class RecipeOptionsProviderContractTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun silenceDiagnosticsLogcat() {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    private fun provider(json: String) = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
        FirebaseGatewayResponse(json, "contract-test-model")
    })

    private fun optionsJson(optionIngredientBlocks: List<String>) = """
        {"options": [
            ${optionIngredientBlocks.joinToString(",\n")}
        ]}
    """.trimIndent()

    private fun optionBlock(
        id: String,
        ingredientLines: List<String>
    ) = """
        {"id": "$id", "name": "Test Recipe $id", "summary": "Summary", "difficulty": "easy",
         "estimatedMinutes": 20, "requiredEquipment": ["pan"], "missingIngredients": [],
         "proposedIngredients": [${ingredientLines.joinToString(",\n")}]}
    """.trimIndent()

    private fun ingredient(name: String, quantity: String, unit: String) =
        """{"name": "$name", "quantity": $quantity, "unit": "$unit"}"""

    private fun request() = RecipeOptionsRequest(
        ingredients = listOf("Yumurta"),
        equipment = setOf("pan"),
        dietType = "none",
        allergies = emptySet(),
        language = "Türkçe"
    )

    private suspend fun validate(json: String): AiResult<com.agentickitchen.shared.ai.dto.RecipeOptionsResponse> =
        provider(json).generateRecipeOptions(request())

    @Test
    fun `valid complete options are accepted`() = runBlocking {
        val json = optionsJson(
            listOf(
                optionBlock("o1", listOf(ingredient("Ekmek", "200", "g"), ingredient("Yumurta", "2", "adet"))),
                optionBlock("o2", listOf(ingredient("Sut", "250", "ml"))),
                optionBlock("o3", listOf(ingredient("Yogurt", "150", "g")))
            )
        )
        val result = validate(json)
        assertTrue(result is AiResult.Success)
        assertEquals(3, result.getOrNull()?.options?.size)
    }

    @Test
    fun `empty proposedIngredients is rejected`() = runBlocking {
        val json = optionsJson(
            listOf(
                optionBlock("o1", listOf(ingredient("Ekmek", "200", "g"))),
                optionBlock("o2", listOf(ingredient("Sut", "250", "ml"))),
                """{"id": "o3", "name": "Empty Bill", "summary": "Summary", "difficulty": "easy",
                    "estimatedMinutes": 20, "requiredEquipment": [], "missingIngredients": [],
                    "proposedIngredients": []}"""
            )
        )
        val result = validate(json)
        assertTrue(result is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
    }

    @Test
    fun `zero negative and nonfinite quantities are rejected`() = runBlocking {
        for (quantity in listOf("0", "-5", "NaN")) {
            val json = optionsJson(
                listOf(
                    optionBlock("o1", listOf(ingredient("Ekmek", "200", "g"))),
                    optionBlock("o2", listOf(ingredient("Sut", "250", "ml"))),
                    optionBlock("o3", listOf(ingredient("Yogurt", quantity, "g")))
                )
            )
            val result = validate(json)
            assertTrue("quantity $quantity must fail", result is AiResult.Failure)
            assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
        }
    }

    @Test
    fun `blank ingredient name is rejected`() = runBlocking {
        val json = optionsJson(
            listOf(
                optionBlock("o1", listOf(ingredient("Ekmek", "200", "g"))),
                optionBlock("o2", listOf(ingredient("Sut", "250", "ml"))),
                optionBlock("o3", listOf(ingredient("   ", "100", "g")))
            )
        )
        val result = validate(json)
        assertTrue(result is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
    }

    @Test
    fun `blank unit is rejected`() = runBlocking {
        val json = optionsJson(
            listOf(
                optionBlock("o1", listOf(ingredient("Ekmek", "200", "g"))),
                optionBlock("o2", listOf(ingredient("Sut", "250", "ml"))),
                optionBlock("o3", listOf(ingredient("Yogurt", "100", "  ")))
            )
        )
        val result = validate(json)
        assertTrue(result is AiResult.Failure)
        assertEquals(AiFailureType.InvalidResponse, (result as AiResult.Failure).type)
    }
}
