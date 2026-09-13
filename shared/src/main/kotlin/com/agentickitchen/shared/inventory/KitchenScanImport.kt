package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ShoppingCandidate

data class LocatedShoppingCandidate(
    val candidate: ShoppingCandidate,
    val location: PantryLocation
)

data class KitchenScanImportPlan(
    val mutations: List<InventoryMutation>,
    val conflicts: List<String>
)

object KitchenScanImportPlanner {
    fun plan(
        existing: List<PantryStockItem>,
        candidates: List<LocatedShoppingCandidate>,
        timestamp: String,
        idFactory: () -> String
    ): KitchenScanImportPlan {
        val locationConflicts = mutableListOf<String>()

        candidates.forEachIndexed { index, located ->
            val sameKnownItem = existing.firstOrNull { item ->
                LocalIngredientResolver.matches(
                    item.originalName,
                    item.canonicalIngredientId,
                    located.candidate.displayName,
                    located.candidate.canonicalIngredientId
                )
            }
            if (sameKnownItem != null && sameKnownItem.location != located.location) {
                locationConflicts += located.candidate.displayName
            }
            candidates.drop(index + 1).forEach { other ->
                if (
                    located.location != other.location &&
                    LocalIngredientResolver.matches(
                        located.candidate.displayName,
                        located.candidate.canonicalIngredientId,
                        other.candidate.displayName,
                        other.candidate.canonicalIngredientId
                    )
                ) {
                    locationConflicts += located.candidate.displayName
                }
            }
        }

        if (locationConflicts.isNotEmpty()) {
            return KitchenScanImportPlan(emptyList(), locationConflicts.distinct())
        }

        val base = InventoryWorkflow.planImport(
            existing = existing,
            candidates = candidates.map(LocatedShoppingCandidate::candidate),
            mode = ShoppingImportMode.ADD,
            timestamp = timestamp,
            idFactory = idFactory
        )
        if (base.conflicts.isNotEmpty()) {
            return KitchenScanImportPlan(emptyList(), base.conflicts)
        }

        val mutations = base.mutations.map { mutation ->
            val located = candidates.firstOrNull { input ->
                LocalIngredientResolver.matches(
                    mutation.item.originalName,
                    mutation.item.canonicalIngredientId,
                    input.candidate.displayName,
                    input.candidate.canonicalIngredientId
                )
            }
            if (located == null) {
                mutation
            } else {
                mutation.copy(
                    item = mutation.item.copy(
                        location = located.location,
                        source = "kitchen_scan"
                    ),
                    adjustment = mutation.adjustment.copy(
                        reason = AdjustmentReason.KITCHEN_SCAN,
                        source = "kitchen_scan"
                    )
                )
            }
        }
        return KitchenScanImportPlan(mutations, emptyList())
    }
}
