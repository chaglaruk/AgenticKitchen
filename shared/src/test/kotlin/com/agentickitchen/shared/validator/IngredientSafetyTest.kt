package com.agentickitchen.shared.validator

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IngredientSafetyTest {
    @Test
    fun `vegetarian and vegan checks recognize Turkish animal ingredients`() {
        assertTrue(IngredientSafety.conflictsWithDiet("Tavuk göğsü", "vegetarian"))
        listOf("Yumurta", "Süt", "Kaşar peyniri", "Bal", "Somon", "Dana eti").forEach {
            assertTrue(IngredientSafety.conflictsWithDiet(it, "vegan"), "$it should conflict with vegan")
        }
        assertFalse(IngredientSafety.conflictsWithDiet("Kırmızı mercimek", "vegan"))
    }

    @Test
    fun `allergen groups recognize Turkish dairy and shellfish`() {
        assertTrue(IngredientSafety.conflictsWithAllergen("Yoğurt", "milk"))
        assertTrue(IngredientSafety.conflictsWithAllergen("Beyaz peynir", "süt"))
        assertTrue(IngredientSafety.conflictsWithAllergen("Karides", "shellfish"))
        assertFalse(IngredientSafety.conflictsWithAllergen("Pirinç", "milk"))
    }
}
