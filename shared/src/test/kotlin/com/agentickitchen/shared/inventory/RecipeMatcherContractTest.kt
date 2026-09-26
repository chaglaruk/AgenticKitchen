package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Contract tests for the local pantry matching authority:
 * tiers, shortages, and preparability are computed ONLY from proposedIngredients against the
 * real pantry. The model-declared missingIngredients field is not part of RecipeMatchCandidate
 * at all, so it can never influence the outcome; these tests pin that behavior.
 */
class RecipeMatcherContractTest {

    private val today = LocalDate.of(2026, 9, 26)

    private val pantry = listOf(
        stock("olive-oil", "zeytinyagi", 500.0, "ml"),
        stock("yogurt", "yogurt", 500.0, "g"),
        stock("milk", "sut", 1000.0, "ml"),
        stock("egg", "yumurta", 4.0, "adet"),
        stock("bread", "ekmek", 400.0, "g")
    )

    @Test
    fun `matching depends only on proposedIngredients and pantry, never on model advice`() {
        // RecipeMatchCandidate has no missingIngredients field: the model's advisory list
        // structurally cannot affect tier, shortages, or preparability.
        val fullyCovered = RecipeMatchCandidate(
            id = "covered",
            proposedIngredients = listOf(
                PlannedIngredientDto("Zeytinyagi", 50.0, "ml"),
                PlannedIngredientDto("Yumurta", 2.0, "adet")
            )
        )

        val result = RecipeMatcher.rank(listOf(fullyCovered), pantry, today = today).single()

        assertEquals(RecipeMatchTier.READY_NOW, result.tier)
        assertTrue(result.shortages.isEmpty())
        assertTrue(RecipeMatcher.canPrepareFromPantry(result))
    }

    @Test
    fun `incompatible unit dimension counts as a local shortage`() {
        // A slice requirement against a gram-based pantry line cannot be compared
        // deterministically and must surface as a shortage, not a silent match.
        val candidate = RecipeMatchCandidate(
            id = "slice-vs-gram",
            proposedIngredients = listOf(PlannedIngredientDto("Ekmek", 4.0, "dilim"))
        )

        val result = RecipeMatcher.rank(listOf(candidate), pantry, today = today).single()

        assertEquals(RecipeMatchTier.MISSING_ONE, result.tier)
        assertEquals(listOf("Ekmek"), result.shortages)
        assertFalse(RecipeMatcher.canPrepareFromPantry(result).not())
    }

    @Test
    fun `insufficient pantry quantity counts as a local shortage`() {
        val candidate = RecipeMatchCandidate(
            id = "butter-short",
            proposedIngredients = listOf(
                PlannedIngredientDto("Yumurta", 2.0, "adet"),
                PlannedIngredientDto("Tereyagi", 50.0, "g")
            )
        )

        val result = RecipeMatcher.rank(listOf(candidate), pantry, today = today).single()

        assertEquals(RecipeMatchTier.MISSING_ONE, result.tier)
        assertEquals(listOf("Tereyagi"), result.shortages)
    }

    @Test
    fun `empty proposedIngredients is never pantry-preparable`() {
        val empty = RecipeMatchCandidate(id = "empty", proposedIngredients = emptyList())

        val result = RecipeMatcher.rank(listOf(empty), pantry, today = today).single()

        assertEquals(RecipeMatchTier.AI_IDEA, result.tier)
        assertTrue(result.shortages.isEmpty())
        assertEquals(0, result.pantryCoveragePercent)
        assertFalse(RecipeMatcher.canPrepareFromPantry(result))
        assertFalse(RecipeMatcher.shouldSurface(result, strictStock = true, maxMissingStaples = 0))
    }

    private fun stock(
        id: String,
        name: String,
        quantity: Double,
        unit: String
    ) = PantryStockItem(
        id = id,
        canonicalIngredientId = null,
        originalName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = InventoryUnits.normalize(quantity, unit).dimension,
        source = "test",
        createdAt = "2026-09-01T00:00:00Z",
        updatedAt = "2026-09-01T00:00:00Z"
    )
}
