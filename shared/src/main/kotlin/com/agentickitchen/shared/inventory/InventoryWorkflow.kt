package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.util.Locale

enum class ShoppingImportMode { ADD, RECOUNT }

data class InventoryImportPlan(
    val mutations: List<InventoryMutation>,
    val conflicts: List<String>
)

@Serializable
data class PlannedPantryUsage(
    val itemId: String,
    val itemName: String,
    val currentQuantity: Double,
    val plannedQuantity: Double,
    val remainingQuantity: Double,
    val unit: String
)

data class InventoryUsagePlan(
    val usages: List<PlannedPantryUsage>,
    val shortages: List<String>
)

object InventoryWorkflow {
    fun planImport(
        existing: List<PantryStockItem>,
        candidates: List<ShoppingCandidate>,
        mode: ShoppingImportMode,
        timestamp: String,
        idFactory: () -> String
    ): InventoryImportPlan {
        val working = existing.associateBy(PantryStockItem::id).toMutableMap()
        val mutations = mutableListOf<InventoryMutation>()
        val conflicts = mutableListOf<String>()

        candidates.forEach { candidate ->
            val amount = runCatching { candidate.normalizedAmount() }.getOrElse {
                conflicts += candidate.displayName
                return@forEach
            }
            val match = working.values.firstOrNull { item -> item.matches(candidate) }
            if (match != null) {
                val current = InventoryUnits.normalize(match.quantity, match.unit)
                if (!compatible(current, amount) || !compatiblePackages(match, candidate, amount)) {
                    conflicts += candidate.displayName
                    return@forEach
                }
                val quantity = if (mode == ShoppingImportMode.ADD) current.quantity + amount.quantity else amount.quantity
                val updated = match.copy(
                    originalName = candidate.displayName.trim(),
                    canonicalIngredientId = candidate.canonicalIngredientId ?: match.canonicalIngredientId,
                    quantity = quantity,
                    unit = amount.unit,
                    unitDimension = amount.dimension,
                    packageLabel = candidate.packageLabel ?: match.packageLabel,
                    isEstimated = candidate.estimated,
                    confidence = candidate.confidence,
                    source = if (mode == ShoppingImportMode.ADD) "shopping" else "recount",
                    updatedAt = timestamp
                )
                working[updated.id] = updated
                mutations += mutation(updated, amount.quantity, mode, timestamp, idFactory)
            } else {
                val id = idFactory()
                val item = PantryStockItem(
                    id = id,
                    canonicalIngredientId = candidate.canonicalIngredientId,
                    originalName = candidate.displayName.trim(),
                    quantity = amount.quantity,
                    unit = amount.unit,
                    unitDimension = amount.dimension,
                    packageLabel = candidate.packageLabel,
                    isEstimated = candidate.estimated,
                    confidence = candidate.confidence,
                    source = if (mode == ShoppingImportMode.ADD) "shopping" else "recount",
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                working[id] = item
                mutations += mutation(item, amount.quantity, mode, timestamp, idFactory)
            }
        }
        return InventoryImportPlan(mutations, conflicts.distinct())
    }

    fun planUsage(
        plan: CookingPlanResponse,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double> = emptyMap()
    ): InventoryUsagePlan {
        val usages = mutableListOf<PlannedPantryUsage>()
        val shortages = mutableListOf<String>()
        plan.ingredients.forEach { ingredient ->
            val resolvedIngredientCanonicalId = ingredient.canonicalIngredientId
                ?: LocalIngredientResolver.resolveCanonicalId(ingredient.name)

            val item = inventory.firstOrNull { stockItem ->
                LocalIngredientResolver.matches(
                    firstName = stockItem.originalName,
                    firstCanonicalId = stockItem.canonicalIngredientId,
                    secondName = ingredient.name,
                    secondCanonicalId = resolvedIngredientCanonicalId
                )
            }
            if (item == null) {
                shortages += ingredient.name
                return@forEach
            }
            val needed = runCatching { InventoryUnits.normalize(ingredient.quantity, ingredient.unit) }.getOrNull()
            val current = runCatching { InventoryUnits.normalize(item.quantity, item.unit) }.getOrNull()
            if (needed == null || current == null || !compatible(needed, current)) {
                shortages += ingredient.name
                return@forEach
            }
            val available = current.quantity - reservedByItem.getOrDefault(item.id, 0.0)
            if (needed.quantity > available + 0.000_001) {
                shortages += ingredient.name
            } else {
                usages += PlannedPantryUsage(
                    itemId = item.id,
                    itemName = item.originalName,
                    currentQuantity = current.quantity,
                    plannedQuantity = needed.quantity,
                    remainingQuantity = available - needed.quantity,
                    unit = current.unit
                )
            }
        }
        return InventoryUsagePlan(usages, shortages.distinct())
    }

    private fun mutation(
        item: PantryStockItem,
        amount: Double,
        mode: ShoppingImportMode,
        timestamp: String,
        idFactory: () -> String
    ) = InventoryMutation(
        item,
        InventoryAdjustmentRecord(
            id = idFactory(),
            itemId = item.id,
            amount = amount,
            mode = if (mode == ShoppingImportMode.ADD) AdjustmentMode.DELTA else AdjustmentMode.REPLACE,
            reason = if (mode == ShoppingImportMode.ADD) AdjustmentReason.SHOPPING_ADD else AdjustmentReason.RECOUNT,
            source = if (mode == ShoppingImportMode.ADD) "shopping" else "recount",
            timestamp = timestamp
        )
    )

    private fun ShoppingCandidate.normalizedAmount(): NormalizedAmount {
        val packageWeight = packageLabel?.let(::visiblePackageAmount)
        return if (
            unitDimension.equals("package", ignoreCase = true) &&
            quantity != null &&
            packageWeight != null
        ) {
            packageWeight.copy(quantity = packageWeight.quantity * quantity)
        } else {
            InventoryUnits.normalize(
                requireNotNull(quantity) { "Quantity required" },
                requireNotNull(unit) { "Unit required" }
            )
        }
    }

    private fun visiblePackageAmount(label: String): NormalizedAmount? {
        val match = Regex("""(\d+(?:[.,]\d+)?)\s*(kg|g|l|ml)\b""", RegexOption.IGNORE_CASE).find(label)
            ?: return null
        return runCatching {
            InventoryUnits.normalize(match.groupValues[1].replace(',', '.').toDouble(), match.groupValues[2])
        }.getOrNull()
    }

    private fun PantryStockItem.matches(candidate: ShoppingCandidate): Boolean =
        LocalIngredientResolver.matches(
            firstName = originalName,
            firstCanonicalId = canonicalIngredientId,
            secondName = candidate.displayName,
            secondCanonicalId = candidate.canonicalIngredientId
        )

    private fun compatible(first: NormalizedAmount, second: NormalizedAmount): Boolean =
        first.dimension == second.dimension && first.dimension != UnitDimension.UNKNOWN

    private fun compatiblePackages(
        item: PantryStockItem,
        candidate: ShoppingCandidate,
        amount: NormalizedAmount
    ): Boolean = amount.dimension != UnitDimension.PACKAGE ||
        item.packageLabel.isNullOrBlank() ||
        candidate.packageLabel.isNullOrBlank() ||
        item.packageLabel.normalized() == candidate.packageLabel.normalized()

    private fun String.normalized(): String = Normalizer.normalize(
        trim().lowercase(Locale.ROOT).replace('ı', 'i'),
        Normalizer.Form.NFD
    ).replace(Regex("""\p{Mn}+"""), "")
}
