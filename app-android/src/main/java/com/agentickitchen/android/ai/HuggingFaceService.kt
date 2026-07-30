package com.agentickitchen.android.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HFRequest(val inputs: String)

@Serializable
data class HFResponse(val generated_text: String)

class HuggingFaceService(private val apiKey: String, private val modelId: String = "mistralai/Mistral-7B-Instruct-v0.3") : LlmProvider {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    override suspend fun generateContent(prompt: String): String? {
        val response: List<HFResponse> = client.post("https://api-inference.huggingface.co/models/$modelId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(HFRequest(inputs = "<s>[INST] $prompt [/INST]"))
        }.body()

        val text = response.firstOrNull()?.generated_text
        // Mistral repeats the prompt, so return only the generated suffix when present.
        return text?.substringAfter("[/INST]")?.trim() ?: text
    }

    fun close() = client.close()
}
