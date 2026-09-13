package com.agentickitchen.shared.inventory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PantryFreshnessPolicyTest {
    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun useByTodayExpiresToday() {
        val info = PantryFreshnessPolicy.evaluate(item(useBy = "2026-08-30"), today)

        assertEquals(PantryFreshnessStatus.EXPIRES_TODAY, info.status)
        assertEquals(0L, info.daysUntilDate)
    }

    @Test
    fun dateInsideUseSoonWindowWinsOverLowStock() {
        val info = PantryFreshnessPolicy.evaluate(
            item(quantity = 1.0, useBy = "2026-09-01"),
            today
        )

        assertEquals(PantryFreshnessStatus.USE_SOON, info.status)
        assertEquals(2L, info.daysUntilDate)
    }

    @Test
    fun pastDateIsExpired() {
        val info = PantryFreshnessPolicy.evaluate(item(bestBefore = "2026-08-29"), today)

        assertEquals(PantryFreshnessStatus.EXPIRED, info.status)
    }

    @Test
    fun lowCountWithoutExpiryIsLowStock() {
        val info = PantryFreshnessPolicy.evaluate(item(quantity = 1.0), today)

        assertEquals(PantryFreshnessStatus.LOW_STOCK, info.status)
    }

    @Test
    fun healthyStockWithoutExpiryIsFresh() {
        val info = PantryFreshnessPolicy.evaluate(item(quantity = 6.0), today)

        assertEquals(PantryFreshnessStatus.FRESH, info.status)
    }

    @Test
    fun earliestAvailableDateDrivesFreshness() {
        val info = PantryFreshnessPolicy.evaluate(
            item(bestBefore = "2026-09-10", useBy = "2026-09-02"),
            today
        )

        assertEquals(LocalDate.of(2026, 9, 2), info.effectiveDate)
        assertEquals(PantryFreshnessStatus.USE_SOON, info.status)
    }

    private fun item(
        quantity: Double = 4.0,
        bestBefore: String? = null,
        useBy: String? = null
    ) = PantryStockItem(
        id = "test",
        originalName = "Egg",
        quantity = quantity,
        unit = "adet",
        unitDimension = UnitDimension.COUNT,
        source = "test",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
        bestBefore = bestBefore,
        useBy = useBy
    )
}
