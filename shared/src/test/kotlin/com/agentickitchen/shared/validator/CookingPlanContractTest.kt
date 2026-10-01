package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Contract tests for the Cooking Plan generation contract that the hardened prompt teaches:
 * chained exclusive heat resources, canonical step types, bounded durations, and an acyclic
 * dependency graph must pass, while the corresponding violations must keep failing closed.
 */
class CookingPlanContractTest {

    private val validator = CookingPlanValidator(
        availableEquipment = setOf("stove", "oven", "knife", "bowl", "pan", "baking_tray"),
        stoveMaxLevel = 9,
        ovenAvailable = true,
        airfryerAvailable = false,
        dietType = "none",
        allergens = emptySet(),
        servings = 2
    )

    private fun ingredients() = listOf(
        PlannedIngredientDto("bread", 200.0, "g"),
        PlannedIngredientDto("tomato", 1.0, "piece"),
        PlannedIngredientDto("egg", 2.0, "piece")
    )

    @Test
    fun `multi oven plan with directly chained oven uses passes`() {
        val plan = CookingPlanResponse(
            recipeName = "Oven Tomato Egg Bread",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "prep", "Slice the bread", "knife", 120),
                CookingStepDto("step_2", "cook", "Toast the bread slices in the oven", "oven", 600, targetTemperatureC = 200, dependsOn = listOf("step_1")),
                CookingStepDto("step_3", "combine", "Top the toasted slices with tomato and egg", "counter", 180, dependsOn = listOf("step_2")),
                CookingStepDto("step_4", "cook", "Bake the topped slices in the oven", "oven", 900, targetTemperatureC = 200, dependsOn = listOf("step_2", "step_3")),
                CookingStepDto("step_5", "serve", "Plate the dish", "counter", 60, dependsOn = listOf("step_4"))
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(
            result.valid,
            "expected valid but got: ${result.errors.map { it.type.name }}"
        )
    }

    @Test
    fun `two independent oven steps still produce PARALLEL_RESOURCE_CONFLICT`() {
        val plan = CookingPlanResponse(
            recipeName = "Independent Oven Steps",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "prep", "Prepare the toppings", "knife", 120),
                CookingStepDto("step_2", "cook", "Toast the bread in the oven", "oven", 600, targetTemperatureC = 200, dependsOn = listOf("step_1")),
                CookingStepDto("step_3", "cook", "Roast the tomatoes in the oven", "oven", 600, targetTemperatureC = 200, dependsOn = listOf("step_1"))
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(result.errors.any { it.type == ErrorType.PARALLEL_RESOURCE_CONFLICT })
    }

    @Test
    fun `invalid step type still fails`() {
        val plan = CookingPlanResponse(
            recipeName = "Bake Type Plan",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "bake", "Bake the bread slices", "oven", 600, targetTemperatureC = 200)
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(result.errors.any { it.type == ErrorType.INVALID_STEP_TYPE })
    }

    @Test
    fun `dependency cycle still fails`() {
        val plan = CookingPlanResponse(
            recipeName = "Cyclic Plan",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "prep", "Slice the bread", "knife", 120, dependsOn = listOf("step_2")),
                CookingStepDto("step_2", "prep", "Slice the tomato", "knife", 120, dependsOn = listOf("step_1"))
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(result.errors.any { it.type == ErrorType.DEPENDENCY_CYCLE })
    }

    @Test
    fun `missing dependency still fails`() {
        val plan = CookingPlanResponse(
            recipeName = "Missing Dependency Plan",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "prep", "Slice the bread", "knife", 120, dependsOn = listOf("ghost_step"))
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(result.errors.any { it.type == ErrorType.MISSING_DEPENDENCY })
    }

    @Test
    fun `excessive duration still fails`() {
        val plan = CookingPlanResponse(
            recipeName = "Excessive Duration Plan",
            servings = 2,
            ingredients = ingredients(),
            steps = listOf(
                CookingStepDto("step_1", "cook", "Dry the bread in the oven", "oven", 8000, targetTemperatureC = 120)
            ),
            safetyNotes = emptyList()
        )

        val result = validator.validate(plan)
        assertTrue(result.errors.any { it.type == ErrorType.EXCESSIVE_DURATION })
    }
}
