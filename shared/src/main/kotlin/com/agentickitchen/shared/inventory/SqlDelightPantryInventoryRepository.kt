package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.db.AppDatabase

class SqlDelightPantryInventoryRepository(private val database: AppDatabase) : PantryInventoryRepository {
    private val queries = database.appDatabaseQueries

    override fun getAll(): List<PantryStockItem> = queries.selectAllPantryItems().executeAsList().map {
        PantryStockItem(
            id = it.id,
            canonicalIngredientId = it.canonicalIngredientId,
            originalName = it.originalName,
            displayNameTr = it.displayNameTr,
            displayNameEn = it.displayNameEn,
            quantity = it.quantity,
            unit = it.unit,
            unitDimension = UnitDimension.valueOf(it.unitDimension),
            packageLabel = it.packageLabel,
            isEstimated = it.isEstimated != 0L,
            confidence = it.confidence,
            source = it.source,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }

    override fun upsert(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
        database.transaction {
            queries.upsertPantryItem(
                item.id,
                item.canonicalIngredientId,
                item.originalName,
                item.displayNameTr,
                item.displayNameEn,
                item.quantity,
                item.unit,
                item.unitDimension.name,
                item.packageLabel,
                if (item.isEstimated) 1L else 0L,
                item.confidence,
                item.source,
                item.createdAt,
                item.updatedAt
            )
            insertAdjustment(adjustment)
        }
    }

    override fun delete(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
        database.transaction {
            insertAdjustment(adjustment)
            queries.deletePantryItem(item.id)
        }
    }

    override fun adjustments(itemId: String): List<InventoryAdjustmentRecord> =
        queries.selectInventoryAdjustments(itemId).executeAsList().map {
            InventoryAdjustmentRecord(
                id = it.id,
                itemId = it.itemId,
                amount = it.amount,
                mode = AdjustmentMode.valueOf(it.mode),
                reason = AdjustmentReason.valueOf(it.reason),
                source = it.source,
                timestamp = it.timestamp
            )
        }

    override fun pendingUsage(sessionId: String): List<PendingRecipeUsageRecord> =
        queries.selectPendingRecipeUsage(sessionId).executeAsList().map {
            PendingRecipeUsageRecord(
                sessionId = it.sessionId,
                itemId = it.itemId,
                plannedQuantity = it.plannedQuantity,
                unit = it.unit,
                actualQuantity = it.actualQuantity,
                status = it.status,
                timestamp = it.timestamp
            )
        }

    override fun upsertPendingUsage(usage: PendingRecipeUsageRecord) {
        queries.upsertPendingRecipeUsage(
            usage.sessionId,
            usage.itemId,
            usage.plannedQuantity,
            usage.unit,
            usage.actualQuantity,
            usage.status,
            usage.timestamp
        )
    }

    override fun deletePendingUsage(sessionId: String) {
        queries.deletePendingRecipeUsage(sessionId)
    }

    private fun insertAdjustment(adjustment: InventoryAdjustmentRecord) {
        queries.insertInventoryAdjustment(
            adjustment.id,
            adjustment.itemId,
            adjustment.amount,
            adjustment.mode.name,
            adjustment.reason.name,
            adjustment.source,
            adjustment.timestamp
        )
    }
}
