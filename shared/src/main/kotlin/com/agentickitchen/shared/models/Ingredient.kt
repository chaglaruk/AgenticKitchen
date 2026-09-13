package com.agentickitchen.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: String,
    val name: String,
    val category: String,
    val rawDensityGPerMl: Double = 1.0,
    val cookedMassLossPct: Double = 0.0,
    val waterContentPct: Double = 0.0,
    val defaultMethods: Map<String, MethodSpec> = emptyMap(),
    val flavorVector: FlavorVector = FlavorVector(),
    val substitutions: List<Substitution> = emptyList(),
    val allergen: Boolean = false
)

@Serializable
data class MethodSpec(
    val timePer100gAtHeatLevel: Int? = null,
    val optimalHeatLevel: Int? = null,
    val timePer100gAtTemp: Int? = null,
    val optimalTempC: Int? = null
)

@Serializable
data class FlavorVector(
    val umami: Double = 0.0,
    val salt: Double = 0.0,
    val sweet: Double = 0.0,
    val bitter: Double = 0.0
)

@Serializable
data class Substitution(
    val id: String,
    val type: String,
    val reason: String? = null
)
