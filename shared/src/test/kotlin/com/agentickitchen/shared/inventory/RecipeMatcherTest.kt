package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeMatcherTest {
    private val today = LocalDate.of(2026, 8, 30)

    @Test
    fun `classifies ready missing one missing two and ai idea locally`() {
        val inventory = listOf(
            stock("tomato", "Domates", 500.0, "g"),
            stock("onion", "Soğan", 2.0, "adet")
        )
        val candidates = listOf(
            candidate("ready", ingredient("Domates", 100.0, "g", "tomato")),
            candidate(
                "missing-one",
                ingredient("Domates", 100.0, "g", "tomato"),
                ingredient("Pirinç", 75.0, "g", "rice")
            ),
            candidate(
                "missing-two",
                ingredient("Domates", 100.0, "g", "tomato"),
                ingredient("Pirinç", 75.0, "g", "rice"),
                ingredient("Mantar", 100.0, "g", "mushroom")
            ),
            candidate(
                "ai-idea",
                ingredient("Pirinç", 75.0, "g", "rice"),
                ingredient("Mantar", 100.0, "g", "mushroom"),
                ingredient("Ispanak", 50.0, "g", "spinach")
            )
        )

        val result = RecipeMatcher.rank(candidates, inventory, today = today)

        assertEquals(
            listOf(
                RecipeMatchTier.READY_NOW,
                RecipeMatchTier.MISSING_ONE,
                RecipeMatchTier.MISSING_TWO,
                RecipeMatchTier.AI_IDEA
            ),
            result.map(RecipeMatchResult::tier)
        )
        assertEquals(100, result[0].pantryCoveragePercent)
        assertEquals(listOf("Pirinç"), result[1].shortages)
    }

    @Test
    fun `ready recipes using expiring stock rank before equally covered fresh recipes`() {
        val inventory = listOf(
            stock("tomato", "Domates", 500.0, "g", useBy = "2026-08-30"),
            stock("rice", "Pirinç", 500.0, "g", bestBefore = "2026-10-10")
        )
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate("fresh", ingredient("Pirinç", 75.0, "g", "rice")),
                candidate("expires", ingredient("Domates", 100.0, "g", "tomato"))
            ),
            inventory = inventory,
            today = today
        )

        assertEquals("expires", result.first().candidateId)
        assertEquals(1, result.first().expiresTodayMatches)
    }

    @Test
    fun `expiry priority remains ahead of ready time priority`() {
        val inventory = listOf(
            stock("tomato", "Domates", 500.0, "g", useBy = "2026-08-30"),
            stock("rice", "Pirinç", 500.0, "g")
        )
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate("fast", ingredient("Pirinç", 75.0, "g", "rice")).copy(estimatedMinutes = 15),
                candidate("expiring", ingredient("Domates", 100.0, "g", "tomato")).copy(estimatedMinutes = 30)
            ),
            inventory = inventory,
            requestedReadyMinutes = 20,
            today = today
        )

        assertEquals("expiring", result.first().candidateId)
        assertEquals(10, result.first().readyTimePenaltyMinutes)
    }

    @Test
    fun `requested ready time penalizes only recipes that miss the target`() {
        val inventory = listOf(stock("tomato", "Domates", 500.0, "g"))
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate("late", ingredient("Domates", 100.0, "g", "tomato")).copy(estimatedMinutes = 30),
                candidate("fits", ingredient("Domates", 100.0, "g", "tomato")).copy(estimatedMinutes = 15)
            ),
            inventory = inventory,
            requestedReadyMinutes = 20,
            today = today
        )

        assertEquals("fits", result.first().candidateId)
        assertTrue(result.first().canMeetRequestedReadyTime)
        assertEquals(10, result.last().readyTimePenaltyMinutes)
        assertFalse(result.last().canMeetRequestedReadyTime)
    }

    @Test
    fun `important missing ingredient ranks after equally sized unimportant shortage`() {
        val inventory = listOf(stock("tomato", "Domates", 500.0, "g"))
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate(
                    "missing-priority",
                    ingredient("Domates", 100.0, "g", "tomato"),
                    ingredient("Ispanak", 100.0, "g", "spinach")
                ),
                candidate(
                    "missing-other",
                    ingredient("Domates", 100.0, "g", "tomato"),
                    ingredient("Pirinç", 100.0, "g", "rice")
                )
            ),
            inventory = inventory,
            prioritizedIngredients = listOf("Ispanak"),
            today = today
        )

        assertEquals("missing-other", result.first().candidateId)
        assertEquals(0, result.first().importantShortageCount)
        assertEquals(1, result.last().importantShortageCount)
    }

    @Test
    fun `reserved stock participates in shortage classification`() {
        val tomato = stock("tomato", "Domates", 100.0, "g")
        val result = RecipeMatcher.rank(
            candidates = listOf(candidate("tomato", ingredient("Domates", 60.0, "g", "tomato"))),
            inventory = listOf(tomato),
            reservedByItem = mapOf(tomato.id to 50.0),
            today = today
        )

        assertEquals(RecipeMatchTier.MISSING_ONE, result.single().tier)
        assertEquals(0, result.single().pantryCoveragePercent)
    }

    @Test
    fun `unsafe or diet rejected candidates are fail closed`() {
        val inventory = listOf(stock("tomato", "Domates", 500.0, "g"))
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate("safe", ingredient("Domates", 100.0, "g", "tomato")),
                candidate("unsafe", ingredient("Domates", 100.0, "g", "tomato")).copy(safetyAllowed = false),
                candidate("diet", ingredient("Domates", 100.0, "g", "tomato")).copy(dietAllowed = false)
            ),
            inventory = inventory,
            today = today
        )

        assertEquals(listOf("safe"), result.map(RecipeMatchResult::candidateId))
    }

    @Test
    fun `constraint policy rejects allergen and diet conflicts locally`() {
        val peanutDish = listOf(ingredient("Yer fıstığı", 30.0, "g", "peanut"))
        val chickenDish = listOf(ingredient("Tavuk", 200.0, "g", "chicken"))

        assertFalse(RecipeMatchConstraintPolicy.safetyAllowed(peanutDish, setOf("peanut")))
        assertTrue(RecipeMatchConstraintPolicy.safetyAllowed(peanutDish, emptySet()))
        assertFalse(RecipeMatchConstraintPolicy.dietAllowed(chickenDish, "vegetarian"))
        assertTrue(RecipeMatchConstraintPolicy.dietAllowed(chickenDish, "none"))
    }

    @Test
    fun `constraint policy fails closed when structured ingredients are absent under active constraints`() {
        assertFalse(RecipeMatchConstraintPolicy.safetyAllowed(emptyList(), setOf("peanut")))
        assertFalse(RecipeMatchConstraintPolicy.dietAllowed(emptyList(), "vegan"))
        assertTrue(RecipeMatchConstraintPolicy.safetyAllowed(emptyList(), emptySet()))
        assertTrue(RecipeMatchConstraintPolicy.dietAllowed(emptyList(), "none"))
    }

    @Test
    fun `surface policy keeps ai ideas visible but never pantry preparable`() {
        val aiIdea = RecipeMatchResult(
            candidateId = "idea",
            tier = RecipeMatchTier.AI_IDEA,
            shortages = listOf("a", "b", "c"),
            pantryCoveragePercent = 25,
            expiresTodayMatches = 0,
            useSoonMatches = 0,
            importantShortageCount = 0,
            readyTimePenaltyMinutes = 0,
            equipmentFit = true,
            estimatedMinutes = 20,
            previouslySuccessful = false,
            priorityMatchCount = 0
        )
        val missingTwo = aiIdea.copy(
            candidateId = "missing-two",
            tier = RecipeMatchTier.MISSING_TWO,
            shortages = listOf("a", "b")
        )

        assertTrue(RecipeMatcher.shouldSurface(aiIdea, strictStock = false, maxMissingStaples = 0))
        assertFalse(RecipeMatcher.shouldSurface(aiIdea, strictStock = true, maxMissingStaples = 2))
        assertFalse(RecipeMatcher.canPrepareFromPantry(aiIdea))
        assertFalse(RecipeMatcher.shouldSurface(missingTwo, strictStock = false, maxMissingStaples = 1))
        assertTrue(RecipeMatcher.shouldSurface(missingTwo, strictStock = false, maxMissingStaples = 2))
        assertTrue(RecipeMatcher.canPrepareFromPantry(missingTwo))
    }

    @Test
    fun `equipment history and explicit priority provide stable local tie breakers`() {
        val inventory = listOf(
            stock("tomato", "Domates", 500.0, "g"),
            stock("onion", "Soğan", 5.0, "adet")
        )
        val baseIngredient = ingredient("Domates", 100.0, "g", "tomato")
        val result = RecipeMatcher.rank(
            candidates = listOf(
                candidate("history", baseIngredient).copy(previouslySuccessful = true, estimatedMinutes = 20),
                candidate("priority", baseIngredient).copy(estimatedMinutes = 20),
                candidate("no-equipment", baseIngredient).copy(estimatedMinutes = 20, requiredEquipment = setOf("oven"))
            ),
            inventory = inventory,
            availableEquipment = setOf("elec"),
            prioritizedIngredients = listOf("Domates"),
            today = today
        )

        assertEquals("history", result.first().candidateId)
        assertTrue(result.first().equipmentFit)
        assertTrue(result.first().previouslySuccessful)
        assertEquals("priority", result[1].candidateId)
        assertEquals("no-equipment", result[2].candidateId)
    }

    @Test
    fun `empty proposed ingredient candidate is an ai idea`() {
        val result = RecipeMatcher.rank(
            candidates = listOf(RecipeMatchCandidate("idea", emptyList())),
            inventory = emptyList(),
            today = today
        ).single()

        assertEquals(RecipeMatchTier.AI_IDEA, result.tier)
        assertEquals(0, result.pantryCoveragePercent)
    }

    private fun candidate(id: String, vararg ingredients: PlannedIngredientDto) =
        RecipeMatchCandidate(id = id, proposedIngredients = ingredients.toList(), estimatedMinutes = 20)

    private fun ingredient(name: String, quantity: Double, unit: String, canonicalId: String) =
        PlannedIngredientDto(name, quantity, unit, canonicalId)

    private fun stock(
        id: String,
        name: String,
        quantity: Double,
        unit: String,
        bestBefore: String? = null,
        useBy: String? = null
    ) = PantryStockItem(
        id = id,
        canonicalIngredientId = id,
        originalName = name,
        quantity = quantity,
        unit = unit,
        unitDimension = InventoryUnits.normalize(quantity, unit).dimension,
        source = "test",
        createdAt = "2026-08-01T00:00:00Z",
        updatedAt = "2026-08-01T00:00:00Z",
        bestBefore = bestBefore,
        useBy = useBy
    )
}
