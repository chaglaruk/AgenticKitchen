package com.agentickitchen.android.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DdgMessage(val role: String, val content: String)

@Serializable
data class DdgRequest(val model: String, val messages: List<DdgMessage>)

class DuckDuckGoProvider : LlmProvider {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private var vqdToken: String? = null

    private suspend fun getVqd(): String? {
        return try {
            val response = client.get("https://duckduckgo.com/aichat/v1/status") {
                header("x-vqd-accept", "1")
            }
            response.headers["x-vqd"]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun generateContent(prompt: String): String? {
        return try {
            if (vqdToken == null) {
                vqdToken = getVqd()
            }
            
            val response = client.post("https://duckduckgo.com/aichat/v1/chat") {
                header("vqd", vqdToken ?: "")
                contentType(ContentType.Application.Json)
                setBody(DdgRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(DdgMessage(role = "user", content = prompt))
                ))
            }

            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val fullText = StringBuilder()
                
                // DDG SSE format: data: {"message":"...","created":...}
                val regex = Regex("""data: (\{.*?\})""")
                val matches = regex.findAll(body)
                
                for (match in matches) {
                    val jsonStr = match.groupValues[1]
                    try {
                        val element = Json.parseToJsonElement(jsonStr).asJsonObject
                        val message = element["message"]?.asJsonPrimitive?.contentOrNull
                        if (message != null) {
                            fullText.append(message)
                        }
                    } catch (e: Exception) {
                        // Manual fallback if JSON parsing fails
                        val start = jsonStr.indexOf("\"message\":\"") + 11
                        val end = jsonStr.indexOf("\"", start)
                        if (start > 10 && end > start) {
                            fullText.append(jsonStr.substring(start, end).replace("\\n", "\n"))
                        }
                    }
                }
                
                response.headers["x-vqd"]?.let { vqdToken = it }
                
                val result = fullText.toString().trim()
                if (result.isNotEmpty()) return result
            }
            
            // Fallback to Pollinations if DDG fails or returns empty
            return PollinationsProvider().generateContent(prompt)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ultimate fallback
            return PollinationsProvider().generateContent(prompt)
        }
    }
    
    // Helper extensions for JsonElement to keep it clean
    private val kotlinx.serialization.json.JsonElement.asJsonObject get() = this as kotlinx.serialization.json.JsonObject
    private val kotlinx.serialization.json.JsonElement.asJsonPrimitive get() = this as kotlinx.serialization.json.JsonPrimitive
    private val kotlinx.serialization.json.JsonPrimitive.contentOrNull get() = this.content

    
    private suspend fun HttpResponse.bodyAsText(): String {
        return this.call.response.body<String>()
    }

    fun close() = client.close()
}
