package com.agentickitchen.android

import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto

internal fun buildImportedCookingPlanRequest(
    recipe: ImportedRecipe,
    authoritativeBill: List<PlannedIngredientDto>,
    equipment: Set<String>,
    stoveType: String,
    stoveMaxLevel: Int,
    ovenAvailable: Boolean,
    ovenHasFan: Boolean,
    airfryerAvailable: Boolean,
    dietType: String,
    allergies: Set<String>,
    language: String,
    inventoryLines: List<String>
): CookingPlanRequest = CookingPlanRequest(
    recipeName = recipe.name,
    ingredients = authoritativeBill.map(PlannedIngredientDto::name),
    equipment = equipment,
    servings = requireNotNull(recipe.servings),
    stoveType = stoveType,
    stoveMaxLevel = stoveMaxLevel,
    ovenAvailable = ovenAvailable,
    ovenHasFan = ovenHasFan,
    airfryerAvailable = airfryerAvailable,
    dietType = dietType,
    allergies = allergies,
    language = language,
    inventoryLines = inventoryLines,
    selectedRecipeIngredients = authoritativeBill,
    sourceRecipeIngredientLines = recipe.ingredients.map { ingredient ->
        ingredient.rawText ?: "${ingredient.quantity} ${ingredient.unit} ${ingredient.displayName}"
    },
    sourceRecipeInstructions = recipe.instructions
)
