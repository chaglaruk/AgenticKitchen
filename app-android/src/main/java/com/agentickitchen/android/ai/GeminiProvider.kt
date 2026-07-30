package com.agentickitchen.android.ai

import com.google.ai.client.generativeai.GenerativeModel

class GeminiProvider(private val model: GenerativeModel) : LlmProvider {
    override suspend fun generateContent(prompt: String): String? = model.generateContent(prompt).text
}
