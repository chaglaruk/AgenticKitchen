package com.agentickitchen.shared.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiProviderId(val label: String) {
    GEMINI("Google Gemini"),
    HUGGINGFACE("Hugging Face"),
    FREE("Free (Pollinations.ai)"),
    DUCKDUCKGO("DuckDuckGo AI");

    companion object {
        fun fromString(value: String): AiProviderId =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: FREE
    }
}
