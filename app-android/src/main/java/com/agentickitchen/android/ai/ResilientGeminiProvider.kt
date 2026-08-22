package com.agentickitchen.android.ai

import com.agentickitchen.android.AppLogger
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.Closeable
import java.io.IOException
import java.util.Base64

/**
 * Keeps Interactions as the primary Gemini path while giving image-based pantry scanning one
 * compatibility fallback through the documented generateContent multimodal endpoint.
 *
 * The fallback is deliberately narrow: it is attempted only for InvalidResponse. Quota,
 * authentication, safety, timeout, and network failures are returned unchanged so a failed
 * request cannot silently double network traffic or bypass a provider boundary.
 */
class ResilientGeminiProvider internal constructor(
    private val primary: KitchenAiProvider,
    private val visionFallback: ShoppingPhotoFallback
) : KitchenAiProvider, Closeable {

    constructor(apiKey: String) : this(
        primary = GeminiProvider(apiKey),
        visionFallback = GenerateContentShoppingPhotoFallback(apiKey)
    )

    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest) =
        primary.generateRecipeOptions(request)

    override suspend fun generateCookingPlan(request: CookingPlanRequest) =
        primary.generateCookingPlan(request)

    override suspend fun parseShoppingText(request: ShoppingTextRequest) =
        primary.parseShoppingText(request)

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> {
        val result = primary.scanShoppingPhoto(request)
        return if (result is AiResult.Failure && result.type == AiFailureType.InvalidResponse) {
            visionFallback.scan(request)
        } else {
            result
        }
    }

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest) =
        primary.inspectCookingPhoto(request)

    override suspend fun askCookingAssistant(request: CookingChatRequest) =
        primary.askCookingAssistant(request)

    override suspend fun testConnection() = primary.testConnection()

    override fun close() {
        (primary as? Closeable)?.close()
        visionFallback.close()
    }
}

internal interface ShoppingPhotoFallback : Closeable {
    suspend fun scan(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse>
}

internal data class GenerateContentDiagnostic(
    val statusCode: Int?,
    val category: String,
    val responseLength: Int
) {
    fun asLogMessage(): String =
        "provider=GEMINI feature=shopping_photo_generate_content status=${statusCode ?: "none"} " +
            "category=$category responseLength=$responseLength"
}

internal class GenerateContentShoppingPhotoFallback(
    private val apiKey: String,
    private val client: HttpClient,
    private val ownsClient: Boolean,
    private val diagnosticSink: (GenerateContentDiagnostic) -> Unit
) : ShoppingPhotoFallback {

    constructor(apiKey: String) : this(
        apiKey = apiKey,
        client = defaultClient(),
        ownsClient = true,
        diagnosticSink = { AppLogger.i("GeminiFallback", it.asLogMessage()) }
    )

    override suspend fun scan(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> {
        if (apiKey.isBlank()) return failure(AiFailureType.MissingCredential, false)
        if (request.image.bytes.size > GeminiProvider.MAX_INLINE_IMAGE_BYTES) {
            return failure(AiFailureType.InvalidResponse, false, "request_too_large")
        }

        val requestBody = buildRequest(request)
        if (requestBody.toString().toByteArray().size > GeminiProvider.MAX_REQUEST_BYTES) {
            return failure(AiFailureType.InvalidResponse, false, "request_too_large")
        }

        return try {
            val response = client.post(ENDPOINT) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            val body = response.bodyAsText()
            if (response.status.value !in 200..299) {
                val type = when (response.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AiFailureType.Unauthorized
                    HttpStatusCode.TooManyRequests -> AiFailureType.RateLimited
                    HttpStatusCode.BadRequest -> AiFailureType.InvalidResponse
                    else -> if (response.status.value >= 500) AiFailureType.ProviderUnavailable else AiFailureType.Unknown
                }
                diagnosticSink(GenerateContentDiagnostic(response.status.value, type.name, body.length))
                failure(type, type in retryableFailures)
            } else {
                val text = extractText(body)
                if (text.isNullOrBlank()) {
                    diagnosticSink(GenerateContentDiagnostic(response.status.value, AiFailureType.InvalidResponse.name, body.length))
                    failure(AiFailureType.InvalidResponse, true)
                } else {
                    parseStructured(text, response.status.value, body.length)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: HttpRequestTimeoutException) {
            diagnosticSink(GenerateContentDiagnostic(null, AiFailureType.Timeout.name, 0))
            failure(AiFailureType.Timeout, true)
        } catch (_: IOException) {
            diagnosticSink(GenerateContentDiagnostic(null, AiFailureType.NetworkUnavailable.name, 0))
            failure(AiFailureType.NetworkUnavailable, true)
        } catch (_: Exception) {
            diagnosticSink(GenerateContentDiagnostic(null, AiFailureType.Unknown.name, 0))
            failure(AiFailureType.Unknown, true)
        }
    }

    override fun close() {
        if (ownsClient) client.close()
    }

    private fun buildRequest(request: ShoppingPhotoRequest): JsonObject = buildJsonObject {
        put("contents", buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("inlineData", buildJsonObject {
                            put("mimeType", request.image.mimeType)
                            put("data", Base64.getEncoder().encodeToString(request.image.bytes))
                        })
                    })
                    add(buildJsonObject {
                        put("text", shoppingPhotoPrompt(request.language))
                    })
                })
            })
        })
        put("generationConfig", buildJsonObject {
            put("responseFormat", buildJsonObject {
                put("text", buildJsonObject {
                    put("mimeType", "application/json")
                    put("schema", shoppingSchema)
                })
            })
        })
    }

    private fun parseStructured(text: String, statusCode: Int, responseLength: Int): AiResult<ShoppingImportResponse> =
        try {
            val value = json.decodeFromString<ShoppingImportResponse>(text)
            if (validShoppingResponse(value)) {
                diagnosticSink(GenerateContentDiagnostic(statusCode, "SUCCESS", responseLength))
                AiResult.Success(value, AiProviderId.GEMINI, GeminiProvider.MODEL)
            } else {
                diagnosticSink(GenerateContentDiagnostic(statusCode, AiFailureType.InvalidResponse.name, responseLength))
                failure(AiFailureType.InvalidResponse, false)
            }
        } catch (_: SerializationException) {
            diagnosticSink(GenerateContentDiagnostic(statusCode, AiFailureType.InvalidResponse.name, responseLength))
            failure(AiFailureType.InvalidResponse, true)
        } catch (_: IllegalArgumentException) {
            diagnosticSink(GenerateContentDiagnostic(statusCode, AiFailureType.InvalidResponse.name, responseLength))
            failure(AiFailureType.InvalidResponse, true)
        }

    private fun extractText(body: String): String? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        return root["candidates"]?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.asSequence()
            ?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            ?.takeIf(String::isNotBlank)
    }

    companion object {
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/${GeminiProvider.MODEL}:generateContent"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private val retryableFailures = setOf(
            AiFailureType.RateLimited,
            AiFailureType.QuotaExceeded,
            AiFailureType.NetworkUnavailable,
            AiFailureType.Timeout,
            AiFailureType.ProviderUnavailable,
            AiFailureType.Unknown
        )

        private fun failure(
            type: AiFailureType,
            retryable: Boolean,
            technical: String? = null
        ) = AiResult.Failure(type, retryable, type.userMessageRes, technical)

        private fun validShoppingResponse(response: ShoppingImportResponse): Boolean =
            response.items.isNotEmpty() && response.items.all {
                it.displayName.isNotBlank() &&
                    it.confidence.isFinite() && it.confidence in 0.0..1.0 &&
                    (it.quantity?.let { quantity -> quantity.isFinite() && quantity > 0 } != false)
            }

        private fun shoppingPhotoPrompt(language: String) =
            """Inspect this kitchen or shopping photo.
Language: $language
Return only food items that are visibly supported by the image.
Never invent hidden items or quantities. Mark uncertain values as estimated and explain the uncertainty."""

        private fun schema(source: String) = json.parseToJsonElement(source).jsonObject

        private val shoppingSchema = schema(
            """{"type":"object","properties":{"items":{"type":"array","items":{"type":"object","properties":{"canonicalIngredientId":{"type":["string","null"]},"displayName":{"type":"string"},"quantity":{"type":["number","null"]},"unit":{"type":["string","null"]},"unitDimension":{"type":"string"},"packageLabel":{"type":["string","null"]},"confidence":{"type":"number"},"estimated":{"type":"boolean"},"uncertaintyReason":{"type":["string","null"]}},"required":["displayName","quantity","unit","unitDimension","confidence","estimated"]}}},"required":["items"]}"""
        )

        private fun defaultClient() = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 45_000
                socketTimeoutMillis = 45_000
            }
        }
    }
}
