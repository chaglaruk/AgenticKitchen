package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.db.RecipeHistoryRepository

class HistoryTrackingPantryInventoryRepository(
    private val delegate: PantryInventoryRepository,
    private val historyRepository: RecipeHistoryRepository
) : PantryInventoryRepository by delegate {

    override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean {
        val consumed = delegate.consume(sessionId, actualQuantities)
        if (consumed) historyRepository.updateStatus(sessionId, "completed")
        return consumed
    }

    override fun releaseReservation(sessionId: String): Boolean {
        val released = delegate.releaseReservation(sessionId)
        if (released) historyRepository.updateStatus(sessionId, "cancelled")
        return released
    }

    override fun saveActiveSession(session: ActiveCookingSessionRecord) {
        delegate.saveActiveSession(session)
        when (session.status.uppercase()) {
            "COMPLETED" -> historyRepository.updateStatus(session.sessionId, "completed")
            "ENDED" -> historyRepository.updateStatus(session.sessionId, "ended")
        }
    }

    override fun updateMetadata(item: PantryStockItem): Boolean = delegate.updateMetadata(item)
}
