package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient

enum class RecipeImportAvailability {
    AVAILABLE,
    PARTIAL,
    MISSING,
    NEEDS_REVIEW
}

data class RecipeImportIngredientMatch(
    val ingredient: ImportedRecipeIngredient,
    val availability: RecipeImportAvailability,
    val matchingPantryItemIds: List<String>,
    val availableQuantity: Double? = null,
    val normalizedUnit: String? = null,
    val canCheckSubstitution: Boolean = false
)

data class RecipeImportPantrySummary(
    val matches: List<RecipeImportIngredientMatch>
) {
    val availableCount: Int get() = matches.count { it.availability == RecipeImportAvailability.AVAILABLE }
    val partialCount: Int get() = matches.count { it.availability == RecipeImportAvailability.PARTIAL }
    val missingCount: Int get() = matches.count { it.availability == RecipeImportAvailability.MISSING }
    val needsReviewCount: Int get() = matches.count { it.availability == RecipeImportAvailability.NEEDS_REVIEW }
    val readyForValidatedPlan: Boolean get() = needsReviewCount == 0
}

object RecipeImportPantryPlanner {
    fun compare(
        recipe: ImportedRecipe,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double> = emptyMap()
    ): RecipeImportPantrySummary = RecipeImportPantrySummary(
        recipe.ingredients.map { ingredient -> compareIngredient(ingredient, inventory, reservedByItem) }
    )

    private fun compareIngredient(
        ingredient: ImportedRecipeIngredient,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double>
    ): RecipeImportIngredientMatch {
        val requestedQuantity = ingredient.quantity
        val requestedUnit = ingredient.unit
        if (requestedQuantity == null || requestedUnit.isNullOrBlank()) {
            return RecipeImportIngredientMatch(
                ingredient = ingredient,
                availability = RecipeImportAvailability.NEEDS_REVIEW,
                matchingPantryItemIds = emptyList(),
                canCheckSubstitution = false
            )
        }
        val requested = runCatching { InventoryUnits.normalize(requestedQuantity, requestedUnit) }.getOrNull()
            ?: return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                emptyList(),
                canCheckSubstitution = false
            )
        if (requested.dimension == UnitDimension.UNKNOWN) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                emptyList(),
                normalizedUnit = requested.unit,
                canCheckSubstitution = false
            )
        }

        val nameMatches = inventory.filter { item ->
            LocalIngredientResolver.matches(
                ingredient.displayName,
                ingredient.canonicalIngredientId,
                item.originalName,
                item.canonicalIngredientId
            )
        }
        if (nameMatches.isEmpty()) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.MISSING,
                emptyList(),
                availableQuantity = 0.0,
                normalizedUnit = requested.unit,
                canCheckSubstitution = true
            )
        }

        val compatible = nameMatches.mapNotNull { item ->
            val amount = runCatching { InventoryUnits.normalize(item.quantity, item.unit) }.getOrNull() ?: return@mapNotNull null
            if (amount.dimension != requested.dimension) return@mapNotNull null
            val available = (amount.quantity - (reservedByItem[item.id] ?: 0.0)).coerceAtLeast(0.0)
            item.id to available
        }
        if (compatible.isEmpty()) {
            return RecipeImportIngredientMatch(
                ingredient,
                RecipeImportAvailability.NEEDS_REVIEW,
                nameMatches.map(PantryStockItem::id),
                normalizedUnit = requested.unit,
                canCheckSubstitution = false
            )
        }

        val totalAvailable = compatible.sumOf { it.second }
        val availability = when {
            totalAvailable >= requested.quantity -> RecipeImportAvailability.AVAILABLE
            totalAvailable > 0.0 -> RecipeImportAvailability.PARTIAL
            else -> RecipeImportAvailability.MISSING
        }
        return RecipeImportIngredientMatch(
            ingredient = ingredient,
            availability = availability,
            matchingPantryItemIds = compatible.map { it.first },
            availableQuantity = totalAvailable,
            normalizedUnit = requested.unit,
            canCheckSubstitution = availability != RecipeImportAvailability.AVAILABLE
        )
    }
}
