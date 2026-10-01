package com.agentickitchen.shared.validator

data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        val valid = ValidationResult(true)
    }
}

data class ValidationError(
    val type: ErrorType,
    val field: String,
    val message: String
)

enum class ErrorType {
    UNKNOWN_RESOURCE,
    UNAVAILABLE_EQUIPMENT,
    POWER_EXCEEDS_MAXIMUM,
    DUPLICATE_STEP_ID,
    MISSING_DEPENDENCY,
    DEPENDENCY_CYCLE,
    NEGATIVE_DURATION,
    EXCESSIVE_DURATION,
    ZERO_DURATION,
    DIET_CONFLICT,
    ALLERGEN_CONFLICT,
    MISSING_QUANTITY,
    UNKNOWN_UNIT,
    SERVING_MISMATCH,
    PARALLEL_RESOURCE_CONFLICT,
    OVER_TEMPERATURE,
    INVALID_STEP_TYPE,
    MISSING_INGREDIENT
}
