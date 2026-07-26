package com.agentickitchen.android.ai

interface LlmProvider {
    suspend fun generateContent(prompt: String): String?
}
