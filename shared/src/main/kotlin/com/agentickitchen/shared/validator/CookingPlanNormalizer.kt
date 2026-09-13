package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import java.util.Locale

/** Normalizes provider vocabulary at the boundary; UI localization remains separate. */
fun normalizeCookingPlan(plan: CookingPlanResponse): CookingPlanResponse = plan.copy(
    ingredients = plan.ingredients.map { ingredient ->
        ingredient.copy(unit = canonicalCookingUnit(ingredient.unit))
    }
)

fun canonicalCookingUnit(unit: String): String {
    val normalized = unit.trim().lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
        .removeSuffix(".")
    return when (normalized) {
        "adet", "ad", "piece", "pieces", "pc", "pcs", "each", "count" -> "piece"
        "diş", "dis", "clove", "cloves" -> "clove"
        "dilim", "slice", "slices" -> "slice"
        "tutam", "pinch", "pinches" -> "pinch"
        "çay kaşığı", "cay kasigi", "çk", "tsp", "teaspoon", "teaspoons" -> "tsp"
        "yemek kaşığı", "yemek kasigi", "yk", "tbsp", "tablespoon", "tablespoons" -> "tbsp"
        "su bardağı", "su bardagi", "bardak", "cup", "cups" -> "cup"
        "gram", "grams", "gr", "g" -> "g"
        "kilogram", "kilograms", "kilo", "kg" -> "kg"
        "millilitre", "milliliter", "millilitres", "milliliters", "ml" -> "ml"
        "litre", "liter", "litres", "liters", "l" -> "l"
        "birim", "unit", "units" -> "unit"
        "damak tadına göre", "isteğe göre", "to taste" -> "to taste"
        else -> normalized
    }
}
