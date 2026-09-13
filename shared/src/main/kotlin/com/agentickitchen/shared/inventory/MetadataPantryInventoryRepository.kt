package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.db.AppDatabase

class MetadataPantryInventoryRepository(
    private val delegate: PantryInventoryRepository,
    database: AppDatabase
) : PantryInventoryRepository {
    private val queries = database.appDatabaseQueries

    override fun getAll(): List<PantryStockItem> = delegate.getAll().map(::attachMetadata)

    override fun upsert(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
        val existingMetadata = queries.selectPantryItemMetadata(item.id).executeAsOneOrNull()
        delegate.upsert(item, adjustment)
        if (existingMetadata == null) {
            upsertMetadata(item)
        }
    }

    override fun delete(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
        delegate.delete(item, adjustment)
        queries.deletePantryItemMetadata(item.id)
    }

    override fun adjustments(itemId: String): List<InventoryAdjustmentRecord> = delegate.adjustments(itemId)

    override fun pendingUsage(sessionId: String): List<PendingRecipeUsageRecord> = delegate.pendingUsage(sessionId)

    override fun allPendingUsage(): List<PendingRecipeUsageRecord> = delegate.allPendingUsage()

    override fun upsertPendingUsage(usage: PendingRecipeUsageRecord) = delegate.upsertPendingUsage(usage)

    override fun deletePendingUsage(sessionId: String) = delegate.deletePendingUsage(sessionId)

    override fun applyMutations(mutations: List<InventoryMutation>) {
        val newIds = mutations.filter { queries.selectPantryItemMetadata(it.item.id).executeAsOneOrNull() == null }
            .map { it.item.id }
            .toSet()
        delegate.applyMutations(mutations)
        mutations.filter { it.item.id in newIds }.forEach { upsertMetadata(it.item) }
    }

    override fun reserve(usages: List<PendingRecipeUsageRecord>): Boolean = delegate.reserve(usages)

    override fun releaseReservation(sessionId: String): Boolean = delegate.releaseReservation(sessionId)

    override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean =
        delegate.consume(sessionId, actualQuantities)

    override fun saveActiveSession(session: ActiveCookingSessionRecord) = delegate.saveActiveSession(session)

    override fun getActiveSession(sessionId: String): ActiveCookingSessionRecord? = delegate.getActiveSession(sessionId)

    override fun getAllActiveSessions(): List<ActiveCookingSessionRecord> = delegate.getAllActiveSessions()

    override fun deleteActiveSession(sessionId: String) = delegate.deleteActiveSession(sessionId)

    override fun updateMetadata(item: PantryStockItem): Boolean = runCatching {
        upsertMetadata(item)
        true
    }.getOrDefault(false)

    private fun attachMetadata(item: PantryStockItem): PantryStockItem {
        val metadata = queries.selectPantryItemMetadata(item.id).executeAsOneOrNull() ?: return item
        val location = runCatching { PantryLocation.valueOf(metadata.location) }.getOrDefault(PantryLocation.PANTRY)
        return item.copy(
            location = location,
            customLocationLabel = metadata.customLocationLabel,
            bestBefore = metadata.bestBefore,
            useBy = metadata.useBy
        )
    }

    private fun upsertMetadata(item: PantryStockItem) {
        queries.upsertPantryItemMetadata(
            itemId = item.id,
            location = item.location.name,
            customLocationLabel = item.customLocationLabel?.trim()?.takeIf(String::isNotEmpty),
            bestBefore = item.bestBefore?.trim()?.takeIf(String::isNotEmpty),
            useBy = item.useBy?.trim()?.takeIf(String::isNotEmpty)
        )
    }
}
