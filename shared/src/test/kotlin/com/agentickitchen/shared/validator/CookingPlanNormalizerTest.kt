package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CookingPlanNormalizerTest {
    @Test
    fun `localized and obvious English units normalize before validation`() {
        val units = listOf("adet", "diş", "dilim", "tutam", "çay kaşığı", "yemek kaşığı", "su bardağı", "teaspoons", "tablespoons", "cups")
        val plan = CookingPlanResponse("Test", 2, units.mapIndexed { index, unit -> PlannedIngredientDto("item-$index", 1.0, unit) }, emptyList(), emptyList())

        assertEquals(
            listOf("piece", "clove", "slice", "pinch", "tsp", "tbsp", "cup", "tsp", "tbsp", "cup"),
            normalizeCookingPlan(plan).ingredients.map { it.unit }
        )
    }

    @Test
    fun `unknown unit is preserved for typed validation error`() {
        assertEquals("kepçe", canonicalCookingUnit(" Kepçe "))
    }
}
