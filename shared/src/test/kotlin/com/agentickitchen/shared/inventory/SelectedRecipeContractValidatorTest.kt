package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deterministic option-to-plan ingredient contract: the Cooking Plan must honor the selected
 * Recipe Option's scaled bill exactly. Equivalent unit normalization passes; drift in either
 * direction fails closed.
 */
class SelectedRecipeContractValidatorTest {

    private fun validate(
        expected: List<PlannedIngredientDto>,
        plan: List<PlannedIngredientDto>
    ) = SelectedRecipeContractValidator.validate(expected, plan)

    @Test
    fun `exact identity and quantity contract passes`() {
        val expected = listOf(
            PlannedIngredientDto("ekmek", 200.0, "g"),
            PlannedIngredientDto("yumurta", 2.0, "adet"),
            PlannedIngredientDto("sut", 100.0, "ml"),
            PlannedIngredientDto("tereyagi", 20.0, "g")
        )
        val plan = expected

        val result = validate(expected, plan)
        assertTrue(result.valid, "reasons: ${result.reasons}")
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun `kg and g equivalent amounts pass`() {
        val expected = listOf(PlannedIngredientDto("ekmek", 200.0, "g"))
        val plan = listOf(PlannedIngredientDto("ekmek", 0.2, "kg"))
        assertTrue(validate(expected, plan).valid)
    }

    @Test
    fun `l and ml equivalent amounts pass`() {
        val expected = listOf(PlannedIngredientDto("sut", 100.0, "ml"))
        val plan = listOf(PlannedIngredientDto("sut", 0.1, "l"))
        assertTrue(validate(expected, plan).valid)
    }

    @Test
    fun `missing expected ingredient fails`() {
        val expected = listOf(
            PlannedIngredientDto("ekmek", 200.0, "g"),
            PlannedIngredientDto("tereyagi", 20.0, "g")
        )
        val plan = listOf(PlannedIngredientDto("ekmek", 200.0, "g"))

        val result = validate(expected, plan)
        assertFalse(result.valid)
        assertEquals(listOf(SelectedRecipeContractReason.MISSING_EXPECTED_INGREDIENT), result.reasons)
    }

    @Test
    fun `unexpected model-introduced ingredient fails`() {
        val expected = listOf(PlannedIngredientDto("ekmek", 200.0, "g"))
        val plan = listOf(
            PlannedIngredientDto("ekmek", 200.0, "g"),
            PlannedIngredientDto("maydanoz", 5.0, "g")
        )

        val result = validate(expected, plan)
        assertFalse(result.valid)
        assertEquals(listOf(SelectedRecipeContractReason.UNEXPECTED_INGREDIENT), result.reasons)
    }

    @Test
    fun `quantity drift fails`() {
        val expected = listOf(PlannedIngredientDto("tereyagi", 20.0, "g"))
        val plan = listOf(PlannedIngredientDto("tereyagi", 60.0, "g"))

        val result = validate(expected, plan)
        assertFalse(result.valid)
        assertEquals(listOf(SelectedRecipeContractReason.QUANTITY_DRIFT), result.reasons)
    }

    @Test
    fun `incompatible unit dimension fails`() {
        val expected = listOf(PlannedIngredientDto("ekmek", 200.0, "g"))
        val plan = listOf(PlannedIngredientDto("ekmek", 4.0, "dilim"))

        val result = validate(expected, plan)
        assertFalse(result.valid)
        assertEquals(listOf(SelectedRecipeContractReason.INCOMPATIBLE_UNIT), result.reasons)
    }

    @Test
    fun `one expected matching two plan ingredients fails closed as ambiguous`() {
        val expected = listOf(PlannedIngredientDto("yumurta", 2.0, "adet"))
        val plan = listOf(
            PlannedIngredientDto("yumurta", 1.0, "adet"),
            PlannedIngredientDto("Yumurta", 1.0, "adet")
        )

        val result = validate(expected, plan)
        assertFalse(result.valid)
        assertEquals(listOf(SelectedRecipeContractReason.AMBIGUOUS_MATCH), result.reasons)
    }

    @Test
    fun `option with one shortage keeps exactly one shortage in an equivalent plan`() {
        // The designed physical scenario: option bill needs tereyagi 20 g, pantry holds 1 g.
        // The contract compares bill vs plan only — the pantry shortage is unchanged by it.
        val expected = listOf(
            PlannedIngredientDto("ekmek", 200.0, "g"),
            PlannedIngredientDto("yumurta", 2.0, "adet"),
            PlannedIngredientDto("sut", 100.0, "ml"),
            PlannedIngredientDto("tereyagi", 20.0, "g")
        )
        val plan = listOf(
            PlannedIngredientDto("ekmek", 200.0, "g"),
            PlannedIngredientDto("yumurta", 2.0, "adet"),
            PlannedIngredientDto("sut", 100.0, "ml"),
            PlannedIngredientDto("tereyagi", 20.0, "g")
        )

        assertTrue(validate(expected, plan).valid)
    }

    @Test
    fun `empty expected bill is trivially valid`() {
        assertTrue(validate(emptyList(), listOf(PlannedIngredientDto("ekmek", 200.0, "g"))).valid)
    }
}
