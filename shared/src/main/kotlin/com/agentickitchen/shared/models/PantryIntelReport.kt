package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class PantryCategorySummary(
    val id: String,
    val label: String,
    val count: Int
)

@Serializable
data class PantryIntelSignal(
    val code: String,
    val message: String
)

@Serializable
data class PantryIntelReport(
    val readinessScore: Int,
    val focusCategoryId: String,
    val focusCategoryLabel: String,
    val categoryBreakdown: List<PantryCategorySummary>,
    val warnings: List<PantryIntelSignal>,
    val tactics: List<PantryIntelSignal>,
    val equipmentLane: String
)
