package com.agentickitchen.shared.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class CookingPlanResponse(
    val recipeName: String,
    val servings: Int,
    val ingredients: List<PlannedIngredientDto>,
    val steps: List<CookingStepDto>,
    val safetyNotes: List<String>
)

@Serializable
data class PlannedIngredientDto(
    val name: String,
    val quantity: Double,
    val unit: String
)

@Serializable
data class CookingStepDto(
    val id: String,
    val type: String,
    val instruction: String,
    val resource: String,
    val durationSeconds: Int,
    val targetTemperatureC: Int? = null,
    val powerLevel: Int? = null,
    val dependsOn: List<String> = emptyList(),
    val visionCheckpointRecommended: Boolean = false
)
