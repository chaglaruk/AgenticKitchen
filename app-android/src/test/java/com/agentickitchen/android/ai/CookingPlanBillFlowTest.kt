package com.agentickitchen.android

import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.inventory.SelectedRecipeBill
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.android.ai.FirebaseAiDiagnostics
import com.agentickitchen.android.ai.FirebaseAiProvider
import com.agentickitchen.android.ai.FirebaseGatewayResponse
import com.agentickitchen.android.ai.FirebaseModelGateway
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Proves the provider-level data flow for the selected recipe bill: the Cooking Plan prompt
 * carries the selected bill as AUTHORITATIVE, the pantry stays separate as stock context, and
 * the ingredient line reflects the bill names rather than the pantry name list.
 */
class CookingPlanBillFlowTest {

    companion object {
        private var capturedPrompt: String? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            FirebaseAiDiagnostics.emit = { _, _ -> }
        }
    }

    private val bill = listOf(
        PlannedIngredientDto("ekmek", 200.0, "g"),
        PlannedIngredientDto("yumurta", 2.0, "adet"),
        PlannedIngredientDto("sut", 100.0, "ml"),
        PlannedIngredientDto("tereyagi", 20.0, "g")
    )

    private val pantryLines = listOf("1 g tereyagi", "500 ml zeytinyagi")

    private fun request() = CookingPlanRequest(
        recipeName = "Yumurtalı Ekmek Kızartması",
        ingredients = SelectedRecipeBill.names(SelectedRecipeBill.scaled(bill, selectionServings = 2, optionServings = 2)),
        equipment = setOf("pan"),
        servings = 2,
        stoveType = "electric",
        stoveMaxLevel = 9,
        ovenAvailable = true,
        ovenHasFan = false,
        airfryerAvailable = false,
        dietType = "none",
        allergies = emptySet(),
        language = "Türkçe",
        inventoryLines = pantryLines,
        selectedRecipeIngredients = bill
    )

    private fun runAndCapturePrompt(): String {
        var prompt: String? = null
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, p, _ ->
            prompt = p
            FirebaseGatewayResponse(
                """{"recipeName":"Yumurtalı Ekmek Kızartması","servings":2,
                    "ingredients":[{"name":"ekmek","quantity":200.0,"unit":"g"},{"name":"yumurta","quantity":2.0,"unit":"adet"},
                    {"name":"sut","quantity":100.0,"unit":"ml"},{"name":"tereyagi","quantity":20.0,"unit":"g"}],
                    "steps":[{"id":"s1","type":"prep","instruction":"Hazırla","resource":"bowl","durationSeconds":60,
                    "dependsOn":[],"visionCheckpointRecommended":false}],"safetyNotes":[]}""",
                "3.5-test-model"
            )
        })
        val result = runBlocking { provider.generateCookingPlan(request()) }
        assertTrue(result is AiResult.Success)
        return prompt ?: error("prompt was not captured")
    }

    @Test
    fun `prompt carries the authoritative selected bill`() {
        val prompt = runAndCapturePrompt()
        assertTrue(prompt.contains("Selected recipe ingredient bill (AUTHORITATIVE)"))
        assertTrue(prompt.contains("- 200 g ekmek"))
        assertTrue(prompt.contains("- 20 g tereyagi"))
    }

    @Test
    fun `pantry lines stay separate as stock context`() {
        val prompt = runAndCapturePrompt()
        assertTrue(prompt.contains("Available pantry quantities:"))
        assertTrue(prompt.contains("1 g tereyagi"))
        assertTrue(prompt.contains("500 ml zeytinyagi"))
    }

    @Test
    fun `ingredient line uses the selected bill names not the pantry name list`() {
        val prompt = runAndCapturePrompt()
        val ingredientsLine = prompt.lineSequence().firstOrNull { it.startsWith("Ingredients:") } ?: error("no Ingredients line")
        assertTrue("ekmek" in ingredientsLine)
        assertTrue("tereyagi" in ingredientsLine)
        assertFalse("pantry-only item must not appear as a recipe ingredient", "zeytinyagi" in ingredientsLine)
    }

    @Test
    fun `plan matching the bill passes the new contract at provider boundary`() {
        val provider = FirebaseAiProvider(FirebaseModelGateway { _, _, _ ->
            FirebaseGatewayResponse(
                """{"recipeName":"Yumurtalı Ekmek Kızartması","servings":2,
                    "ingredients":[{"name":"ekmek","quantity":200.0,"unit":"g"},{"name":"yumurta","quantity":2.0,"unit":"adet"},
                    {"name":"sut","quantity":100.0,"unit":"ml"},{"name":"tereyagi","quantity":20.0,"unit":"g"}],
                    "steps":[{"id":"s1","type":"prep","instruction":"Hazırla","resource":"bowl","durationSeconds":60,
                    "dependsOn":[],"visionCheckpointRecommended":false}],"safetyNotes":[]}""",
                "3.5-test-model"
            )
        })
        val result = runBlocking { provider.generateCookingPlan(request()) }
        assertTrue(result is AiResult.Success)
        assertEquals(4, result.getOrNull()?.ingredients?.size)
    }
}
