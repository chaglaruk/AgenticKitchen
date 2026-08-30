package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import java.time.LocalDate

/** Local-only pantry matching buckets used before presentation. */
enum class RecipeMatchTier(val order: Int) {
    READY_NOW(0),
    MISSING_ONE(1),
    MISSING_TWO(2),
    AI_IDEA(3)
}

data class RecipeMatchCandidate(
    val id: String,
    val proposedIngredients: List<PlannedIngredientDto>,
    val requiredEquipment: Set<String> = emptySet(),
    val estimatedMinutes: Int = Int.MAX_VALUE,
    val safetyAllowed: Boolean = true,
    val dietAllowed: Boolean = true,
    val previouslySuccessful: Boolean = false
)

data class RecipeMatchResult(
    val candidateId: String,
    val tier: RecipeMatchTier,
    val shortages: List<String>,
    val pantryCoveragePercent: Int,
    val expiresTodayMatches: Int,
    val useSoonMatches: Int,
    val equipmentFit: Boolean,
    val estimatedMinutes: Int,
    val previouslySuccessful: Boolean,
    val priorityMatchCount: Int
)

/**
 * Deterministic pantry comparison/ranking. No provider or network call is made here.
 *
 * Safety and diet are fail-closed inputs: candidates explicitly rejected by either gate are
 * removed before ranking. Current callers only set them true after their existing constrained
 * recipe-generation/validation path has accepted the option.
 */
object RecipeMatcher {
    fun rank(
        candidates: List<RecipeMatchCandidate>,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double> = emptyMap(),
        availableEquipment: Set<String> = emptySet(),
        prioritizedIngredients: List<String> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): List<RecipeMatchResult> = candidates
        .asSequence()
        .filter { it.safetyAllowed && it.dietAllowed }
        .map { candidate ->
            evaluate(
                candidate = candidate,
                inventory = inventory,
                reservedByItem = reservedByItem,
                availableEquipment = availableEquipment,
                prioritizedIngredients = prioritizedIngredients,
                today = today
            )
        }
        .sortedWith(
            compareBy<RecipeMatchResult> { it.tier.order }
                .thenByDescending { it.pantryCoveragePercent }
                .thenByDescending { it.expiresTodayMatches }
                .thenByDescending { it.useSoonMatches }
                .thenBy { it.shortages.size }
                .thenBy { it.estimatedMinutes }
                .thenByDescending { it.equipmentFit }
                .thenByDescending { it.previouslySuccessful }
                .thenByDescending { it.priorityMatchCount }
                .thenBy { it.candidateId }
        )
        .toList()

    private fun evaluate(
        candidate: RecipeMatchCandidate,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double>,
        availableEquipment: Set<String>,
        prioritizedIngredients: List<String>,
        today: LocalDate
    ): RecipeMatchResult {
        if (candidate.proposedIngredients.isEmpty()) {
            return RecipeMatchResult(
                candidateId = candidate.id,
                tier = RecipeMatchTier.AI_IDEA,
                shortages = emptyList(),
                pantryCoveragePercent = 0,
                expiresTodayMatches = 0,
                useSoonMatches = 0,
                equipmentFit = equipmentFits(candidate, availableEquipment),
                estimatedMinutes = candidate.estimatedMinutes,
                previouslySuccessful = candidate.previouslySuccessful,
                priorityMatchCount = 0
            )
        }

        val usage = InventoryWorkflow.planUsage(
            plan = CookingPlanResponse(
                recipeName = candidate.id,
                servings = 1,
                ingredients = candidate.proposedIngredients,
                steps = emptyList(),
                safetyNotes = emptyList()
            ),
            inventory = inventory,
            reservedByItem = reservedByItem
        )
        val shortageCount = usage.shortages.size
        val tier = when (shortageCount) {
            0 -> RecipeMatchTier.READY_NOW
            1 -> RecipeMatchTier.MISSING_ONE
            2 -> RecipeMatchTier.MISSING_TWO
            else -> RecipeMatchTier.AI_IDEA
        }
        val totalIngredients = candidate.proposedIngredients.size.coerceAtLeast(1)
        val matchedIngredients = (totalIngredients - shortageCount).coerceIn(0, totalIngredients)
        val coverage = ((matchedIngredients * 100.0) / totalIngredients).toInt()
        val usedItemIds = usage.usages.mapTo(hashSetOf(), PlannedPantryUsage::itemId)
        val freshness = inventory
            .asSequence()
            .filter { it.id in usedItemIds }
            .map { PantryFreshnessPolicy.evaluate(it, today).status }
            .toList()

        return RecipeMatchResult(
            candidateId = candidate.id,
            tier = tier,
            shortages = usage.shortages,
            pantryCoveragePercent = coverage,
            expiresTodayMatches = freshness.count { it == PantryFreshnessStatus.EXPIRES_TODAY },
            useSoonMatches = freshness.count { it == PantryFreshnessStatus.USE_SOON },
            equipmentFit = equipmentFits(candidate, availableEquipment),
            estimatedMinutes = candidate.estimatedMinutes,
            previouslySuccessful = candidate.previouslySuccessful,
            priorityMatchCount = prioritizedIngredients.count { priority ->
                candidate.proposedIngredients.any { ingredient ->
                    LocalIngredientResolver.matches(
                        firstName = priority,
                        firstCanonicalId = null,
                        secondName = ingredient.name,
                        secondCanonicalId = ingredient.canonicalIngredientId
                    )
                }
            }
        )
    }

    private fun equipmentFits(candidate: RecipeMatchCandidate, availableEquipment: Set<String>): Boolean =
        candidate.requiredEquipment.all(availableEquipment::contains)
}
