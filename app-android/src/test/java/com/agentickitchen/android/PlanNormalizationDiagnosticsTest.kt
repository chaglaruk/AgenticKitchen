package com.agentickitchen.android

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.validator.ExclusiveResourceSequencer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Release-safe sanitization tests for the exclusive-resource normalization diagnostics. */
class PlanNormalizationDiagnosticsTest {

    private fun outcome(
        result: ExclusiveResourceSequencer.Result,
        edges: Int,
        stove: Int = 0,
        oven: Int = 0,
        airfryer: Int = 0
    ) = ExclusiveResourceSequencer.Outcome(
        plan = CookingPlanResponse("hidden-name", 2, emptyList(), emptyList(), emptyList()),
        result = result,
        edgesAdded = edges,
        stoveEdges = stove,
        ovenEdges = oven,
        airfryerEdges = airfryer
    )

    @Test
    fun `unchanged outcome reports zero edges`() {
        val line = PlanNormalizationDiagnostics.buildLine(
            outcome(ExclusiveResourceSequencer.Result.UNCHANGED, 0)
        )
        assertEquals("exclusiveEdgesAdded=0 result=UNCHANGED stoveEdges=0 ovenEdges=0 airfryerEdges=0", line)
    }

    @Test
    fun `normalized outcome reports per resource edge counts`() {
        val line = PlanNormalizationDiagnostics.buildLine(
            outcome(ExclusiveResourceSequencer.Result.NORMALIZED, 2, stove = 2)
        )
        assertEquals("exclusiveEdgesAdded=2 result=NORMALIZED stoveEdges=2 ovenEdges=0 airfryerEdges=0", line)
    }

    @Test
    fun `skipped invalid graph reports warn-worthy outcome with zero edges`() {
        val line = PlanNormalizationDiagnostics.buildLine(
            outcome(ExclusiveResourceSequencer.Result.SKIPPED_INVALID_GRAPH, 0)
        )
        assertEquals("exclusiveEdgesAdded=0 result=SKIPPED_INVALID_GRAPH stoveEdges=0 ovenEdges=0 airfryerEdges=0", line)
    }

    @Test
    fun `no step or recipe names can leak`() {
        val line = PlanNormalizationDiagnostics.buildLine(
            outcome(ExclusiveResourceSequencer.Result.NORMALIZED, 3, stove = 3)
        )
        assertFalse(line.contains("hidden-name"))
        assertFalse(line.contains("s1"))
        assertFalse(line.contains("stove use"))
        assertTrue(line.contains("exclusiveEdgesAdded=3"))
        assertFalse(line.contains('\n'))
    }

    @Test
    fun `log writes through the emit seam`() {
        val captured = mutableListOf<Pair<Int, String>>()
        val previous = PlanNormalizationDiagnostics.emit
        PlanNormalizationDiagnostics.emit = { priority, message -> captured.add(priority to message) }
        try {
            PlanNormalizationDiagnostics.log(outcome(ExclusiveResourceSequencer.Result.NORMALIZED, 1, oven = 1))
        } finally {
            PlanNormalizationDiagnostics.emit = previous
        }
        assertEquals(1, captured.size)
        assertTrue(captured[0].second.contains("result=NORMALIZED"))
    }
}
