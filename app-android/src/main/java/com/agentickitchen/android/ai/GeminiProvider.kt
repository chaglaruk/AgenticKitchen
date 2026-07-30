package com.agentickitchen.android.ai

import com.agentickitchen.android.AppLogger
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import com.agentickitchen.shared.ai.prompt.PromptFactory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
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

internal data class GeminiDiagnostic(
    val feature: String,
    val statusCode: Int?,
    val category: String,
    val responseLength: Int,
    val elapsedMillis: Long
) {
    fun asLogMessage(): String =
        "provider=GEMINI feature=$feature status=${statusCode ?: "none"} category=$category responseLength=$responseLength elapsedMs=$elapsedMillis"
}

class GeminiProvider internal constructor(
    private val apiKey: String,
    private val client: HttpClient,
    private val ownsClient: Boolean,
    private val diagnosticSink: (GeminiDiagnostic) -> Unit
) : KitchenAiProvider, Closeable {

    constructor(apiKey: String) : this(
        apiKey = apiKey,
        client = defaultClient(),
        ownsClient = true,
        diagnosticSink = { AppLogger.i("Gemini", it.asLogMessage()) }
    )

    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> =
        structured(
            feature = "recipe_options",
            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ),
            schema = recipeOptionsSchema,
            decode = json::decodeFromString,
            validate = { response ->
                response.options.size == 3 &&
                    response.options.map { it.id }.toSet().size == 3 &&
                    response.options.all {
                        it.id.isNotBlank() && it.name.isNotBlank() &&
                            it.summary.isNotBlank() && it.estimatedMinutes > 0
                    }
            }
        )

    override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> =
        structured(
            feature = "cooking_plan",
            prompt = PromptFactory.cookingPlanPrompt(
                request.recipeName,
                request.ingredients,
                request.equipment,
                request.servings,
                request.stoveType,
                request.stoveMaxLevel,
                request.ovenAvailable,
                request.ovenHasFan,
                request.airfryerAvailable,
                request.dietType,
                request.allergies,
                request.language
            ),
            schema = cookingPlanSchema,
            decode = json::decodeFromString,
            validate = { plan ->
                plan.recipeName.isNotBlank() && plan.servings > 0 &&
                    plan.ingredients.isNotEmpty() && plan.ingredients.all {
                        it.name.isNotBlank() && it.quantity.isFinite() && it.quantity > 0 && it.unit.isNotBlank()
                    } &&
                    plan.steps.isNotEmpty() && plan.steps.all {
                        it.id.isNotBlank() && it.instruction.isNotBlank() &&
                            it.resource.isNotBlank() && it.durationSeconds > 0
                    }
            }
        )

    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =
        structured(
            feature = "shopping_text",
            prompt = shoppingPrompt(request.text, request.language),
            schema = shoppingSchema,
            decode = json::decodeFromString,
            validate = ::validShoppingResponse
        )

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
        structured(
            feature = "shopping_photo",
            prompt = shoppingPhotoPrompt(request.language),
            schema = shoppingSchema,
            image = request.image,
            decode = json::decodeFromString,
            validate = ::validShoppingResponse
        )

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
        structured(
            feature = "cooking_photo",
            prompt = cookingContext(
                request.recipeName,
                request.plan.toString(),
                request.currentStep,
                request.elapsedSeconds,
                request.resource,
                request.recentTurns.joinToString("\n") { "${it.role}: ${it.text}" },
                request.question,
                request.language
            ),
            schema = cookingPhotoSchema,
            image = request.image,
            decode = json::decodeFromString,
            validate = {
                it.assessment.isNotBlank() &&
                    it.visibleObservation.isNotBlank() &&
                    it.immediateAction.isNotBlank() &&
                    it.uncertainty.isNotBlank()
            }
        )

    override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> =
        structured(
            feature = "cooking_chat",
            prompt = cookingContext(
                request.recipeName,
                request.plan.toString(),
                request.currentStep,
                request.elapsedSeconds,
                request.resource,
                request.recentTurns.joinToString("\n") { "${it.role}: ${it.text}" },
                request.question,
                request.language
            ),
            schema = cookingChatSchema,
            decode = json::decodeFromString,
            validate = { it.answer.isNotBlank() }
        )

    override suspend fun testConnection(): AiResult<Unit> {
        val result = interaction("connection_test", "Reply with OK.", null, null)
        return when (result) {
            is AiResult.Success -> AiResult.Success(Unit, AiProviderId.GEMINI, MODEL)
            is AiResult.Failure -> result
        }
    }

    private suspend fun <T> structured(
        feature: String,
        prompt: String,
        schema: JsonObject,
        image: KitchenImage? = null,
        decode: (String) -> T,
        validate: (T) -> Boolean
    ): AiResult<T> = when (val result = interaction(feature, prompt, schema, image)) {
        is AiResult.Failure -> result
        is AiResult.Success -> try {
            val value = decode(result.value)
            if (validate(value)) {
                AiResult.Success(value, AiProviderId.GEMINI, MODEL)
            } else {
                failure(AiFailureType.InvalidResponse, false)
            }
        } catch (_: SerializationException) {
            failure(AiFailureType.InvalidResponse, true)
        } catch (_: IllegalArgumentException) {
            failure(AiFailureType.InvalidResponse, true)
        }
    }

    private suspend fun interaction(
        feature: String,
        prompt: String,
        schema: JsonObject?,
        image: KitchenImage?
    ): AiResult<String> {
        if (apiKey.isBlank()) return failure(AiFailureType.MissingCredential, false)
        if (image != null && image.bytes.size > MAX_INLINE_IMAGE_BYTES) {
            return failure(AiFailureType.InvalidResponse, false, "request_too_large")
        }

        val request = interactionRequest(prompt, schema, image)
        if (request.toString().toByteArray().size > MAX_REQUEST_BYTES) {
            return failure(AiFailureType.InvalidResponse, false, "request_too_large")
        }

        val started = System.nanoTime()
        val outcome: Pair<AiResult<String>, GeminiDiagnostic> = try {
            val response = client.post(ENDPOINT) {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            val body = response.bodyAsText()
            if (response.status.value !in 200..299) {
                val type = when (response.status) {
                    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AiFailureType.Unauthorized
                    HttpStatusCode.TooManyRequests -> AiFailureType.RateLimited
                    HttpStatusCode.BadRequest -> AiFailureType.InvalidResponse
                    else -> if (response.status.value >= 500) AiFailureType.ProviderUnavailable else AiFailureType.Unknown
                }
                failure(type, type in retryableFailures) to
                    GeminiDiagnostic(feature, response.status.value, type.name, body.length, 0)
            } else {
                val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                val output = root?.let(::extractOutput)
                if (output.isNullOrBlank()) {
                    val safetyBlocked = root?.get("status")?.jsonPrimitive?.contentOrNull == "failed" &&
                        body.contains("safety", ignoreCase = true)
                    val type = if (safetyBlocked) AiFailureType.SafetyBlocked else AiFailureType.InvalidResponse
                    failure(type, type != AiFailureType.SafetyBlocked) to
                        GeminiDiagnostic(feature, response.status.value, type.name, body.length, 0)
                } else {
                    AiResult.Success(output, AiProviderId.GEMINI, MODEL) to
                        GeminiDiagnostic(feature, response.status.value, "SUCCESS", body.length, 0)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: HttpRequestTimeoutException) {
            failure(AiFailureType.Timeout, true) to
                GeminiDiagnostic(feature, null, AiFailureType.Timeout.name, 0, 0)
        } catch (_: IOException) {
            failure(AiFailureType.NetworkUnavailable, true) to
                GeminiDiagnostic(feature, null, AiFailureType.NetworkUnavailable.name, 0, 0)
        } catch (_: Exception) {
            failure(AiFailureType.Unknown, true) to
                GeminiDiagnostic(feature, null, AiFailureType.Unknown.name, 0, 0)
        }
        val elapsed = (System.nanoTime() - started) / 1_000_000
        diagnosticSink(outcome.second.copy(elapsedMillis = elapsed))
        return outcome.first
    }

    private fun interactionRequest(prompt: String, schema: JsonObject?, image: KitchenImage?): JsonObject =
        buildJsonObject {
            put("model", MODEL)
            put("store", false)
            if (image == null) {
                put("input", prompt)
            } else {
                put("input", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", prompt)
                    })
                    add(buildJsonObject {
                        put("type", "image")
                        put("data", Base64.getEncoder().encodeToString(image.bytes))
                        put("mime_type", image.mimeType)
                    })
                })
            }
            schema?.let {
                put("response_format", buildJsonObject {
                    put("type", "text")
                    put("mime_type", "application/json")
                    put("schema", it)
                })
            }
        }

    private fun extractOutput(root: JsonObject): String? =
        root["steps"]?.jsonArray
            ?.asSequence()
            ?.mapNotNull { it as? JsonObject }
            ?.filter { it["type"]?.jsonPrimitive?.contentOrNull == "model_output" }
            ?.flatMap { (it["content"] as? JsonArray).orEmpty().asSequence() }
            ?.mapNotNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            ?.takeIf(String::isNotBlank)

    override fun close() {
        if (ownsClient) client.close()
    }

    companion object {
        const val MODEL = "gemini-3.6-flash"
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"
        const val MAX_REQUEST_BYTES = 20 * 1024 * 1024
        const val MAX_INLINE_IMAGE_BYTES = 14 * 1024 * 1024

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

        private fun defaultClient() = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 45_000
                socketTimeoutMillis = 45_000
            }
        }

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

        private fun shoppingPrompt(text: String, language: String) =
            """Parse this shopping text into only visibly or explicitly stated food items.
Language: $language
Text: $text
Never invent a quantity. Use null when it is not stated."""

        private fun shoppingPhotoPrompt(language: String) =
            """Inspect this kitchen or shopping photo.
Language: $language
Return only food items that are visibly supported by the image.
Never invent hidden items or quantities. Mark uncertain values as estimated and explain the uncertainty."""

        private fun cookingContext(
            recipeName: String,
            plan: String,
            currentStep: String,
            elapsedSeconds: Long,
            resource: String?,
            recentTurns: String,
            question: String,
            language: String
        ) = """You are a careful home-cooking assistant.
Language: $language
Recipe: $recipeName
Validated plan: $plan
Current step: $currentStep
Elapsed seconds: $elapsedSeconds
Resource: ${resource.orEmpty()}
Recent conversation:
$recentTurns
Question: $question
For photo requests, describe only visible evidence and state uncertainty."""

        private fun schema(source: String) = json.parseToJsonElement(source).jsonObject

        private val recipeOptionsSchema = schema(
            """{"type":"object","properties":{"options":{"type":"array","minItems":3,"maxItems":3,"items":{"type":"object","properties":{"id":{"type":"string"},"name":{"type":"string"},"summary":{"type":"string"},"difficulty":{"type":"string"},"estimatedMinutes":{"type":"integer"},"requiredEquipment":{"type":"array","items":{"type":"string"}},"missingIngredients":{"type":"array","items":{"type":"string"}}},"required":["id","name","summary","difficulty","estimatedMinutes","requiredEquipment","missingIngredients"]}}},"required":["options"]}"""
        )
        private val cookingPlanSchema = schema(
            """{"type":"object","properties":{"recipeName":{"type":"string"},"servings":{"type":"integer"},"ingredients":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"quantity":{"type":"number"},"unit":{"type":"string"}},"required":["name","quantity","unit"]}},"steps":{"type":"array","items":{"type":"object","properties":{"id":{"type":"string"},"type":{"type":"string"},"instruction":{"type":"string"},"resource":{"type":"string"},"durationSeconds":{"type":"integer"},"targetTemperatureC":{"type":["integer","null"]},"powerLevel":{"type":["integer","null"]},"dependsOn":{"type":"array","items":{"type":"string"}},"visionCheckpointRecommended":{"type":"boolean"}},"required":["id","type","instruction","resource","durationSeconds","dependsOn","visionCheckpointRecommended"]}},"safetyNotes":{"type":"array","items":{"type":"string"}}},"required":["recipeName","servings","ingredients","steps","safetyNotes"]}"""
        )
        private val shoppingSchema = schema(
            """{"type":"object","properties":{"items":{"type":"array","items":{"type":"object","properties":{"canonicalIngredientId":{"type":["string","null"]},"displayName":{"type":"string"},"quantity":{"type":["number","null"]},"unit":{"type":["string","null"]},"unitDimension":{"type":"string"},"packageLabel":{"type":["string","null"]},"confidence":{"type":"number"},"estimated":{"type":"boolean"},"uncertaintyReason":{"type":["string","null"]}},"required":["displayName","quantity","unit","unitDimension","confidence","estimated"]}}},"required":["items"]}"""
        )
        private val cookingPhotoSchema = schema(
            """{"type":"object","properties":{"assessment":{"type":"string"},"visibleObservation":{"type":"string"},"immediateAction":{"type":"string"},"heatAdjustment":{"type":["string","null"]},"recheckAfterSeconds":{"type":["integer","null"]},"safetyWarning":{"type":["string","null"]},"uncertainty":{"type":"string"}},"required":["assessment","visibleObservation","immediateAction","uncertainty"]}"""
        )
        private val cookingChatSchema = schema(
            """{"type":"object","properties":{"answer":{"type":"string"}},"required":["answer"]}"""
        )
    }
}
