package com.agentickitchen.shared.agents

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimplePantryIntelAgentTest {
    private val agent = SimplePantryIntelAgent()

    @Test
    fun `identifies seafood dominant pantry and assigns high readiness`() {
        val report = agent.analyze(
            ingredients = listOf("Salmon", "Rice", "Lemon", "Garlic", "Olive oil", "Water"),
            equipment = setOf("oven", "pan"),
            dietType = "none"
        )

        assertEquals("protein_aqua", report.focusCategoryId)
        assertEquals(1, report.categoryBreakdown.first { it.id == "protein_aqua" }.count)
        assertTrue(report.readinessScore >= 75)
        assertTrue(report.tactics.any { it.code == "protein_forward_plan" })
        assertTrue(report.warnings.none { it.code == "diet_conflict" })
    }

    @Test
    fun `flags vegan conflict and dry pantry gaps`() {
        val report = agent.analyze(
            ingredients = listOf("Chicken breast", "Potato", "Paprika"),
            equipment = setOf("oven"),
            dietType = "vegan"
        )

        assertTrue(report.warnings.any { it.code == "diet_conflict" })
        assertTrue(report.warnings.any { it.code == "needs_liquid" })
        assertTrue(report.warnings.any { it.code == "needs_aromatic" })
        assertTrue(report.readinessScore <= 55)
        assertTrue(report.tactics.any { it.code == "add_liquid_support" })
    }
}
