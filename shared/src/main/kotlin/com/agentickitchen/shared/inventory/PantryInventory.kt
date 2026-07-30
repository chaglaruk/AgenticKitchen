package com.agentickitchen.shared.inventory

enum class UnitDimension { WEIGHT, VOLUME, COUNT, PACKAGE, BUNCH, UNKNOWN }
enum class AdjustmentMode { DELTA, REPLACE }
enum class AdjustmentReason {
    MANUAL_ADD,
    SHOPPING_ADD,
    RECOUNT,
    RECIPE_RESERVATION,
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
    val updatedAt: String
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
            "count", "adet", "piece", "pieces", "pcs" -> NormalizedAmount(quantity, "adet", UnitDimension.COUNT)
            "package", "paket", "pack", "packs" -> NormalizedAmount(quantity, "paket", UnitDimension.PACKAGE)
            "bunch", "demet" -> NormalizedAmount(quantity, "demet", UnitDimension.BUNCH)
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
    fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean
}
