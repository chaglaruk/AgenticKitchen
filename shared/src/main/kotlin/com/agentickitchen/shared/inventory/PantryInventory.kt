package com.agentickitchen.shared.inventory

import kotlinx.serialization.Serializable

enum class UnitDimension { WEIGHT, VOLUME, COUNT, PACKAGE, BUNCH, UNKNOWN }
enum class PantryLocation { FRIDGE, FREEZER, PANTRY, COUNTER, OTHER }
enum class AdjustmentMode { DELTA, REPLACE }
enum class AdjustmentReason {
    MANUAL_ADD,
    SHOPPING_ADD,
    KITCHEN_SCAN,
    RECOUNT,
    RECIPE_RESERVATION,
    RECIPE_RESERVATION_RELEASE,
    RECIPE_CONSUMPTION,
    CORRECTION,
    DELETION
}

data class PantryStockItem(
    val id: String,
    val canonicalIngredientId: String? = null,
    val originalName: String,
    val displayNameTr: String? = null,
    val displayNameEn: String? = null,
    val quantity: Double,
    val unit: String,
    val unitDimension: UnitDimension,
    val packageLabel: String? = null,
    val isEstimated: Boolean = false,
    val confidence: Double? = null,
    val source: String,
    val createdAt: String,
    val updatedAt: String,
    val location: PantryLocation = PantryLocation.PANTRY,
    val customLocationLabel: String? = null,
    val bestBefore: String? = null,
    val useBy: String? = null
)

data class InventoryAdjustmentRecord(
    val id: String,
    val itemId: String,
    val amount: Double,
    val mode: AdjustmentMode,
    val reason: AdjustmentReason,
    val source: String,
    val timestamp: String
)

data class PendingRecipeUsageRecord(
    val sessionId: String,
    val itemId: String,
    val plannedQuantity: Double,
    val unit: String,
    val actualQuantity: Double? = null,
    val status: String,
    val timestamp: String
)

@Serializable
data class ActiveCookingSessionRecord(
    val sessionId: String,
    val recipeOptionId: String,
    val recipeName: String,
    val recipeType: String,
    val description: String,
    val sourceLabel: String? = null,
    val servings: Int,
    val resolvedReadyTimeIso: String,
    val cookingPlanJson: String,
    val eventsJson: String,
    val plannedUsageJson: String,
    val status: String,
    val startedAtMillis: Long,
    val accumulatedElapsedSeconds: Long,
    val lastRunningStartMillis: Long? = null,
    val pausedAtMillis: Long? = null,
    val completedStepIdsJson: String,
    val skippedStepIdsJson: String,
    val recentChatTurnsJson: String,
    val updatedAtIso: String
)

data class InventoryMutation(
    val item: PantryStockItem,
    val adjustment: InventoryAdjustmentRecord
)

data class NormalizedAmount(
    val quantity: Double,
    val unit: String,
    val dimension: UnitDimension
)

object InventoryUnits {
    fun normalize(quantity: Double, unit: String): NormalizedAmount {
        require(quantity.isFinite() && quantity > 0) { "Quantity must be a positive finite number" }
        return when (unit.trim().lowercase()) {
            "kg", "kilogram", "kilo" -> NormalizedAmount(quantity * 1_000, "g", UnitDimension.WEIGHT)
            "g", "gram", "gr" -> NormalizedAmount(quantity, "g", UnitDimension.WEIGHT)
            "l", "litre", "liter", "litreler" -> NormalizedAmount(quantity * 1_000, "ml", UnitDimension.VOLUME)
            "ml", "millilitre", "milliliter" -> NormalizedAmount(quantity, "ml", UnitDimension.VOLUME)
            "1", "count", "adet", "piece", "pieces", "pcs" -> NormalizedAmount(quantity, "adet", UnitDimension.COUNT)
            "package", "paket", "pack", "packs" -> NormalizedAmount(quantity, "paket", UnitDimension.PACKAGE)
            "bunch", "bunches", "demet" -> NormalizedAmount(quantity, "demet", UnitDimension.BUNCH)
            "cup", "cups" -> NormalizedAmount(quantity * 240.0, "ml", UnitDimension.VOLUME)
            "tbsp", "tablespoon", "tablespoons" -> NormalizedAmount(quantity * 15.0, "ml", UnitDimension.VOLUME)
            "tsp", "teaspoon", "teaspoons" -> NormalizedAmount(quantity * 5.0, "ml", UnitDimension.VOLUME)
            else -> NormalizedAmount(quantity, unit.trim().ifBlank { "birim" }, UnitDimension.UNKNOWN)
        }
    }

    fun requireCompatible(first: NormalizedAmount, second: NormalizedAmount) {
        require(first.dimension == second.dimension && first.dimension != UnitDimension.UNKNOWN) {
            "Incompatible inventory units"
        }
    }
}

interface PantryInventoryRepository {
    fun getAll(): List<PantryStockItem>
    fun upsert(item: PantryStockItem, adjustment: InventoryAdjustmentRecord)
    fun delete(item: PantryStockItem, adjustment: InventoryAdjustmentRecord)
    fun adjustments(itemId: String): List<InventoryAdjustmentRecord>
    fun pendingUsage(sessionId: String): List<PendingRecipeUsageRecord>
    fun allPendingUsage(): List<PendingRecipeUsageRecord>
    fun upsertPendingUsage(usage: PendingRecipeUsageRecord)
    fun deletePendingUsage(sessionId: String)
    fun applyMutations(mutations: List<InventoryMutation>)
    fun reserve(usages: List<PendingRecipeUsageRecord>): Boolean
    fun releaseReservation(sessionId: String): Boolean
    fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean
    fun saveActiveSession(session: ActiveCookingSessionRecord)
    fun getActiveSession(sessionId: String): ActiveCookingSessionRecord?
    fun getAllActiveSessions(): List<ActiveCookingSessionRecord>
    fun deleteActiveSession(sessionId: String)
    fun updateMetadata(item: PantryStockItem): Boolean = false
}
