package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto

/**
 * Prepares the authoritative selected-recipe ingredient bill that a Recipe Option hands to
 * Cooking Plan generation. The bill — not the pantry name list — is the recipe the model must
 * honor; the pantry stays separately available as stock-comparison context.
 */
object SelectedRecipeBill {

    /**
     * Scales every positive, finite quantity from the option's serving count to the user's
     * selected serving count. Ingredient identities, canonical ids, and units are preserved.
     * Zero/invalid quantities are passed through unchanged: the provider contract already
     * rejects them, so silently "fixing" them here would hide an upstream defect.
     */
    fun scaled(
        optionBill: List<PlannedIngredientDto>,
        selectionServings: Int,
        optionServings: Int
    ): List<PlannedIngredientDto> {
        if (optionServings <= 0 || selectionServings <= 0 || selectionServings == optionServings) {
            return optionBill
        }
        val factor = selectionServings.toDouble() / optionServings.toDouble()
        if (factor.isNaN() || factor.isInfinite()) return optionBill
        return optionBill.map { ingredient ->
            if (ingredient.quantity.isFinite() && ingredient.quantity > 0.0) {
                ingredient.copy(quantity = ingredient.quantity * factor)
            } else {
                ingredient
            }
        }
    }

    /** Plain display/ingredient names of a bill, for the prompt's ingredient line. */
    fun names(bill: List<PlannedIngredientDto>): List<String> = bill.map { it.name }
}
