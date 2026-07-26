package com.agentickitchen.shared.ai.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeOptionsResponse(
    val options: List<RecipeOptionDto>
)

@Serializable
data class RecipeOptionDto(
    val id: String,
    val name: String,
    val summary: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val requiredEquipment: List<String>,
    val missingIngredients: List<String>
)
