package com.agentickitchen.shared.scheduler

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Duration
import java.time.LocalTime

sealed interface TargetTimeChoice {
    @Serializable
    data class Exact(@Contextual val localTime: LocalTime) : TargetTimeChoice

    @Serializable
    data class After(@Contextual val duration: Duration) : TargetTimeChoice

    @Serializable
    data object ThisEvening : TargetTimeChoice

    @Serializable
    data object Flexible : TargetTimeChoice
}
