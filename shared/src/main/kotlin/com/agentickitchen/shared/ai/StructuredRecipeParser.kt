package com.agentickitchen.shared.ai

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object StructuredRecipeParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun recipeOptions(text: String): AiResult<RecipeOptionsResponse> = parse(text) { json.decodeFromString(it) }
    fun cookingPlan(text: String): AiResult<CookingPlanResponse> = parse(text) { json.decodeFromString(it) }

    private fun <T> parse(text: String, decode: (String) -> T): AiResult<T> = try {
        AiResult.Success(decode(text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()), AiProviderId.GEMINI, "gemini-3.6-flash")
    } catch (error: Exception) {
        AiResult.Failure(AiFailureType.InvalidResponse, true, "AI returned invalid JSON. Please retry.", error.message)
    }
}
