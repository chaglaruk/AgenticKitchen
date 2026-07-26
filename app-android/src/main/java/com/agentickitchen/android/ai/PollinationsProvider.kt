package com.agentickitchen.android.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class PollinationsProvider : LlmProvider {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    override suspend fun generateContent(prompt: String): String? {
        return try {
            // Pollinations.ai returns raw text if not requested as JSON
            val response = client.get("https://text.pollinations.ai/$prompt") {
                parameter("model", "openai") // or "mistral", "llama"
            }
            response.bodyAsText()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun io.ktor.client.statement.HttpResponse.bodyAsText(): String {
        return this.call.response.body<String>()
    }

    fun close() = client.close()
}
