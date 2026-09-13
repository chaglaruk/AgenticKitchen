package com.agentickitchen.shared.ai

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import kotlinx.serialization.Serializable

interface KitchenAiProvider {
    suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse>
    suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse>
    suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =
        AiResult.Failure(
            AiFailureType.ProviderUnavailable,
            retryable = false,
            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,
            technicalMessage = "substitution_not_supported"
        )
    suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse>
    suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse>
    suspend fun parseRecipeText(request: RecipeTextImportRequest): AiResult<RecipeImportResponse> =
        AiResult.Failure(
            AiFailureType.ProviderUnavailable,
            retryable = false,
            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,
            technicalMessage = "recipe_import_not_supported"
        )
    suspend fun scanRecipePhoto(request: RecipePhotoImportRequest): AiResult<RecipeImportResponse> =
        AiResult.Failure(
            AiFailureType.ProviderUnavailable,
            retryable = false,
            userMessage = AiFailureType.ProviderUnavailable.userMessageRes,
            technicalMessage = "recipe_import_not_supported"
        )
    suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse>
    suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse>
    suspend fun testConnection(): AiResult<Unit>
}

data class RecipeOptionsRequest(
    val ingredients: List<String>,
    val equipment: Set<String>,
    val dietType: String,
    val allergies: Set<String>,
    val language: String,
    val inventoryLines: List<String> = emptyList(),
    val strictStock: Boolean = false,
    val maxMissingStaples: Int = 0,
    val prioritizedIngredients: List<String> = emptyList(),
    val servings: Int = 2
)

data class CookingPlanRequest(
    val recipeName: String,
    val ingredients: List<String>,
    val equipment: Set<String>,
    val servings: Int,
    val stoveType: String,
    val stoveMaxLevel: Int,
    val ovenAvailable: Boolean,
    val ovenHasFan: Boolean,
    val airfryerAvailable: Boolean,
    val dietType: String,
    val allergies: Set<String>,
    val language: String,
    val inventoryLines: List<String> = emptyList(),
    val sourceRecipeIngredientLines: List<String> = emptyList(),
    val sourceRecipeInstructions: List<String> = emptyList()
)

data class ShoppingTextRequest(val text: String, val language: String)

data class KitchenImage(
    val bytes: ByteArray,
    val mimeType: String
)

data class ShoppingPhotoRequest(
    val image: KitchenImage,
    val language: String
)

data class CookingPhotoRequest(
    val recipeName: String,
    val plan: CookingPlanResponse,
    val currentStep: String,
    val elapsedSeconds: Long,
    val resource: String?,
    val recentTurns: List<CookingChatTurn>,
    val question: String,
    val image: KitchenImage,
    val language: String
)

data class CookingChatRequest(
    val recipeName: String,
    val plan: CookingPlanResponse,
    val currentStep: String,
    val elapsedSeconds: Long,
    val resource: String?,
    val recentTurns: List<CookingChatTurn>,
    val question: String,
    val language: String
)

@Serializable
data class ShoppingImportResponse(val items: List<ShoppingCandidate>)

@Serializable
data class ShoppingCandidate(
    val canonicalIngredientId: String? = null,
    val displayName: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val unitDimension: String = "unknown",
    val packageLabel: String? = null,
    val confidence: Double,
    val estimated: Boolean,
    val uncertaintyReason: String? = null
)

@Serializable
data class CookingPhotoResponse(
    val assessment: String,
    val visibleObservation: String,
    val immediateAction: String,
    val heatAdjustment: String? = null,
    val recheckAfterSeconds: Int? = null,
    val safetyWarning: String? = null,
    val uncertainty: String
)

@Serializable
data class CookingChatResponse(val answer: String)

@Serializable
data class CookingChatTurn(val role: String, val text: String)
