package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Servings scaling of the selected recipe bill: quantities scale, identity/unit/canonical stay. */
class RecipeBillScalingTest {

    private val optionBill = listOf(
        PlannedIngredientDto("ekmek", 200.0, "g", "bread"),
        PlannedIngredientDto("yumurta", 2.0, "adet", "egg"),
        PlannedIngredientDto("tereyagi", 20.0, "g", "butter")
    )

    @Test
    fun `same servings leaves quantities unchanged`() {
        val scaled = SelectedRecipeBill.scaled(optionBill, selectionServings = 2, optionServings = 2)
        assertEquals(optionBill, scaled)
    }

    @Test
    fun `two to four servings doubles quantities`() {
        val scaled = SelectedRecipeBill.scaled(optionBill, selectionServings = 4, optionServings = 2)
        assertEquals(listOf(400.0, 4.0, 40.0), scaled.map { it.quantity })
    }

    @Test
    fun `four to two servings halves quantities`() {
        val bill = listOf(PlannedIngredientDto("ekmek", 400.0, "g"))
        val scaled = SelectedRecipeBill.scaled(bill, selectionServings = 2, optionServings = 4)
        assertEquals(200.0, scaled.single().quantity)
    }

    @Test
    fun `source bill is never mutated and canonical id and unit are preserved`() {
        val source = listOf(PlannedIngredientDto("tereyagi", 20.0, "g", "butter"))
        SelectedRecipeBill.scaled(source, selectionServings = 4, optionServings = 2)
        assertEquals(20.0, source.single().quantity)
        assertEquals("g", source.single().unit)
        val scaled = SelectedRecipeBill.scaled(source, selectionServings = 4, optionServings = 2).single()
        assertEquals("tereyagi", scaled.name)
        assertEquals("butter", scaled.canonicalIngredientId)
        assertEquals("g", scaled.unit)
    }

    @Test
    fun `invalid option servings is a no-op`() {
        assertEquals(optionBill, SelectedRecipeBill.scaled(optionBill, selectionServings = 4, optionServings = 0))
        assertEquals(optionBill, SelectedRecipeBill.scaled(optionBill, selectionServings = 0, optionServings = 2))
    }

    @Test
    fun `zero and invalid quantities pass through unscaled`() {
        val bill = listOf(
            PlannedIngredientDto("tuz", 0.0, "g"),
            PlannedIngredientDto("bozuk", Double.NaN, "g")
        )
        val scaled = SelectedRecipeBill.scaled(bill, selectionServings = 4, optionServings = 2)
        assertEquals(0.0, scaled[0].quantity)
        assertTrue(scaled[1].quantity.isNaN())
    }

    @Test
    fun `bill names expose plain ingredient names`() {
        assertEquals(listOf("ekmek", "yumurta", "tereyagi"), SelectedRecipeBill.names(optionBill))
    }
}
