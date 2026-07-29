package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CookingPlanValidatorTest {

    private lateinit var validator: CookingPlanValidator

    @BeforeEach
    fun setUp() {
        validator = CookingPlanValidator(
            availableEquipment = setOf("stove", "oven", "knife", "bowl", "pan"),
            stoveMaxLevel = 9,
            ovenAvailable = true,
            airfryerAvailable = false,
            dietType = "none",
            allergens = emptySet(),
            servings = 2
        )
    }

    private fun validPlan() = CookingPlanResponse(
        recipeName = "Test Recipe",
        servings = 2,
        ingredients = listOf(
            PlannedIngredientDto("chicken", 300.0, "g"),
            PlannedIngredientDto("salt", 5.0, "g"),
            PlannedIngredientDto("oil", 30.0, "ml")
        ),
        steps = listOf(
            CookingStepDto("step_1", "prep", "Cut chicken", "knife", 120),
            CookingStepDto("step_2", "cook", "Cook on stove", "stove", 600, powerLevel = 7, dependsOn = listOf("step_1")),
            CookingStepDto("step_3", "rest", "Let rest", "counter", 300, dependsOn = listOf("step_2"))
        ),
        safetyNotes = listOf("Ensure chicken is cooked through")
    )

    @Test
    fun `valid plan passes all checks`() {
        val result = validator.validate(validPlan())
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `unavailable oven rejected`() {
        val noOvenValidator = CookingPlanValidator(
            availableEquipment = setOf("stove", "knife"),
            stoveMaxLevel = 9,
            ovenAvailable = false,
            airfryerAvailable = false,
            dietType = "none",
            allergens = emptySet(),
            servings = 2
        )
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "Bake", "oven", 1200)
            )
        )
        val result = noOvenValidator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.UNAVAILABLE_EQUIPMENT })
    }

    @Test
    fun `power exceeds maximum rejected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "Cook", "stove", 300, powerLevel = 10)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.POWER_EXCEEDS_MAXIMUM })
    }

    @Test
    fun `numeric power is rejected for gas stove heating`() {
        val gasValidator = CookingPlanValidator(
            availableEquipment = setOf("gas", "pan"), stoveMaxLevel = 9, stoveType = "gas",
            ovenAvailable = false, airfryerAvailable = false, dietType = "none", allergens = emptySet(), servings = 2
        )

        val result = gasValidator.validate(validPlan())

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.POWER_EXCEEDS_MAXIMUM })
    }

    @Test
    fun `gas rejects numeric power for every stove-heating resource`() {
        val gasValidator = CookingPlanValidator(
            availableEquipment = setOf("gas", "pan", "pot"), stoveMaxLevel = 9, stoveType = "gas",
            ovenAvailable = false, airfryerAvailable = false, dietType = "none", allergens = emptySet(), servings = 2
        )

        listOf("stove", "pan", "pot").forEach { resource ->
            val result = gasValidator.validate(validPlan().copy(steps = listOf(CookingStepDto("step_$resource", "cook", "Heat", resource, 60, powerLevel = 3))))

            assertFalse(result.valid, "$resource must reject numeric gas power")
            assertTrue(result.errors.any { it.type == ErrorType.POWER_EXCEEDS_MAXIMUM })
        }
    }

    @Test
    fun `no stove rejects stove heating without duplicate numeric power error`() {
        val noStoveValidator = CookingPlanValidator(
            availableEquipment = setOf("knife", "bowl"), stoveMaxLevel = 9, stoveType = "none",
            ovenAvailable = false, airfryerAvailable = false, dietType = "none", allergens = emptySet(), servings = 2
        )
        val plan = validPlan().copy(steps = listOf(CookingStepDto("step_1", "cook", "Heat", "stove", 60, powerLevel = 3)))

        val result = noStoveValidator.validate(plan)

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.UNAVAILABLE_EQUIPMENT })
        assertFalse(result.errors.any { it.type == ErrorType.POWER_EXCEEDS_MAXIMUM })
    }

    @Test
    fun `no stove rejects pan and pot heat even when pan is owned`() {
        val noStoveValidator = CookingPlanValidator(
            availableEquipment = setOf("pan", "pot", "knife"), stoveMaxLevel = 9, stoveType = "none",
            ovenAvailable = false, airfryerAvailable = false, dietType = "none", allergens = emptySet(), servings = 2
        )

        listOf("pan", "pot").forEach { resource ->
            val result = noStoveValidator.validate(validPlan().copy(steps = listOf(CookingStepDto("step_$resource", "cook", "Heat", resource, 60, powerLevel = 3))))

            assertFalse(result.valid, "$resource must require a stove heat source")
            assertEquals(1, result.errors.count { it.type == ErrorType.UNAVAILABLE_EQUIPMENT })
            assertFalse(result.errors.any { it.type == ErrorType.POWER_EXCEEDS_MAXIMUM })
        }
    }

    @Test
    fun `electric accepts inclusive power limits and rejects zero or above maximum`() {
        listOf(1, 9).forEach { power ->
            assertTrue(validator.validate(validPlan().copy(steps = listOf(CookingStepDto("step_$power", "cook", "Heat", "stove", 60, powerLevel = power)))).valid)
        }
        listOf(0, 10).forEach { power ->
            assertFalse(validator.validate(validPlan().copy(steps = listOf(CookingStepDto("step_$power", "cook", "Heat", "stove", 60, powerLevel = power)))).valid)
        }
    }

    @Test
    fun `dependency cycle detected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 100, dependsOn = listOf("step_3")),
                CookingStepDto("step_2", "cook", "B", "stove", 100, dependsOn = listOf("step_1")),
                CookingStepDto("step_3", "cook", "C", "stove", 100, dependsOn = listOf("step_2"))
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.DEPENDENCY_CYCLE })
    }

    @Test
    fun `missing dependency detected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 100, dependsOn = listOf("nonexistent"))
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_DEPENDENCY })
    }

    @Test
    fun `negative duration rejected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", -5)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.NEGATIVE_DURATION })
    }

    @Test
    fun `allergen conflict detected`() {
        val allergenValidator = CookingPlanValidator(
            availableEquipment = setOf("stove"),
            stoveMaxLevel = 9,
            ovenAvailable = false,
            airfryerAvailable = false,
            dietType = "none",
            allergens = setOf("yumurta"),
            servings = 2
        )
        val plan = validPlan()
        val result = allergenValidator.validate(plan)
        assertTrue(result.valid)
    }

    @Test
    fun `allergen conflict with known allergen mapping`() {
        val allergenValidator = CookingPlanValidator(
            availableEquipment = setOf("stove"),
            stoveMaxLevel = 9,
            ovenAvailable = false,
            airfryerAvailable = false,
            dietType = "none",
            allergens = setOf("milk"),
            servings = 2
        )
        val plan = validPlan().copy(
            ingredients = listOf(PlannedIngredientDto("milk", 200.0, "ml"))
        )
        val result = allergenValidator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.ALLERGEN_CONFLICT })
    }

    @Test
    fun `same-resource serialization required`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 100),
                CookingStepDto("step_2", "cook", "B", "stove", 100)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.PARALLEL_RESOURCE_CONFLICT })
    }

    @Test
    fun `valid parallel resources allowed`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "prep", "A", "knife", 100),
                CookingStepDto("step_2", "cook", "B", "stove", 200, dependsOn = listOf("step_1"))
            )
        )
        val result = validator.validate(plan)
        assertTrue(result.valid)
    }

    @Test
    fun `duplicate step ID rejected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 100),
                CookingStepDto("step_1", "prep", "B", "knife", 50)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.DUPLICATE_STEP_ID })
    }

    @Test
    fun `serving mismatch detected`() {
        val plan = validPlan().copy(servings = 4)
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.SERVING_MISMATCH })
    }

    @Test
    fun `unknown resource rejected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "unknown_device", 100)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.UNKNOWN_RESOURCE })
    }

    @Test
    fun `excessive duration rejected`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 99999)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.EXCESSIVE_DURATION })
    }

    @Test
    fun `missing ingredient quantity`() {
        val plan = validPlan().copy(
            ingredients = listOf(PlannedIngredientDto("salt", 0.0, "g"))
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_QUANTITY })
    }

    @Test
    fun `unknown unit normalized`() {
        val plan = validPlan().copy(
            ingredients = listOf(PlannedIngredientDto("chicken", 300.0, "xyz"))
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.UNKNOWN_UNIT })
    }

    @Test
    fun `zero duration`() {
        val plan = validPlan().copy(
            steps = listOf(
                CookingStepDto("step_1", "cook", "A", "stove", 0)
            )
        )
        val result = validator.validate(plan)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.type == ErrorType.ZERO_DURATION })
    }
}
