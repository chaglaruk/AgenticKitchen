package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.validator.IngredientSafety
import java.time.LocalDate
import java.util.Locale

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
    val importantShortageCount: Int,
    val readyTimePenaltyMinutes: Int,
    val equipmentFit: Boolean,
    val estimatedMinutes: Int,
    val previouslySuccessful: Boolean,
    val priorityMatchCount: Int
) {
    val canMeetRequestedReadyTime: Boolean get() = readyTimePenaltyMinutes == 0
}

/**
 * Performs the deterministic option-level checks that can be answered from structured ingredient
 * data. Detailed cooking-step food safety remains enforced later by CookingPlanValidator.
 */
object RecipeMatchConstraintPolicy {
    fun safetyAllowed(
        ingredients: List<PlannedIngredientDto>,
        allergies: Set<String>
    ): Boolean {
        if (allergies.isEmpty()) return true
        if (ingredients.isEmpty()) return false
        return ingredients.none { ingredient ->
            allergies.any { allergen -> IngredientSafety.conflictsWithAllergen(ingredient.name, allergen) }
        }
    }

    fun dietAllowed(
        ingredients: List<PlannedIngredientDto>,
        dietType: String
    ): Boolean {
        val normalizedDiet = dietType.trim().lowercase(Locale.ROOT)
        if (normalizedDiet.isBlank() || normalizedDiet == "none") return true
        if (ingredients.isEmpty()) return false
        return ingredients.none { ingredient -> IngredientSafety.conflictsWithDiet(ingredient.name, normalizedDiet) }
    }
}

/**
 * Deterministic pantry comparison/ranking. No provider or network call is made here.
 *
 * Ordering follows the product priority: constraints first, pantry coverage, expiring stock,
 * shortage count/importance, requested ready time, equipment, previous success, then local
 * preference/history signals. Duration and id are only stable final tie-breakers.
 */
object RecipeMatcher {
    fun rank(
        candidates: List<RecipeMatchCandidate>,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double> = emptyMap(),
        availableEquipment: Set<String> = emptySet(),
        prioritizedIngredients: List<String> = emptyList(),
        requestedReadyMinutes: Int? = null,
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
                requestedReadyMinutes = requestedReadyMinutes,
                today = today
            )
        }
        .sortedWith(
            compareBy<RecipeMatchResult> { it.tier.order }
                .thenByDescending { it.pantryCoveragePercent }
                .thenByDescending { it.expiresTodayMatches }
                .thenByDescending { it.useSoonMatches }
                .thenBy { it.shortages.size }
                .thenBy { it.importantShortageCount }
                .thenBy { it.readyTimePenaltyMinutes }
                .thenByDescending { it.equipmentFit }
                .thenByDescending { it.previouslySuccessful }
                .thenByDescending { it.priorityMatchCount }
                .thenBy { it.estimatedMinutes }
                .thenBy { it.candidateId }
        )
        .toList()

    /**
     * Pantry matches obey the user's missing-item allowance. AI ideas remain visible in non-strict
     * mode as inspiration, but callers must not treat an AI_IDEA as pantry-preparable.
     */
    fun shouldSurface(
        result: RecipeMatchResult,
        strictStock: Boolean,
        maxMissingStaples: Int
    ): Boolean {
        if (strictStock) return result.tier == RecipeMatchTier.READY_NOW
        return when (result.tier) {
            RecipeMatchTier.READY_NOW -> true
            RecipeMatchTier.MISSING_ONE -> maxMissingStaples >= 1
            RecipeMatchTier.MISSING_TWO -> maxMissingStaples >= 2
            RecipeMatchTier.AI_IDEA -> true
        }
    }

    fun canPrepareFromPantry(result: RecipeMatchResult): Boolean = result.tier != RecipeMatchTier.AI_IDEA

    private fun evaluate(
        candidate: RecipeMatchCandidate,
        inventory: List<PantryStockItem>,
        reservedByItem: Map<String, Double>,
        availableEquipment: Set<String>,
        prioritizedIngredients: List<String>,
        requestedReadyMinutes: Int?,
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
                importantShortageCount = 0,
                readyTimePenaltyMinutes = readyTimePenalty(candidate.estimatedMinutes, requestedReadyMinutes),
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
        val importantShortages = prioritizedIngredients.count { priority ->
            usage.shortages.any { shortage -> ingredientNamesMatch(priority, shortage) }
        }

        return RecipeMatchResult(
            candidateId = candidate.id,
            tier = tier,
            shortages = usage.shortages,
            pantryCoveragePercent = coverage,
            expiresTodayMatches = freshness.count { it == PantryFreshnessStatus.EXPIRES_TODAY },
            useSoonMatches = freshness.count { it == PantryFreshnessStatus.USE_SOON },
            importantShortageCount = importantShortages,
            readyTimePenaltyMinutes = readyTimePenalty(candidate.estimatedMinutes, requestedReadyMinutes),
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

    private fun readyTimePenalty(estimatedMinutes: Int, requestedReadyMinutes: Int?): Int {
        if (requestedReadyMinutes == null) return 0
        return (estimatedMinutes.coerceAtLeast(0) - requestedReadyMinutes.coerceAtLeast(0)).coerceAtLeast(0)
    }

    private fun ingredientNamesMatch(first: String, second: String): Boolean =
        LocalIngredientResolver.matches(first, null, second, null)

    private fun equipmentFits(candidate: RecipeMatchCandidate, availableEquipment: Set<String>): Boolean =
        candidate.requiredEquipment.all(availableEquipment::contains)
}
