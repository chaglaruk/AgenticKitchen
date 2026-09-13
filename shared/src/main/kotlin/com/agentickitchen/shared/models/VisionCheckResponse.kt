package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class VisionCheckResponse(
    val stepId: String,
    val verdict: String,
    val confidence: Double,
    val recommendedDelaySec: Int = 0
)
