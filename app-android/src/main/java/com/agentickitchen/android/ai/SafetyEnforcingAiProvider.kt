package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.CookingPhotoResponse
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingImportResponse
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.SubstitutionPlanRequest
import com.agentickitchen.shared.ai.SubstitutionPlanResponse
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.VisionSafetyPolicy
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import java.io.Closeable

class SafetyEnforcingAiProvider(
    private val delegate: KitchenAiProvider
) : KitchenAiProvider, Closeable {
    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> =
        delegate.generateRecipeOptions(request)

    override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> =
        delegate.generateCookingPlan(request)

    override suspend fun generateSubstitution(request: SubstitutionPlanRequest): AiResult<SubstitutionPlanResponse> =
        delegate.generateSubstitution(request)

    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =
        delegate.parseShoppingText(request)

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
        when (val result = delegate.scanShoppingPhoto(request)) {
            is AiResult.Failure -> result
            is AiResult.Success -> result.copy(
                value = VisionSafetyPolicy.filterShoppingCandidates(result.value)
            )
        }

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
        when (val result = delegate.inspectCookingPhoto(request)) {
            is AiResult.Failure -> result
            is AiResult.Success -> {
                if (!VisionSafetyPolicy.validateCookingPhoto(result.value)) {
                    com.agentickitchen.shared.ai.AiResult.Failure(
                        com.agentickitchen.shared.ai.AiFailureType.InvalidResponse,
                        retryable = true,
                        userMessage = com.agentickitchen.shared.ai.AiFailureType.InvalidResponse.userMessageRes,
                        technicalMessage = "unsafe_vision_response"
                    )
                } else {
                    result.copy(
                        value = VisionSafetyPolicy.requireUserConfirmation(
                            response = result.value,
                            language = request.language
                        )
                    )
                }
            }
        }

    override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> =
        delegate.askCookingAssistant(request)

    override suspend fun testConnection(): AiResult<Unit> = delegate.testConnection()

    override fun close() {
        (delegate as? Closeable)?.close()
    }
}
