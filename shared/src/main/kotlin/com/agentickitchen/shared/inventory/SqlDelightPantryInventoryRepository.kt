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
            upsertItem(item)
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
        queries.selectPendingRecipeUsage(sessionId).executeAsList().map(::pendingRecord)

    override fun allPendingUsage(): List<PendingRecipeUsageRecord> =
        queries.selectAllPendingRecipeUsage().executeAsList().map(::pendingRecord)

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

    override fun applyMutations(mutations: List<InventoryMutation>) {
        database.transaction {
            mutations.forEach {
                upsertItem(it.item)
                insertAdjustment(it.adjustment)
            }
        }
    }

    override fun reserve(usages: List<PendingRecipeUsageRecord>): Boolean = try {
        database.transaction {
            val existingReservations = queries.selectAllPendingRecipeUsage().executeAsList()
            usages.forEach { usage ->
                val item = queries.selectPantryItem(usage.itemId).executeAsOneOrNull()
                    ?: throw InventoryConflict()
                val planned = InventoryUnits.normalize(usage.plannedQuantity, usage.unit)
                val available = InventoryUnits.normalize(item.quantity, item.unit)
                InventoryUnits.requireCompatible(planned, available)
                val alreadyReserved = existingReservations
                    .filter { it.itemId == usage.itemId && it.sessionId != usage.sessionId && it.status == "reserved" }
                    .sumOf { InventoryUnits.normalize(it.plannedQuantity, it.unit).quantity }
                if (planned.quantity > available.quantity - alreadyReserved + 0.000_001) {
                    throw InventoryConflict()
                }
            }
            usages.forEach { usage ->
                upsertPendingUsage(usage)
                insertAdjustment(
                    InventoryAdjustmentRecord(
                        id = "${usage.sessionId}:${usage.itemId}:reservation",
                        itemId = usage.itemId,
                        amount = usage.plannedQuantity,
                        mode = AdjustmentMode.DELTA,
                        reason = AdjustmentReason.RECIPE_RESERVATION,
                        source = "recipe",
                        timestamp = usage.timestamp
                    )
                )
            }
        }
        true
    } catch (_: InventoryConflict) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean = try {
        database.transaction {
            val usages = queries.selectPendingRecipeUsage(sessionId).executeAsList()
            val allReservations = queries.selectAllPendingRecipeUsage().executeAsList()
            if (usages.isEmpty()) throw InventoryConflict()
            usages.forEach { usage ->
                val itemRow = queries.selectPantryItem(usage.itemId).executeAsOneOrNull()
                    ?: throw InventoryConflict()
                val item = pantryItem(itemRow)
                val actual = actualQuantities[usage.itemId] ?: usage.plannedQuantity
                val consumed = InventoryUnits.normalize(actual, usage.unit)
                val available = InventoryUnits.normalize(item.quantity, item.unit)
                InventoryUnits.requireCompatible(consumed, available)
                val reservedElsewhere = allReservations
                    .filter { it.itemId == usage.itemId && it.sessionId != sessionId && it.status == "reserved" }
                    .sumOf { InventoryUnits.normalize(it.plannedQuantity, it.unit).quantity }
                if (consumed.quantity > available.quantity - reservedElsewhere + 0.000_001) {
                    throw InventoryConflict()
                }
                val now = java.time.Instant.now().toString()
                upsertItem(item.copy(quantity = available.quantity - consumed.quantity, unit = available.unit, updatedAt = now))
                insertAdjustment(
                    InventoryAdjustmentRecord(
                        id = "$sessionId:${usage.itemId}:consumption",
                        itemId = usage.itemId,
                        amount = -consumed.quantity,
                        mode = AdjustmentMode.DELTA,
                        reason = AdjustmentReason.RECIPE_CONSUMPTION,
                        source = "recipe",
                        timestamp = now
                    )
                )
            }
            queries.deletePendingRecipeUsage(sessionId)
        }
        true
    } catch (_: InventoryConflict) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun upsertItem(item: PantryStockItem) {
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
    }

    private fun pantryItem(it: com.agentickitchen.shared.db.PantryItem) = PantryStockItem(
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

    private fun pendingRecord(it: com.agentickitchen.shared.db.PendingRecipeUsage) =
        PendingRecipeUsageRecord(
            sessionId = it.sessionId,
            itemId = it.itemId,
            plannedQuantity = it.plannedQuantity,
            unit = it.unit,
            actualQuantity = it.actualQuantity,
            status = it.status,
            timestamp = it.timestamp
        )

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

    private class InventoryConflict : RuntimeException()
}
