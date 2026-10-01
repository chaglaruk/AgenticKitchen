package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.VisionCheckResponse

/**
 * VisionAgent: platform-specific görsel doğrulama implementasyonları için interface.
 */
interface VisionAgent {
    suspend fun analyzeStepImage(stepId: String, imageJpeg: ByteArray): VisionCheckResponse
}
