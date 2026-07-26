package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class HardwareProfile(
    val id: String,
    val type: String,
    val fuel: String,
    val controlRangeMin: Int,
    val controlRangeMax: Int,
    val heatMap: Map<Int, Int> = emptyMap(),
    val preheatTimeSecForTemp: Map<Int, Int> = emptyMap()
)
