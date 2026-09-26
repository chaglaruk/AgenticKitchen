package com.agentickitchen.shared.ai.prompt

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Cooking Plan prompt must carry the selected option's bill as the authoritative contract. */
class SelectedBillPromptContractTest {

    private val bill = listOf(
        PlannedIngredientDto("ekmek", 200.0, "g"),
        PlannedIngredientDto("yumurta", 2.0, "adet"),
        PlannedIngredientDto("sut", 100.0, "ml"),
        PlannedIngredientDto("tereyagi", 20.0, "g")
    )

    @Test
    fun `context declares the authoritative bill with structured lines`() {
        val context = PromptFactory.selectedRecipeBillContext(bill)
        assertContains(context, "Selected recipe ingredient bill (AUTHORITATIVE)")
        assertContains(context, "- 200 g ekmek")
        assertContains(context, "- 2 adet yumurta")
        assertContains(context, "- 100 ml sut")
        assertContains(context, "- 20 g tereyagi")
    }

    @Test
    fun `identity and amount preservation rules are stated`() {
        val context = PromptFactory.selectedRecipeBillContext(bill)
        assertContains(context, "plan.ingredients MUST contain the same ingredient identities as this bill")
        assertContains(context, "Do NOT add ingredients")
        assertContains(context, "Do NOT remove ingredients")
        assertContains(context, "Do NOT substitute ingredients at cooking plan generation time")
        assertContains(context, "1000 g = 1 kg")
        assertContains(context, "1000 ml = 1 L")
    }

    @Test
    fun `pantry is context only and second shortage is forbidden`() {
        val context = PromptFactory.selectedRecipeBillContext(bill)
        assertContains(context, "Pantry availability is context only")
        assertContains(context, "Do not introduce a second shortage by changing quantities or adding ingredients")
        assertContains(context, "Cooking instructions must reference only ingredients present in this bill")
    }

    @Test
    fun `empty bill renders no context`() {
        assertEquals("", PromptFactory.selectedRecipeBillContext(emptyList()))
        assertTrue(PromptFactory.selectedRecipeBillContext(emptyList()).isEmpty())
    }
}
