package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CookingPlanValidatorCoverageTest {
    private fun validator(
        equipment: Set<String> = setOf("elec", "pan", "knife", "bowl"),
        ovenAvailable: Boolean = false,
        airfryerAvailable: Boolean = false
    ) = CookingPlanValidator(
        availableEquipment = equipment,
        stoveMaxLevel = 9,
        stoveType = "electric",
        ovenAvailable = ovenAvailable,
        airfryerAvailable = airfryerAvailable,
        dietType = "none",
        allergens = emptySet(),
        servings = 2
    )

    private fun plan(
        ingredients: List<PlannedIngredientDto> = listOf(PlannedIngredientDto("Rice", 200.0, "g")),
        steps: List<CookingStepDto> = listOf(
            CookingStepDto("prep", "prep", "Rinse rice", "sink", 60),
            CookingStepDto("cook", "cook", "Cook rice", "pot", 600, powerLevel = 5, dependsOn = listOf("prep"))
        )
    ) = CookingPlanResponse(
        recipeName = "Rice",
        servings = 2,
        ingredients = ingredients,
        steps = steps,
        safetyNotes = emptyList()
    )

    @Test
    fun `blank ingredient name is rejected`() {
        val result = validator().validate(
            plan(ingredients = listOf(PlannedIngredientDto(" ", 100.0, "g")))
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_INGREDIENT && it.field == "ingredients[0].name" })
    }

    @Test
    fun `empty step list is rejected`() {
        val result = validator().validate(plan(steps = emptyList()))

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_INGREDIENT && it.field == "steps" })
    }

    @Test
    fun `blank instruction is rejected`() {
        val result = validator().validate(
            plan(steps = listOf(CookingStepDto("step", "prep", " ", "knife", 30)))
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_INGREDIENT && it.field == "steps[0].instruction" })
    }

    @Test
    fun `invalid step type is rejected`() {
        val result = validator().validate(
            plan(steps = listOf(CookingStepDto("step", "teleport", "Move food", "counter", 30)))
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.INVALID_STEP_TYPE })
    }

    @Test
    fun `airfryer requires declared availability`() {
        val unavailable = validator(airfryerAvailable = false).validate(
            plan(steps = listOf(CookingStepDto("step", "cook", "Air fry", "airfryer", 300)))
        )
        val available = validator(
            equipment = setOf("airfryer"),
            airfryerAvailable = true
        ).validate(
            plan(steps = listOf(CookingStepDto("step", "cook", "Air fry", "airfryer", 300)))
        )

        assertTrue(unavailable.errors.any { it.type == ErrorType.UNAVAILABLE_EQUIPMENT })
        assertTrue(available.valid)
    }

    @Test
    fun `self dependency is reported once as a cycle`() {
        val result = validator().validate(
            plan(steps = listOf(CookingStepDto("step", "prep", "Prepare", "counter", 30, dependsOn = listOf("step"))))
        )

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.DEPENDENCY_CYCLE })
    }

    @Test
    fun `temperature boundary produces deterministic warnings`() {
        val high = validator().validate(
            plan(steps = listOf(CookingStepDto("high", "cook", "Heat", "oven", 30, targetTemperatureC = 301)))
        )
        val low = validator().validate(
            plan(steps = listOf(CookingStepDto("low", "cool", "Freeze", "fridge", 30, targetTemperatureC = -21)))
        )

        assertEquals(1, high.warnings.count { "exceeds 300" in it })
        assertEquals(1, low.warnings.count { "unusually low" in it })
    }

    @Test
    fun `raw meat without cook step is surfaced as warning`() {
        val result = validator().validate(
            plan(
                ingredients = listOf(PlannedIngredientDto("chicken", 200.0, "g")),
                steps = listOf(CookingStepDto("prep", "prep", "Cut chicken", "knife", 30))
            )
        )

        assertTrue(result.valid)
        assertEquals(1, result.warnings.count { "raw meat" in it })
    }
}
