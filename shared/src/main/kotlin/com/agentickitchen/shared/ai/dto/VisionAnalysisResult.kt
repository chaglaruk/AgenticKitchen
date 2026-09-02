package com.agentickitchen.shared.ai.dto

import com.agentickitchen.shared.ai.AiProviderId

sealed interface VisionAnalysisResult {
    data class Success(
        val items: List<DetectedIngredient>,
        val provider: AiProviderId
    ) : VisionAnalysisResult

    data class LowConfidence(
        val message: String
    ) : VisionAnalysisResult

    data class Failure(
        val message: String
    ) : VisionAnalysisResult
}

data class DetectedIngredient(
    val name: String,
    val confidence: Double? = null
)
