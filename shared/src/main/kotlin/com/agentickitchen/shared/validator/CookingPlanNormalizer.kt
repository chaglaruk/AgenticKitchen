package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import java.util.Locale

/** Normalization outcome: unit canonicalization plus exclusive-resource sequencing. */
data class NormalizedCookingPlan(
    val plan: CookingPlanResponse,
    val sequencing: ExclusiveResourceSequencer.Outcome
)

/** Normalizes provider vocabulary at the boundary; UI localization remains separate. */
fun normalizeCookingPlan(plan: CookingPlanResponse): CookingPlanResponse =
    normalizeCookingPlanWithSequencing(plan).plan

/**
 * Full boundary normalization: canonical ingredient units plus deterministic exclusive-resource
 * sequencing. Invalid graphs (duplicate ids, missing dependencies, cycles) are passed through
 * untouched so CookingPlanValidator can reject them fail-closed.
 */
fun normalizeCookingPlanWithSequencing(plan: CookingPlanResponse): NormalizedCookingPlan {
    val unitNormalized = plan.copy(
        ingredients = plan.ingredients.map { ingredient ->
            ingredient.copy(unit = canonicalCookingUnit(ingredient.unit))
        }
    )
    val sequencing = ExclusiveResourceSequencer.sequence(unitNormalized)
    return NormalizedCookingPlan(sequencing.plan, sequencing)
}

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
