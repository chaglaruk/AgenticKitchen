package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.*
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Basit, deterministik IngredientAgent implementasyonu.
 * - Alerjen uyuşmazlığı -> REJECT
 * - Flavor vector Euclidean distance > threshold -> REJECT
 * - Cooked mass loss farkı çok büyükse -> REJECT
 * - Aksi halde ALTERNATIVE + basit ayarlama önerisi
 */
class SimpleIngredientAgent : IngredientAgent {
    private val flavorThreshold = 0.45
    private val cookedLossAllowedDiff = 60.0

    override fun evaluateSubstitution(original: Ingredient, candidate: Ingredient): SubstitutionDecision {
        if (original.allergen && !candidate.allergen) {
            // if original is allergen and candidate is not, that's ok; but if candidate is allergen and original not, reject
        }
        if (!original.allergen && candidate.allergen) {
            return SubstitutionDecision(Decision.REJECT, reason = "Candidate introduces allergen")
        }

        val dist = flavorDistance(original.flavorVector, candidate.flavorVector)
        if (dist > flavorThreshold) {
            return SubstitutionDecision(Decision.REJECT, reason = "Flavor distance too large: ${"%.2f".format(dist)}", confidence = 0.0)
        }

        val lossDiff = kotlin.math.abs(original.cookedMassLossPct - candidate.cookedMassLossPct)
        if (lossDiff > cookedLossAllowedDiff) {
            return SubstitutionDecision(Decision.REJECT, reason = "Cooked mass loss incompatible (diff=$lossDiff)")
        }

        // propose salt adjustment as a simple example
        val saltAdj = candidate.flavorVector.salt - original.flavorVector.salt
        val adjustments = if (kotlin.math.abs(saltAdj) > 0.0001) mapOf("salt_frac_change" to -saltAdj) else null

        return SubstitutionDecision(Decision.ALTERNATIVE, reason = "Technically compatible", adjustments = adjustments, confidence = 0.8)
    }

    private fun flavorDistance(a: FlavorVector, b: FlavorVector): Double {
        val sum = (a.umami - b.umami).pow(2) + (a.salt - b.salt).pow(2) + (a.sweet - b.sweet).pow(2) + (a.bitter - b.bitter).pow(2)
        return sqrt(sum)
    }
}
