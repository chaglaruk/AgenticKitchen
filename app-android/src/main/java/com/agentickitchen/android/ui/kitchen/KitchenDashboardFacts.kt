package com.agentickitchen.android.ui.kitchen

import com.agentickitchen.shared.inventory.PantryFreshnessPolicy
import com.agentickitchen.shared.inventory.PantryFreshnessStatus
import com.agentickitchen.shared.inventory.PantryStockItem
import java.time.LocalDate

data class KitchenDashboardFacts(
    val pantryCount: Int,
    val selectedCount: Int,
    val expiringItems: List<PantryStockItem>,
    val readinessScore: Int,
    val intelligenceMessage: String?
)

fun KitchenUiState.dashboardFacts(today: LocalDate = LocalDate.now()): KitchenDashboardFacts {
    val expiring = inventory.filter { item ->
        when (PantryFreshnessPolicy.evaluate(item, today).status) {
            PantryFreshnessStatus.USE_SOON,
            PantryFreshnessStatus.EXPIRES_TODAY,
            PantryFreshnessStatus.EXPIRED -> true
            PantryFreshnessStatus.FRESH,
            PantryFreshnessStatus.LOW_STOCK -> false
        }
    }
    val message = (pantryIntel.warnings + pantryIntel.tactics)
        .firstOrNull { it.message.isNotBlank() }
        ?.message
        ?.trim()

    return KitchenDashboardFacts(
        pantryCount = inventory.size,
        selectedCount = chips.size,
        expiringItems = expiring,
        readinessScore = pantryIntel.readinessScore.coerceIn(0, 100),
        intelligenceMessage = message
    )
}
