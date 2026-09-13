package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleEvent(
    val id: String,
    val startIso: String,
    val endIso: String,
    val instruction: String,
    val resource: String,
    val parallelizable: Boolean = false
)

@Serializable
data class ScheduleRequest(
    val sessionId: String,
    val targetTimeIso: String,
    val ingredients: List<IngredientAmount>,
    val hardwareProfileId: String,
    val steps: List<RecipeStep> = emptyList()
)

@Serializable
data class ScheduleResult(val events: List<ScheduleEvent> = emptyList())
