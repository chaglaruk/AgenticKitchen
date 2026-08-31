package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.dto.CookingPlanResponse

object RecipeImportPlanGuard {
    data class Result(val valid: Boolean, val reasons: List<String>)

    fun validate(recipe: ImportedRecipe, plan: CookingPlanResponse): Result {
        val reasons = mutableListOf<String>()
        if (!plan.recipeName.equals(recipe.name, ignoreCase = true)) reasons += "recipe_name_changed"
        if (recipe.servings == null || recipe.servings <= 0 || plan.servings != recipe.servings) reasons += "servings_changed"
        if (plan.ingredients.size != recipe.ingredients.size) reasons += "ingredient_count_changed"

        val unmatched = plan.ingredients.toMutableList()
        recipe.ingredients.forEach { imported ->
            val sourceQuantity = imported.quantity
            val sourceUnit = imported.unit
            if (sourceQuantity == null || sourceUnit.isNullOrBlank()) {
                reasons += "source_amount_unreviewed"
                return@forEach
            }
            val candidates = unmatched.withIndex().filter { (_, planned) ->
                LocalIngredientResolver.matches(
                    imported.displayName,
                    imported.canonicalIngredientId,
                    planned.name,
                    planned.canonicalIngredientId
                )
            }
            if (candidates.size != 1) {
                reasons += if (candidates.isEmpty()) "ingredient_missing" else "ingredient_ambiguous"
                return@forEach
            }
            val indexed = candidates.single()
            val planned = indexed.value
            unmatched.removeAt(indexed.index)
            val source = runCatching { InventoryUnits.normalize(sourceQuantity, sourceUnit) }.getOrNull()
            val target = runCatching { InventoryUnits.normalize(planned.quantity, planned.unit) }.getOrNull()
            if (source == null || target == null || source.dimension == UnitDimension.UNKNOWN || target.dimension != source.dimension) {
                reasons += "ingredient_unit_changed"
                return@forEach
            }
            val tolerance = maxOf(0.0001, source.quantity * 0.05)
            if (kotlin.math.abs(target.quantity - source.quantity) > tolerance) reasons += "ingredient_amount_changed"
        }
        if (unmatched.isNotEmpty()) reasons += "ingredient_added"
        return Result(reasons.isEmpty(), reasons.distinct())
    }
}
