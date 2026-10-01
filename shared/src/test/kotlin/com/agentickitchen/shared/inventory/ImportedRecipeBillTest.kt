package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImportedRecipeBillTest {
    @Test
    fun `reviewed edited values become the authoritative bill`() {
        val reviewed = ImportedRecipe(
            name = "Edited soup",
            servings = 3,
            ingredients = listOf(
                ImportedRecipeIngredient(
                    displayName = "Edited milk",
                    quantity = 175.0,
                    unit = "ml",
                    canonicalIngredientId = "milk",
                    rawText = "100 ml milk"
                ),
                ImportedRecipeIngredient("Egg", 2.0, "adet", "egg")
            ),
            instructions = listOf("Mix and cook.")
        )

        val bill = ImportedRecipeBill.fromReviewed(reviewed)

        assertEquals(listOf("Edited milk", "Egg"), bill.map { it.name })
        assertEquals(175.0, bill.first().quantity)
        assertEquals("ml", bill.first().unit)
        assertEquals("milk", bill.first().canonicalIngredientId)
    }

    @Test
    fun `invalid reviewed amounts are rejected rather than repaired`() {
        val invalid = ImportedRecipe(
            name = "Soup",
            servings = 2,
            ingredients = listOf(ImportedRecipeIngredient("Milk", null, "ml", "milk")),
            instructions = listOf("Cook.")
        )

        assertFailsWith<IllegalArgumentException> { ImportedRecipeBill.fromReviewed(invalid) }
    }
}
