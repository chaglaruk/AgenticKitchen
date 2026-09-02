package com.agentickitchen.shared.inventory

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PantryFreshnessStatus {
    FRESH,
    USE_SOON,
    EXPIRES_TODAY,
    EXPIRED,
    LOW_STOCK
}

data class PantryFreshnessInfo(
    val status: PantryFreshnessStatus,
    val effectiveDate: LocalDate? = null,
    val daysUntilDate: Long? = null
)

object PantryFreshnessPolicy {
    const val DEFAULT_USE_SOON_DAYS = 3L

    fun evaluate(
        item: PantryStockItem,
        today: LocalDate = LocalDate.now(),
        useSoonDays: Long = DEFAULT_USE_SOON_DAYS
    ): PantryFreshnessInfo {
        require(useSoonDays >= 0) { "useSoonDays must be non-negative" }
        val effectiveDate = effectiveDate(item)
        val daysUntilDate = effectiveDate?.let { ChronoUnit.DAYS.between(today, it) }

        val status = when {
            daysUntilDate != null && daysUntilDate < 0 -> PantryFreshnessStatus.EXPIRED
            daysUntilDate == 0L -> PantryFreshnessStatus.EXPIRES_TODAY
            daysUntilDate != null && daysUntilDate <= useSoonDays -> PantryFreshnessStatus.USE_SOON
            isLowStock(item) -> PantryFreshnessStatus.LOW_STOCK
            else -> PantryFreshnessStatus.FRESH
        }

        return PantryFreshnessInfo(status, effectiveDate, daysUntilDate)
    }

    fun effectiveDate(item: PantryStockItem): LocalDate? =
        listOfNotNull(parseIsoDate(item.useBy), parseIsoDate(item.bestBefore)).minOrNull()

    fun parseIsoDate(value: String?): LocalDate? =
        value?.trim()?.takeIf(String::isNotEmpty)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun isLowStock(item: PantryStockItem): Boolean = when (item.unitDimension) {
        UnitDimension.WEIGHT -> item.quantity <= 150.0
        UnitDimension.VOLUME -> item.quantity <= 200.0
        UnitDimension.COUNT, UnitDimension.PACKAGE, UnitDimension.BUNCH -> item.quantity <= 1.0
        UnitDimension.UNKNOWN -> false
    }
}
