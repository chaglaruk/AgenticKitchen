package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class RecipeSession(
    val sessionId: String,
    val targetTimeIso: String,
    val ingredients: List<IngredientAmount> = emptyList(),
    val hardwareProfileId: String,
    val steps: List<RecipeStep> = emptyList()
)

@Serializable
data class IngredientAmount(val id: String, val massG: Int)

@Serializable
data class RecipeStep(
    val id: String,
    val type: String,
    val resource: String,
    val targetTempC: Int? = null,
    val durationSec: Int? = null,
    val instruction: String? = null,
    val dependsOn: List<String> = emptyList()
)
