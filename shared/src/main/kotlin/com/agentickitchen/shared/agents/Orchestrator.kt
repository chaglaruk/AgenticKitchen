package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.RecipeSession
import com.agentickitchen.shared.models.ScheduleResult

/**
 * Orchestrator: koordinasyon katmanı. IngredientAgent, HardwareProfileService ve TimingAgent'i çağırır.
 * Shared modülde interface/soyutlama olarak tutulur; platform-specific wiring Android tarafında yapılır.
 */
interface Orchestrator {
    suspend fun startSession(session: RecipeSession): ScheduleResult
}
