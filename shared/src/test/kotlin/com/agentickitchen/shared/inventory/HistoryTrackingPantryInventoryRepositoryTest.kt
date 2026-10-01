package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HistoryTrackingPantryInventoryRepositoryTest {
    @Test
    fun `successful consumption completes history entry`() {
        val history = RecordingHistory()
        val repository = HistoryTrackingPantryInventoryRepository(StubInventory(), history)

        assertTrue(repository.consume("session", emptyMap()))
        assertEquals("completed", history.statuses["session"])
    }

    @Test
    fun `released reservation cancels history entry`() {
        val history = RecordingHistory()
        val repository = HistoryTrackingPantryInventoryRepository(StubInventory(), history)

        assertTrue(repository.releaseReservation("session"))
        assertEquals("cancelled", history.statuses["session"])
    }

    @Test
    fun `terminal active-session states update history`() {
        val history = RecordingHistory()
        val repository = HistoryTrackingPantryInventoryRepository(StubInventory(), history)

        repository.saveActiveSession(session("COMPLETED"))
        assertEquals("completed", history.statuses["session"])

        repository.saveActiveSession(session("ENDED"))
        assertEquals("ended", history.statuses["session"])
    }

    private fun session(status: String) = ActiveCookingSessionRecord(
        sessionId = "session",
        recipeOptionId = "option",
        recipeName = "Recipe",
        recipeType = "test",
        description = "description",
        servings = 2,
        resolvedReadyTimeIso = "2026-08-01T12:00:00Z",
        cookingPlanJson = "{}",
        eventsJson = "[]",
        plannedUsageJson = "[]",
        status = status,
        startedAtMillis = 0,
        accumulatedElapsedSeconds = 0,
        completedStepIdsJson = "[]",
        skippedStepIdsJson = "[]",
        recentChatTurnsJson = "[]",
        updatedAtIso = "2026-08-01T12:00:00Z"
    )

    private class RecordingHistory : RecipeHistoryRepository {
        val statuses = mutableMapOf<String, String>()
        override fun getAllHistory(): List<RecipeHistory> = emptyList()
        override fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String) = Unit
        override fun updateStatus(id: String, status: String) { statuses[id] = status }
        override fun deleteRecipe(id: String) = Unit
    }

    private class StubInventory : PantryInventoryRepository {
        override fun getAll(): List<PantryStockItem> = emptyList()
        override fun upsert(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) = Unit
        override fun delete(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) = Unit
        override fun adjustments(itemId: String): List<InventoryAdjustmentRecord> = emptyList()
        override fun pendingUsage(sessionId: String): List<PendingRecipeUsageRecord> = emptyList()
        override fun allPendingUsage(): List<PendingRecipeUsageRecord> = emptyList()
        override fun upsertPendingUsage(usage: PendingRecipeUsageRecord) = Unit
        override fun deletePendingUsage(sessionId: String) = Unit
        override fun applyMutations(mutations: List<InventoryMutation>) = Unit
        override fun reserve(usages: List<PendingRecipeUsageRecord>): Boolean = true
        override fun releaseReservation(sessionId: String): Boolean = true
        override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean = true
        override fun saveActiveSession(session: ActiveCookingSessionRecord) = Unit
        override fun getActiveSession(sessionId: String): ActiveCookingSessionRecord? = null
        override fun getAllActiveSessions(): List<ActiveCookingSessionRecord> = emptyList()
        override fun deleteActiveSession(sessionId: String) = Unit
    }
}
