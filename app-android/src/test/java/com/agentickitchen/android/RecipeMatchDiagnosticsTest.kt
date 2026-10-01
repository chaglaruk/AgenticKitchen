package com.agentickitchen.android

import com.agentickitchen.shared.inventory.RecipeMatchResult
import com.agentickitchen.shared.inventory.RecipeMatchTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused tests for the release-visible local matcher diagnostics channel: deterministic
 * ordering, whitelisted metadata only, and no recipe/ingredient/pantry name leakage.
 */
class RecipeMatchDiagnosticsTest {

    private fun result(
        id: String,
        tier: RecipeMatchTier,
        shortageCount: Int,
        coverage: Int,
        equipmentFit: Boolean = true
    ) = RecipeMatchResult(
        candidateId = id,
        tier = tier,
        shortages = List(shortageCount) { "hidden-shortage-$it" },
        pantryCoveragePercent = coverage,
        expiresTodayMatches = 0,
        useSoonMatches = 0,
        importantShortageCount = 0,
        readyTimePenaltyMinutes = 0,
        equipmentFit = equipmentFit,
        estimatedMinutes = 20,
        previouslySuccessful = false,
        priorityMatchCount = 0
    )

    @Test
    fun `line contains only whitelisted metadata in deterministic order`() {
        val line = RecipeMatchDiagnostics.buildLine(
            index = 2,
            proposedCount = 3,
            result = result("o1", RecipeMatchTier.MISSING_ONE, 1, 67, equipmentFit = true)
        )
        assertEquals(
            "index=2 proposedCount=3 shortageCount=1 tier=MISSING_ONE coverage=67 canPrepare=true equipmentFit=true",
            line
        )
    }

    @Test
    fun `ai idea line reports canPrepare false and zero coverage`() {
        val line = RecipeMatchDiagnostics.buildLine(
            index = 3,
            proposedCount = 0,
            result = result("o2", RecipeMatchTier.AI_IDEA, 0, 0)
        )
        assertTrue(line.contains("tier=AI_IDEA"))
        assertTrue(line.contains("canPrepare=false"))
        assertTrue(line.contains("coverage=0"))
    }

    @Test
    fun `hidden shortage names never leak into the line`() {
        val line = RecipeMatchDiagnostics.buildLine(
            index = 1,
            proposedCount = 2,
            result = result("o3", RecipeMatchTier.MISSING_TWO, 2, 50)
        )
        assertFalse(line.contains("hidden-shortage"))
        assertFalse(line.contains("o3"))
    }

    @Test
    fun `logMatches emits rank ordered lines with proposed counts`() {
        val captured = mutableListOf<String>()
        val previous = RecipeMatchDiagnostics.emit
        RecipeMatchDiagnostics.emit = { _, message -> captured.add(message) }
        try {
            val ranked = listOf(
                result("b", RecipeMatchTier.MISSING_ONE, 1, 67),
                result("a", RecipeMatchTier.AI_IDEA, 0, 0)
            )
            RecipeMatchDiagnostics.logMatches(ranked, mapOf("b" to 3, "a" to 0))
        } finally {
            RecipeMatchDiagnostics.emit = previous
        }
        assertEquals(2, captured.size)
        assertTrue(captured[0].startsWith("index=1 proposedCount=3"))
        assertTrue(captured[1].startsWith("index=2 proposedCount=0"))
    }

    @Test
    fun `tier names survive sanitization unchanged`() {
        val line = RecipeMatchDiagnostics.buildLine(
            index = 1,
            proposedCount = 1,
            result = result("x", RecipeMatchTier.READY_NOW, 0, 100)
        )
        assertTrue(line.contains("tier=READY_NOW"))
        assertFalse(line.contains("\n"))
        assertFalse(line.contains("\t"))
        assertTrue(line.endsWith("equipmentFit=true"))
    }
}
