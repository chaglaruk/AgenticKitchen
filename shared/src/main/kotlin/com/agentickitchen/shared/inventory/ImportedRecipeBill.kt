package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto

/** Builds the authoritative Cooking Plan ingredient bill from a fully reviewed import draft. */
object ImportedRecipeBill {
    fun fromReviewed(recipe: ImportedRecipe): List<PlannedIngredientDto> {
        val issues = RecipeImportDraftPolicy.issues(recipe)
        require(issues.isEmpty()) { "Imported recipe must be fully reviewed" }
        return recipe.ingredients.map { ingredient ->
            PlannedIngredientDto(
                name = ingredient.displayName,
                quantity = requireNotNull(ingredient.quantity),
                unit = requireNotNull(ingredient.unit),
                canonicalIngredientId = ingredient.canonicalIngredientId
            )
        }
    }
}
