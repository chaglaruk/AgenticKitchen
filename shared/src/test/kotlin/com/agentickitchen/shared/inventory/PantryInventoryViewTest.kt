package com.agentickitchen.shared.inventory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PantryInventoryViewTest {
    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun locationFilterKeepsOnlyRequestedArea() {
        val items = listOf(
            item("Milk", PantryLocation.FRIDGE, "2026-09-01"),
            item("Rice", PantryLocation.PANTRY, null)
        )

        val result = PantryInventoryView.filterAndSort(items, PantryLocation.FRIDGE, today = today)

        assertEquals(listOf("Milk"), result.map { it.originalName })
    }

    @Test
    fun expirySortPlacesDatedItemsFirstByDate() {
        val items = listOf(
            item("Rice", PantryLocation.PANTRY, null),
            item("Milk", PantryLocation.FRIDGE, "2026-09-02"),
            item("Tomato", PantryLocation.FRIDGE, "2026-08-31")
        )

        val result = PantryInventoryView.filterAndSort(items, sortOrder = PantrySortOrder.EXPIRY, today = today)

        assertEquals(listOf("Tomato", "Milk", "Rice"), result.map { it.originalName })
    }

    @Test
    fun useFirstReturnsOnlySoonItemsAndExcludesExpired() {
        val items = listOf(
            item("Expired", PantryLocation.FRIDGE, "2026-08-29"),
            item("Today", PantryLocation.FRIDGE, "2026-08-30"),
            item("Soon", PantryLocation.FRIDGE, "2026-09-01"),
            item("Later", PantryLocation.FRIDGE, "2026-09-10")
        )

        val result = PantryInventoryView.useFirst(items, today, limit = 3)

        assertEquals(listOf("Today", "Soon"), result.map { it.originalName })
    }

    private fun item(name: String, location: PantryLocation, useBy: String?) = PantryStockItem(
        id = name,
        originalName = name,
        quantity = 4.0,
        unit = "adet",
        unitDimension = UnitDimension.COUNT,
        source = "test",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
        location = location,
        useBy = useBy
    )
}
