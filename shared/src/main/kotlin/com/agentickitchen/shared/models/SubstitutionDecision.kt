package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class SubstitutionDecision(
    val decision: Decision,
    val reason: String? = null,
    val adjustments: Map<String, Double>? = null,
    val confidence: Double = 1.0
)

@Serializable
enum class Decision { REJECT, ALTERNATIVE }
