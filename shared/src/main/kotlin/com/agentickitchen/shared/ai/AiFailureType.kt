package com.agentickitchen.shared.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiFailureType(val userMessageRes: String) {
    MissingCredential("error_missing_credential"),
    Unauthorized("error_unauthorized"),
    QuotaExceeded("error_quota_exceeded"),
    RateLimited("error_rate_limited"),
    NetworkUnavailable("error_network"),
    Timeout("error_timeout"),
    InvalidResponse("error_invalid_response"),
    SafetyBlocked("error_safety_blocked"),
    ProviderUnavailable("error_provider_unavailable"),
    InvalidPlan("error_invalid_plan"),
    Unknown("error_generic")
}
