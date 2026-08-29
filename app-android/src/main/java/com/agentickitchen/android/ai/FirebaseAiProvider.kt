package com.agentickitchen.android.ai

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PermissionMissingException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiProviderId
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import com.agentickitchen.shared.ai.prompt.PromptFactory
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

internal data class FirebaseGatewayResponse(
    val text: String,
    val modelName: String
)

internal fun interface FirebaseModelGateway {
    suspend fun generate(
        kind: FirebaseResponseKind,
        prompt: String,
        image: KitchenImage?
    ): FirebaseGatewayResponse
}

internal class FirebaseSdkModelGateway(
    firebaseApp: FirebaseApp,
    private val modelConfig: FirebaseAiModelConfig = FirebaseRemoteModelConfig(firebaseApp)
) : FirebaseModelGateway {
    private val ai = Firebase.ai(
        app = firebaseApp,
        backend = GenerativeBackend.googleAI()
    )

    override suspend fun generate(
        kind: FirebaseResponseKind,
        prompt: String,
        image: KitchenImage?
    ): FirebaseGatewayResponse {
        val modelName = modelConfig.modelFor(kind.task)
        val model = ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = kind.schema
            }
        )
        val response = if (image == null) {
            model.generateContent(prompt)
        } else {
            model.generateContent(
                content {
                    inlineData(image.bytes, image.mimeType)
                    text(prompt)
                }
            )
        }
        return FirebaseGatewayResponse(response.text.orEmpty(), modelName)
    }
}

class FirebaseAiProvider internal constructor(
    private val gateway: FirebaseModelGateway
) : KitchenAiProvider {

    constructor(firebaseApp: FirebaseApp) : this(FirebaseSdkModelGateway(firebaseApp))

    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> =
        structured(
            kind = FirebaseResponseKind.RECIPE_OPTIONS,
            prompt = PromptFactory.recipeOptionsPrompt(
                request.ingredients,
                request.equipment,
                request.dietType,
                request.allergies,
                request.language
            ) + inventoryRecipeContext(request),
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
            kind = FirebaseResponseKind.COOKING_PLAN,
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
            ) + inventoryPlanContext(request),
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
            kind = FirebaseResponseKind.SHOPPING_IMPORT,
            prompt = """Parse this shopping text into only explicitly stated food items.
Language: ${request.language}
Text: ${request.text}
Never invent a quantity. Use null when quantity is not stated.
Return only valid JSON for the app's shopping import schema.""",
            decode = json::decodeFromString,
            validate = ::validShoppingResponse
        )

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
        structured(
            kind = FirebaseResponseKind.SHOPPING_IMPORT,
            prompt = """Inspect this shopping or kitchen photo.
Language: ${request.language}
Return only food items visibly supported by the image.
Never invent hidden items or quantities. Mark uncertainty conservatively.
Return only valid JSON for the app's shopping import schema.""",
            image = request.image,
            decode = json::decodeFromString,
            validate = ::validShoppingResponse
        )

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
        structured(
            kind = FirebaseResponseKind.COOKING_PHOTO,
            prompt = cookingContext(
                request.recipeName,
                request.plan.toString(),
                request.currentStep,
                request.elapsedSeconds,
                request.resource,
                request.recentTurns.joinToString("\n") { "${it.role}: ${it.text}" },
                request.question,
                request.language,
                photo = true
            ),
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
            kind = FirebaseResponseKind.COOKING_CHAT,
            prompt = cookingContext(
                request.recipeName,
                request.plan.toString(),
                request.currentStep,
                request.elapsedSeconds,
                request.resource,
                request.recentTurns.joinToString("\n") { "${it.role}: ${it.text}" },
                request.question,
                request.language,
                photo = false
            ),
            decode = json::decodeFromString,
            validate = { it.answer.isNotBlank() }
        )

    override suspend fun testConnection(): AiResult<Unit> = when (
        val result = invoke(
            FirebaseResponseKind.CONNECTION_TEST,
            """Reply only with valid JSON: {"status":"ok"}"""
        )
    ) {
        is AiResult.Success -> AiResult.Success(Unit, AiProviderId.FIREBASE, result.model)
        is AiResult.Failure -> result
    }

    private suspend fun <T> structured(
        kind: FirebaseResponseKind,
        prompt: String,
        image: KitchenImage? = null,
        decode: (String) -> T,
        validate: (T) -> Boolean
    ): AiResult<T> = when (val result = invoke(kind, prompt, image)) {
        is AiResult.Failure -> result
        is AiResult.Success -> try {
            val decoded = decode(result.value)
            if (validate(decoded)) AiResult.Success(decoded, AiProviderId.FIREBASE, result.model)
            else failure(AiFailureType.InvalidResponse, false)
        } catch (_: SerializationException) {
            failure(AiFailureType.InvalidResponse, true)
        } catch (_: IllegalArgumentException) {
            failure(AiFailureType.InvalidResponse, true)
        }
    }

    private suspend fun invoke(
        kind: FirebaseResponseKind,
        prompt: String,
        image: KitchenImage? = null
    ): AiResult<String> {
        if (image != null && image.bytes.size > MAX_INLINE_IMAGE_BYTES) {
            return failure(AiFailureType.InvalidResponse, false, "request_too_large")
        }
        return try {
            val response = gateway.generate(kind, prompt, image)
            response.text.takeIf(String::isNotBlank)?.let {
                AiResult.Success(it, AiProviderId.FIREBASE, response.modelName)
            } ?: failure(AiFailureType.InvalidResponse, true)
        } catch (error: CancellationException) {
            throw error
        } catch (_: QuotaExceededException) {
            failure(AiFailureType.QuotaExceeded, true)
        } catch (_: RequestTimeoutException) {
            failure(AiFailureType.Timeout, true)
        } catch (_: InvalidAPIKeyException) {
            failure(AiFailureType.Unauthorized, false)
        } catch (_: PermissionMissingException) {
            failure(AiFailureType.ProviderUnavailable, false, "firebase_app_check_or_permission")
        } catch (_: APINotConfiguredException) {
            failure(AiFailureType.ProviderUnavailable, false, "firebase_ai_not_configured")
        } catch (_: ServiceDisabledException) {
            failure(AiFailureType.ProviderUnavailable, false, "firebase_ai_disabled")
        } catch (_: PromptBlockedException) {
            failure(AiFailureType.SafetyBlocked, false)
        } catch (_: ContentBlockedException) {
            failure(AiFailureType.SafetyBlocked, false)
        } catch (_: ServerException) {
            failure(AiFailureType.ProviderUnavailable, true)
        } catch (_: IOException) {
            failure(AiFailureType.NetworkUnavailable, true)
        } catch (_: FirebaseAIException) {
            failure(AiFailureType.Unknown, true)
        } catch (_: Exception) {
            failure(AiFailureType.Unknown, true)
        }
    }

    private fun inventoryRecipeContext(request: RecipeOptionsRequest): String =
        if (request.inventoryLines.isEmpty()) "" else """

Available pantry quantities:
${request.inventoryLines.joinToString("\n")}
Strict stock only: ${request.strictStock}
Maximum missing staples: ${request.maxMissingStaples}
Servings: ${request.servings}
Prioritize: ${request.prioritizedIngredients.joinToString(", ")}
Include exact proposedIngredients for every option. Never exceed available quantities when strict stock only is true.
""".trimEnd()

    private fun inventoryPlanContext(request: CookingPlanRequest): String =
        if (request.inventoryLines.isEmpty()) ""
        else "\nAvailable pantry quantities:\n${request.inventoryLines.joinToString("\n")}\nDo not exceed these quantities."

    private fun cookingContext(
        recipeName: String,
        plan: String,
        currentStep: String,
        elapsedSeconds: Long,
        resource: String?,
        recentTurns: String,
        question: String,
        language: String,
        photo: Boolean
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
${if (photo) "Describe only visible evidence and state uncertainty." else "Answer concisely and safely."}
Return only valid JSON matching the app response schema."""

    companion object {
        const val MAX_INLINE_IMAGE_BYTES = 14 * 1024 * 1024

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private fun validShoppingResponse(response: ShoppingImportResponse): Boolean =
            response.items.isNotEmpty() && response.items.all {
                it.displayName.isNotBlank() &&
                    it.confidence.isFinite() && it.confidence in 0.0..1.0 &&
                    (it.quantity?.let { quantity -> quantity.isFinite() && quantity > 0 } != false)
            }

        private fun failure(
            type: AiFailureType,
            retryable: Boolean,
            technical: String? = null
        ) = AiResult.Failure(type, retryable, type.userMessageRes, technical)
    }
}
