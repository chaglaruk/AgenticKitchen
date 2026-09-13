package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.SubstitutionPlanResponse
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubstitutionMutationValidatorTest {
    private val before = CookingPlanResponse(
        recipeName = "Onion rice",
        servings = 2,
        ingredients = listOf(
            PlannedIngredientDto("Rice", 160.0, "g", "rice"),
            PlannedIngredientDto("Onion", 1.0, "piece", "onion")
        ),
        steps = listOf(
            CookingStepDto("prep", "prep", "Chop onion", "knife", 60),
            CookingStepDto("cook", "cook", "Cook rice and onion", "pot", 900, dependsOn = listOf("prep"))
        ),
        safetyNotes = emptyList()
    )

    @Test fun acceptsOneForOneIdentityMutationWithStepAdjustments() {
        val after = before.copy(
            ingredients = listOf(
                before.ingredients[0],
                PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")
            ),
            steps = listOf(
                before.steps[0].copy(instruction = "Mince garlic", durationSeconds = 45),
                before.steps[1].copy(instruction = "Cook rice and garlic", durationSeconds = 840)
            )
        )
        val response = SubstitutionPlanResponse(
            "Onion",
            PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"),
            "Garlic is available and changes the aromatic profile.",
            .8,
            after
        )
        assertTrue(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }

    @Test fun rejectsUnrelatedIngredientIdentityChange() {
        val after = before.copy(ingredients = listOf(
            PlannedIngredientDto("Pasta", 160.0, "g", "pasta"),
            PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")
        ))
        val response = SubstitutionPlanResponse(
            "Onion", PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"), "reason", .8, after
        )
        assertFalse(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }

    @Test fun rejectsChangedStepIdentityOrRecipeIdentity() {
        val after = before.copy(
            recipeName = "Different recipe",
            ingredients = listOf(before.ingredients[0], PlannedIngredientDto("Garlic", 2.0, "clove", "garlic")),
            steps = listOf(before.steps[0].copy(id = "new_step"), before.steps[1])
        )
        val response = SubstitutionPlanResponse(
            "Onion", PlannedIngredientDto("Garlic", 2.0, "clove", "garlic"), "reason", .8, after
        )
        assertFalse(SubstitutionMutationValidator.validate(before, "Onion", response).valid)
    }
}
