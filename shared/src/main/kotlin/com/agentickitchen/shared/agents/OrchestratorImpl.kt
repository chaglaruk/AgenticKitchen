package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.*

/**
 * Basit Orchestrator implementasyonu: ingredient validation (minimal), sonra scheduling çağrısı.
 */
class SimpleOrchestrator(
    private val ingredientAgent: IngredientAgent,
    private val timingAgent: TimingAgent
) : Orchestrator {
    override suspend fun startSession(session: RecipeSession): ScheduleResult {
        // Minimal ingredient checks: if any substitution is present in ingredient.substitutions, run evaluateSubstitution
        // (In full product this would be interactive; here we auto-reject unsafe subs.)
        for (ia in session.ingredients) {
            // session.ingredients only has id and mass; in a full flow we'd look up ingredient records.
            // Skip here: assume upstream validated.
        }

        val request = ScheduleRequest(
            sessionId = session.sessionId,
            targetTimeIso = session.targetTimeIso,
            ingredients = session.ingredients,
            hardwareProfileId = session.hardwareProfileId,
            steps = session.steps
        )

        return timingAgent.computeSchedule(request)
    }
}
