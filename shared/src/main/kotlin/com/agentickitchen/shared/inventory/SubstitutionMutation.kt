package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.SubstitutionPlanResponse
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto

data class SubstitutionMutationCheck(
    val valid: Boolean,
    val errors: List<String>
)

object SubstitutionMutationValidator {
    fun validate(
        before: CookingPlanResponse,
        targetIngredientName: String,
        response: SubstitutionPlanResponse
    ): SubstitutionMutationCheck {
        val errors = mutableListOf<String>()
        val after = response.mutatedPlan
        val target = before.ingredients.singleOrNull { matches(it, targetIngredientName, null) }
        if (target == null) errors += "target_not_unique_or_missing"
        if (!LocalIngredientResolver.matches(
                response.originalIngredientName, null,
                targetIngredientName, target?.canonicalIngredientId
            )) errors += "response_original_mismatch"
        if (response.reason.isBlank()) errors += "reason_blank"
        if (!response.confidence.isFinite() || response.confidence <= 0.0 || response.confidence > 1.0) errors += "confidence_invalid"
        if (response.replacementIngredient.name.isBlank() || response.replacementIngredient.quantity <= 0.0) errors += "replacement_invalid"
        if (target != null && matches(response.replacementIngredient, target.name, target.canonicalIngredientId)) {
            errors += "replacement_same_as_original"
        }
        if (after.recipeName != before.recipeName) errors += "recipe_identity_changed"
        if (after.servings != before.servings) errors += "servings_changed"
        if (after.ingredients.size != before.ingredients.size) errors += "ingredient_count_changed"

        val beforeStepIds = before.steps.map { it.id }
        val afterStepIds = after.steps.map { it.id }
        if (beforeStepIds.size != beforeStepIds.toSet().size || afterStepIds.size != afterStepIds.toSet().size) {
            errors += "duplicate_step_id"
        }
        if (beforeStepIds.toSet() != afterStepIds.toSet()) errors += "step_identity_changed"

        if (target != null) {
            val unchanged = before.ingredients.filterNot { it === target }
            unchanged.forEach { ingredient ->
                if (after.ingredients.none { matches(it, ingredient.name, ingredient.canonicalIngredientId) }) {
                    errors += "unrelated_ingredient_identity_changed"
                }
            }
            val replacementCount = after.ingredients.count {
                matches(
                    it,
                    response.replacementIngredient.name,
                    response.replacementIngredient.canonicalIngredientId
                )
            }
            if (replacementCount != 1) errors += "replacement_not_unique"
            val targetStillPresent = after.ingredients.any { matches(it, target.name, target.canonicalIngredientId) }
            if (targetStillPresent) errors += "original_still_present"
        }

        return SubstitutionMutationCheck(errors.isEmpty(), errors.distinct())
    }

    private fun matches(
        ingredient: PlannedIngredientDto,
        otherName: String,
        otherCanonicalId: String?
    ): Boolean = LocalIngredientResolver.matches(
        firstName = ingredient.name,
        firstCanonicalId = ingredient.canonicalIngredientId,
        secondName = otherName,
        secondCanonicalId = otherCanonicalId
    )
}
