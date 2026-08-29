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
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.ai.dto.RecipeOptionDto
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
import com.agentickitchen.shared.inventory.InventoryUnits
import com.agentickitchen.shared.inventory.LocalIngredientResolver
import com.agentickitchen.shared.inventory.UnitDimension
import com.agentickitchen.shared.validator.canonicalCookingUnit
import java.io.Closeable

/**
 * Hydrates offline inventory recipe previews with the deterministic ingredient plan that will
 * also be used when the selected option is expanded into a cooking plan.
 *
 * LocalRecipeProvider intentionally knows nothing about persisted pantry quantities. This
 * wrapper fits its deterministic recipe quantities to the inventory snapshot supplied by the
 * request so strict-stock validation never rejects a recipe merely because the offline default
 * portion is larger than the quantity the user actually has.
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
                is AiResult.Success -> hydratedOptions += option.copy(
                    proposedIngredients = fitPlanToInventory(plan.value, request.inventoryLines).ingredients
                )
                is AiResult.Failure -> return plan
            }
        }

        return AiResult.Success(
            value = result.value.copy(options = hydratedOptions),
            provider = result.provider,
            model = result.model
        )
    }

    override suspend fun generateCookingPlan(request: CookingPlanRequest): AiResult<CookingPlanResponse> {
        val result = delegate.generateCookingPlan(request)
        if (result !is AiResult.Success || request.inventoryLines.isEmpty()) return result
        return AiResult.Success(
            value = fitPlanToInventory(result.value, request.inventoryLines),
            provider = result.provider,
            model = result.model
        )
    }

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

    private fun fitPlanToInventory(
        plan: CookingPlanResponse,
        inventoryLines: List<String>
    ): CookingPlanResponse {
        val inventory = inventoryLines.mapNotNull(::parseInventoryLine)
        if (inventory.isEmpty()) return plan
        return plan.copy(
            ingredients = plan.ingredients.map { ingredient ->
                val stock = inventory.firstOrNull { line ->
                    LocalIngredientResolver.matches(
                        firstName = line.name,
                        firstCanonicalId = null,
                        secondName = ingredient.name,
                        secondCanonicalId = ingredient.canonicalIngredientId
                    )
                }
                if (stock == null) ingredient else fitIngredientToStock(ingredient, stock, plan.servings)
            }
        )
    }

    private fun fitIngredientToStock(
        ingredient: PlannedIngredientDto,
        stock: InventoryLine,
        servings: Int
    ): PlannedIngredientDto {
        val available = runCatching { InventoryUnits.normalize(stock.quantity, stock.unit) }.getOrNull()
            ?: return ingredient
        if (available.dimension == UnitDimension.UNKNOWN) return ingredient

        val desired = comparableCookingAmount(ingredient)
        val desiredQuantity = if (desired?.dimension == available.dimension) {
            desired.quantity
        } else {
            fallbackQuantity(available.dimension, servings)
        }
        val fittedQuantity = minOf(desiredQuantity, available.quantity).coerceAtLeast(0.000_001)
        return ingredient.copy(
            quantity = fittedQuantity,
            unit = cookingUnitFor(available.dimension)
        )
    }

    private fun comparableCookingAmount(ingredient: PlannedIngredientDto): ComparableAmount? {
        val unit = canonicalCookingUnit(ingredient.unit)
        val quantity = ingredient.quantity
        if (!quantity.isFinite() || quantity <= 0.0) return null
        return when (unit) {
            "g" -> ComparableAmount(quantity, UnitDimension.WEIGHT)
            "kg" -> ComparableAmount(quantity * 1_000.0, UnitDimension.WEIGHT)
            "ml" -> ComparableAmount(quantity, UnitDimension.VOLUME)
            "l" -> ComparableAmount(quantity * 1_000.0, UnitDimension.VOLUME)
            "tsp" -> ComparableAmount(quantity * 5.0, UnitDimension.VOLUME)
            "tbsp" -> ComparableAmount(quantity * 15.0, UnitDimension.VOLUME)
            "cup" -> ComparableAmount(quantity * 240.0, UnitDimension.VOLUME)
            "piece", "clove", "slice", "unit" -> ComparableAmount(quantity, UnitDimension.COUNT)
            "package" -> ComparableAmount(quantity, UnitDimension.PACKAGE)
            "bunch" -> ComparableAmount(quantity, UnitDimension.BUNCH)
            else -> null
        }
    }

    private fun fallbackQuantity(dimension: UnitDimension, servings: Int): Double = when (dimension) {
        UnitDimension.WEIGHT -> 100.0 * servings
        UnitDimension.VOLUME -> 100.0 * servings
        UnitDimension.COUNT -> servings.toDouble().coerceAtLeast(1.0)
        UnitDimension.PACKAGE, UnitDimension.BUNCH -> 1.0
        UnitDimension.UNKNOWN -> 1.0
    }

    private fun cookingUnitFor(dimension: UnitDimension): String = when (dimension) {
        UnitDimension.WEIGHT -> "g"
        UnitDimension.VOLUME -> "ml"
        UnitDimension.COUNT -> "piece"
        UnitDimension.PACKAGE -> "package"
        UnitDimension.BUNCH -> "bunch"
        UnitDimension.UNKNOWN -> "unit"
    }

    private fun parseInventoryLine(line: String): InventoryLine? {
        val match = INVENTORY_LINE.matchEntire(line.trim()) ?: return null
        val quantity = match.groupValues[1].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
            ?: return null
        val unit = match.groupValues[2].trim()
        val name = match.groupValues[3].trim().takeIf(String::isNotEmpty) ?: return null
        return InventoryLine(quantity, unit, name)
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

    private data class InventoryLine(val quantity: Double, val unit: String, val name: String)
    private data class ComparableAmount(val quantity: Double, val dimension: UnitDimension)

    private companion object {
        val INVENTORY_LINE = Regex("""^(\d+(?:[.,]\d+)?)\s+(\S+)\s+(.+)$""")
    }
}
