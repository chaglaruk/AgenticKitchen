package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ShoppingCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KitchenScanImportPlannerTest {
    @Test fun sameLocationCandidateAddsToKnownStockAndPreservesLocation() {
        val existing = listOf(stock("milk", "Milk", 500.0, "ml", PantryLocation.FRIDGE))
        val plan = KitchenScanImportPlanner.plan(
            existing = existing,
            candidates = listOf(located("milk", "Milk", 250.0, "ml", PantryLocation.FRIDGE)),
            timestamp = "2026-08-30T12:00:00Z",
            idFactory = sequenceIds()
        )
        assertTrue(plan.conflicts.isEmpty())
        assertEquals(1, plan.mutations.size)
        assertEquals(750.0, plan.mutations.single().item.quantity)
        assertEquals(PantryLocation.FRIDGE, plan.mutations.single().item.location)
        assertEquals("kitchen_scan", plan.mutations.single().item.source)
        assertEquals(AdjustmentReason.KITCHEN_SCAN, plan.mutations.single().adjustment.reason)
    }

    @Test fun knownIngredientInDifferentLocationFailsClosed() {
        val plan = KitchenScanImportPlanner.plan(
            existing = listOf(stock("milk", "Milk", 500.0, "ml", PantryLocation.FRIDGE)),
            candidates = listOf(located("milk", "Milk", 250.0, "ml", PantryLocation.PANTRY)),
            timestamp = "2026-08-30T12:00:00Z",
            idFactory = sequenceIds()
        )
        assertTrue(plan.mutations.isEmpty())
        assertEquals(listOf("Milk"), plan.conflicts)
    }

    @Test fun sameIngredientAcrossTwoScannedLocationsFailsClosed() {
        val plan = KitchenScanImportPlanner.plan(
            existing = emptyList(),
            candidates = listOf(
                located("tomato", "Tomato", 2.0, "adet", PantryLocation.FRIDGE),
                located("tomato", "Tomato", 1.0, "adet", PantryLocation.COUNTER)
            ),
            timestamp = "2026-08-30T12:00:00Z",
            idFactory = sequenceIds()
        )
        assertTrue(plan.mutations.isEmpty())
        assertEquals(listOf("Tomato"), plan.conflicts)
    }

    @Test fun missingQuantityRemainsAReviewConflict() {
        val candidate = ShoppingCandidate(
            canonicalIngredientId = "onion",
            displayName = "Onion",
            quantity = null,
            unit = null,
            confidence = .72,
            estimated = true,
            uncertaintyReason = "Quantity not visible"
        )
        val plan = KitchenScanImportPlanner.plan(
            existing = emptyList(),
            candidates = listOf(LocatedShoppingCandidate(candidate, PantryLocation.PANTRY)),
            timestamp = "2026-08-30T12:00:00Z",
            idFactory = sequenceIds()
        )
        assertTrue(plan.mutations.isEmpty())
        assertEquals(listOf("Onion"), plan.conflicts)
    }

    private fun stock(id: String, name: String, quantity: Double, unit: String, location: PantryLocation) =
        PantryStockItem(
            id = id,
            canonicalIngredientId = id,
            originalName = name,
            quantity = quantity,
            unit = unit,
            unitDimension = InventoryUnits.normalize(quantity, unit).dimension,
            source = "manual",
            createdAt = "2026-08-30T10:00:00Z",
            updatedAt = "2026-08-30T10:00:00Z",
            location = location
        )

    private fun located(id: String, name: String, quantity: Double, unit: String, location: PantryLocation) =
        LocatedShoppingCandidate(
            ShoppingCandidate(
                canonicalIngredientId = id,
                displayName = name,
                quantity = quantity,
                unit = unit,
                unitDimension = InventoryUnits.normalize(quantity, unit).dimension.name.lowercase(),
                confidence = .95,
                estimated = false
            ),
            location
        )

    private fun sequenceIds(): () -> String {
        var next = 0
        return { "id-${next++}" }
    }
}
