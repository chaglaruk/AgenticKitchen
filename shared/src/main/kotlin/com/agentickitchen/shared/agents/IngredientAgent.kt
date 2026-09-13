package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.Ingredient
import com.agentickitchen.shared.models.SubstitutionDecision

/**
 * IngredientAgent: katı alternatif filtresi için deterministik karar fonksiyonu.
 */
interface IngredientAgent {
    fun evaluateSubstitution(original: Ingredient, candidate: Ingredient): SubstitutionDecision
}
