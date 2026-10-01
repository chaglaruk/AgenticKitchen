package com.agentickitchen.android.ui.kitchen

import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.UnitDimension
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.PantryIntelSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class KitchenDashboardFactsTest {
    private val today = LocalDate.of(2026, 9, 7)

    @Test
    fun factsUseRealCountsWithoutDemoFloors() {
        val state = state(inventory = emptyList(), chips = emptyList())
        val facts = state.dashboardFacts(today)

        assertEquals(0, facts.pantryCount)
        assertEquals(0, facts.selectedCount)
        assertTrue(facts.expiringItems.isEmpty())
    }

    @Test
    fun factsDeriveUseSoonItemsFromRealFreshnessDates() {
        val useSoon = item("mushroom", "Mushrooms", useBy = "2026-09-09")
        val fresh = item("rice", "Rice", bestBefore = "2026-12-01")
        val state = state(inventory = listOf(useSoon, fresh), chips = listOf("Rice"))
        val facts = state.dashboardFacts(today)

        assertEquals(2, facts.pantryCount)
        assertEquals(1, facts.selectedCount)
        assertEquals(listOf("Mushrooms"), facts.expiringItems.map { it.originalName })
    }

    @Test
    fun factsUseExistingPantryIntelligenceMessageWhenPresent() {
        val state = state(
            inventory = emptyList(),
            chips = emptyList(),
            warning = PantryIntelSignal("needs_liquid", "Add a little liquid for more options.")
        )
        assertEquals("Add a little liquid for more options.", state.dashboardFacts(today).intelligenceMessage)
    }

    private fun state(
        inventory: List<PantryStockItem>,
        chips: List<String>,
        warning: PantryIntelSignal? = null
    ) = KitchenUiState(
        chips = chips,
        inventory = inventory,
        inventoryAdjustments = emptyMap(),
        shoppingList = emptyList(),
        pantryIntel = PantryIntelReport(
            readinessScore = 72,
            focusCategoryId = "vegetation",
            focusCategoryLabel = "Vegetables",
            categoryBreakdown = emptyList(),
            warnings = listOfNotNull(warning),
            tactics = emptyList(),
            equipmentLane = "stovetop"
        ),
        scannedIngredients = null,
        kitchenScanState = com.agentickitchen.android.KitchenScanState.Idle,
        shoppingImportState = com.agentickitchen.android.ShoppingImportState.Idle,
        recipeImportState = com.agentickitchen.android.RecipeImportState.Idle
    )

    private fun item(id: String, name: String, bestBefore: String? = null, useBy: String? = null) = PantryStockItem(
        id = id,
        originalName = name,
        quantity = 2.0,
        unit = "adet",
        unitDimension = UnitDimension.COUNT,
        source = "test",
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z",
        bestBefore = bestBefore,
        useBy = useBy
    )
}
