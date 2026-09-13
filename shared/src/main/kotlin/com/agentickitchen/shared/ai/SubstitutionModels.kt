package com.agentickitchen.shared.ai

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import kotlinx.serialization.Serializable

data class SubstitutionPlanRequest(
    val plan: CookingPlanResponse,
    val missingIngredientName: String,
    val inventoryLines: List<String>,
    val equipment: Set<String>,
    val stoveType: String,
    val stoveMaxLevel: Int,
    val ovenAvailable: Boolean,
    val ovenHasFan: Boolean,
    val airfryerAvailable: Boolean,
    val dietType: String,
    val allergies: Set<String>,
    val language: String
)

@Serializable
data class SubstitutionPlanResponse(
    val originalIngredientName: String,
    val replacementIngredient: PlannedIngredientDto,
    val reason: String,
    val confidence: Double,
    val mutatedPlan: CookingPlanResponse
)
