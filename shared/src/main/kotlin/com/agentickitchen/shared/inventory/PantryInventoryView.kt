package com.agentickitchen.shared.inventory

import java.time.LocalDate
import java.util.Locale

enum class PantrySortOrder { EXPIRY, NAME, QUANTITY }

object PantryInventoryView {
    fun filterAndSort(
        items: List<PantryStockItem>,
        location: PantryLocation? = null,
        sortOrder: PantrySortOrder = PantrySortOrder.EXPIRY,
        today: LocalDate = LocalDate.now()
    ): List<PantryStockItem> {
        val filtered = location?.let { target -> items.filter { it.location == target } } ?: items
        return when (sortOrder) {
            PantrySortOrder.EXPIRY -> filtered.sortedWith(
                compareBy<PantryStockItem> { PantryFreshnessPolicy.effectiveDate(it) ?: LocalDate.MAX }
                    .thenBy { it.originalName.lowercase(Locale.ROOT) }
            )
            PantrySortOrder.NAME -> filtered.sortedBy { it.originalName.lowercase(Locale.ROOT) }
            PantrySortOrder.QUANTITY -> filtered.sortedWith(
                compareBy<PantryStockItem> { it.quantity }
                    .thenBy { it.originalName.lowercase(Locale.ROOT) }
            )
        }
    }

    fun useFirst(
        items: List<PantryStockItem>,
        today: LocalDate = LocalDate.now(),
        limit: Int = 3
    ): List<PantryStockItem> {
        if (limit <= 0) return emptyList()
        return items.asSequence()
            .map { it to PantryFreshnessPolicy.evaluate(it, today) }
            .filter { (_, info) ->
                info.status == PantryFreshnessStatus.EXPIRES_TODAY ||
                    info.status == PantryFreshnessStatus.USE_SOON
            }
            .sortedWith(
                compareBy<Pair<PantryStockItem, PantryFreshnessInfo>> { it.second.effectiveDate ?: LocalDate.MAX }
                    .thenBy { it.first.originalName.lowercase(Locale.ROOT) }
            )
            .take(limit)
            .map(Pair<PantryStockItem, PantryFreshnessInfo>::first)
            .toList()
    }
}
