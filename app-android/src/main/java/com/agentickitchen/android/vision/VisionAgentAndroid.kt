package com.agentickitchen.android.vision

import com.agentickitchen.shared.agents.VisionAgent
import com.agentickitchen.shared.models.VisionCheckResponse

/**
 * Stub VisionAgent for Android. Returns a mock positive verdict with high confidence.
 * Replace with TFLite/ML Kit implementation for production.
 */
class VisionAgentAndroid : VisionAgent {
    override suspend fun analyzeStepImage(stepId: String, imageJpeg: ByteArray): VisionCheckResponse {
        // Simple heuristic stub: always return slice_ok with high confidence and no delay.
        return VisionCheckResponse(
            stepId = stepId,
            verdict = "slice_ok",
            confidence = 0.95,
            recommendedDelaySec = 0
        )
    }
}
