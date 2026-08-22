package com.agentickitchen.android.ai

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
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import java.io.Closeable

/**
 * Hydrates offline inventory recipe previews with the same deterministic ingredient plan
 * that will be used when the selected option is expanded into a cooking plan.
 *
 * Inventory-backed UI validation requires proposedIngredients at option time. The local
 * provider historically left that field empty even though it can already produce a complete
 * cooking plan, which made every offline inventory option fail before a session could start.
 */
class InventoryAwareOfflineProvider(
    private val delegate: KitchenAiProvider = LocalRecipeProvider()
) : KitchenAiProvider, Closeable {

    override suspend fun generateRecipeOptions(request: RecipeOptionsRequest): AiResult<RecipeOptionsResponse> {
        val result = delegate.generateRecipeOptions(request)
        if (result !is AiResult.Success || request.inventoryLines.isEmpty()) return result

        val hydratedOptions = mutableListOf<RecipeOptionDto>()
        for (option in result.value.options) {
            when (val plan = delegate.generateCookingPlan(planRequest(option, request))) {
                is AiResult.Success -> hydratedOptions += option.copy(proposedIngredients = plan.value.ingredients)
                is AiResult.Failure -> return plan
            }
        }

        return AiResult.Success(
            value = result.value.copy(options = hydratedOptions),
            provider = result.provider,
            model = result.model
        )
    }

    override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> =
        delegate.generateCookingPlan(request)

    override suspend fun parseShoppingText(request: ShoppingTextRequest): AiResult<ShoppingImportResponse> =
        delegate.parseShoppingText(request)

    override suspend fun scanShoppingPhoto(request: ShoppingPhotoRequest): AiResult<ShoppingImportResponse> =
        delegate.scanShoppingPhoto(request)

    override suspend fun inspectCookingPhoto(request: CookingPhotoRequest): AiResult<CookingPhotoResponse> =
        delegate.inspectCookingPhoto(request)

    override suspend fun askCookingAssistant(request: CookingChatRequest): AiResult<CookingChatResponse> =
        delegate.askCookingAssistant(request)

    override suspend fun testConnection(): AiResult<Unit> = delegate.testConnection()

    override fun close() {
        (delegate as? Closeable)?.close()
    }

    private fun planRequest(option: RecipeOptionDto, request: RecipeOptionsRequest) = CookingPlanRequest(
        recipeName = option.name,
        ingredients = request.ingredients,
        equipment = request.equipment,
        servings = request.servings,
        stoveType = stoveType(request.equipment),
        stoveMaxLevel = 9,
        ovenAvailable = "oven" in request.equipment,
        ovenHasFan = false,
        airfryerAvailable = "airfryer" in request.equipment,
        dietType = request.dietType,
        allergies = request.allergies,
        language = request.language,
        inventoryLines = request.inventoryLines
    )

    private fun stoveType(equipment: Set<String>): String = when {
        "gas" in equipment || "camping" in equipment -> "gas"
        "elec" in equipment -> "electric"
        else -> "none"
    }
}
