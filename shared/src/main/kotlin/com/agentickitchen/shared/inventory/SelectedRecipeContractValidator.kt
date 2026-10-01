package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.inventory.InventoryUnits.normalize
import kotlin.math.abs

/** Why a Cooking Plan drifted from the selected Recipe Option's authoritative ingredient bill. */
enum class SelectedRecipeContractReason {
    MISSING_EXPECTED_INGREDIENT,
    UNEXPECTED_INGREDIENT,
    INCOMPATIBLE_UNIT,
    QUANTITY_DRIFT,
    AMBIGUOUS_MATCH
}

data class SelectedRecipeContractResult(
    val valid: Boolean,
    val reasons: List<SelectedRecipeContractReason>
)

/**
 * Deterministic application-side contract between the selected Recipe Option's ingredient bill
 * (already scaled to the requested servings) and the returned CookingPlanResponse.ingredients.
 *
 * The Cooking Plan must not drift from the selected candidate: every expected ingredient needs
 * exactly one unambiguous plan match with an equivalent amount, and the plan may not introduce
 * anything the candidate did not contain. Identity matching goes through
 * [LocalIngredientResolver] (canonical ids when available, normalized names otherwise) and
 * amounts go through [InventoryUnits.normalize]; equivalent unit conversions (1000 g <-> 1 kg,
 * 1000 ml <-> 1 L) pass, incompatible unit dimensions and quantity drift fail. Model-provided
 * missingIngredients is never consulted.
 */
object SelectedRecipeContractValidator {

    /** Absolute/relative tolerance: amounts must match within 0.5 base units or 2%, whichever is larger. */
    private fun tolerance(expectedQuantity: Double): Double = maxOf(0.5, expectedQuantity * 0.02)

    private fun identityMatches(expected: PlannedIngredientDto, plan: PlannedIngredientDto): Boolean =
        LocalIngredientResolver.matches(
            firstName = expected.name,
            firstCanonicalId = expected.canonicalIngredientId,
            secondName = plan.name,
            secondCanonicalId = plan.canonicalIngredientId
        )

    fun validate(
        expectedBill: List<PlannedIngredientDto>,
        planIngredients: List<PlannedIngredientDto>
    ): SelectedRecipeContractResult {
        if (expectedBill.isEmpty()) {
            return SelectedRecipeContractResult(valid = true, reasons = emptyList())
        }

        val reasons = linkedSetOf<SelectedRecipeContractReason>()
        val planIdentityMatched = BooleanArray(planIngredients.size)
        val planClaims = IntArray(planIngredients.size)

        for (expected in expectedBill) {
            val matches = planIngredients.withIndex()
                .filter { identityMatches(expected, it.value) }
                .map { it.index }
            when {
                matches.isEmpty() -> reasons += SelectedRecipeContractReason.MISSING_EXPECTED_INGREDIENT
                matches.size > 1 -> reasons += SelectedRecipeContractReason.AMBIGUOUS_MATCH
                else -> {
                    val planIndex = matches.single()
                    planIdentityMatched[planIndex] = true
                    if (planClaims[planIndex] > 0) {
                        reasons += SelectedRecipeContractReason.AMBIGUOUS_MATCH
                    }
                    planClaims[planIndex]++
                    val expectedAmount = runCatching { normalize(expected.quantity, expected.unit) }.getOrNull()
                    val planAmount = runCatching { normalize(planIngredients[planIndex].quantity, planIngredients[planIndex].unit) }.getOrNull()
                    when {
                        expectedAmount == null || planAmount == null ||
                            expectedAmount.dimension == UnitDimension.UNKNOWN ||
                            planAmount.dimension == UnitDimension.UNKNOWN ->
                            reasons += SelectedRecipeContractReason.INCOMPATIBLE_UNIT
                        expectedAmount.dimension != planAmount.dimension ->
                            reasons += SelectedRecipeContractReason.INCOMPATIBLE_UNIT
                        abs(planAmount.quantity - expectedAmount.quantity) > tolerance(expectedAmount.quantity) ->
                            reasons += SelectedRecipeContractReason.QUANTITY_DRIFT
                    }
                }
            }
        }

        for ((index, planIngredient) in planIngredients.withIndex()) {
            val matchesAnyExpected = expectedBill.any { identityMatches(it, planIngredient) }
            if (!planIdentityMatched[index] && !matchesAnyExpected) {
                reasons += SelectedRecipeContractReason.UNEXPECTED_INGREDIENT
            }
        }

        return SelectedRecipeContractResult(
            valid = reasons.isEmpty(),
            reasons = reasons.toList()
        )
    }
}
