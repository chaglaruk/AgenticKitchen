package com.agentickitchen.android.ai

import com.google.ai.client.generativeai.GenerativeModel

class GeminiProvider(private val model: GenerativeModel) : LlmProvider {
    override suspend fun generateContent(prompt: String): String? {
        return try {
            model.generateContent(prompt).text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
