package com.agentickitchen.shared.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiProviderId(val label: String) {
    FIREBASE("Firebase AI"),
    GEMINI("Google Gemini"),
    HUGGINGFACE("Hugging Face"),
    FREE("Offline"),
    DUCKDUCKGO("DuckDuckGo AI");

    companion object {
        fun fromString(value: String): AiProviderId =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}
