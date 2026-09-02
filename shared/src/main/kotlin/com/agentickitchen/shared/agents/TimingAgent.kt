package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.ScheduleRequest
import com.agentickitchen.shared.models.ScheduleResult

/**
 * TimingAgent: tersine zaman mühendisliği algoritmasını sağlayacak arayüz.
 */
interface TimingAgent {
    suspend fun computeSchedule(request: ScheduleRequest): ScheduleResult
}
