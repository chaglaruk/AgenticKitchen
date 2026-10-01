package com.agentickitchen.android

import com.agentickitchen.shared.validator.ErrorType
import com.agentickitchen.shared.validator.ValidationError
import com.agentickitchen.shared.validator.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused tests for the release-visible Cooking Plan validation diagnostics channel:
 * only ErrorType enum names may appear, duplicates are deduplicated, ordering is
 * deterministic, and ValidationError field/message text can never leak.
 */
class PlanValidationDiagnosticsTest {

    @Test
    fun `valid result emits count zero`() {
        val line = PlanValidationDiagnostics.buildLine(ValidationResult(valid = true))
        assertEquals("result=VALID errorCount=0", line)
    }

    @Test
    fun `rejected result is sorted distinct enum names with count`() {
        val result = ValidationResult(
            valid = false,
            errors = listOf(
                ValidationError(ErrorType.PARALLEL_RESOURCE_CONFLICT, "oven", "hidden detail A"),
                ValidationError(ErrorType.INVALID_STEP_TYPE, "steps[3].type", "hidden detail B"),
                ValidationError(ErrorType.INVALID_STEP_TYPE, "steps[4].type", "hidden detail C")
            )
        )
        assertEquals(
            "result=REJECTED errorCount=2 errors=INVALID_STEP_TYPE,PARALLEL_RESOURCE_CONFLICT",
            PlanValidationDiagnostics.buildLine(result)
        )
    }

    @Test
    fun `only enum names can appear in the emitted line`() {
        val result = ValidationResult(
            valid = false,
            errors = listOf(
                ValidationError(ErrorType.EXCESSIVE_DURATION, "steps[1].durationSeconds", "SECRET-MESSAGE-TEXT"),
                ValidationError(ErrorType.UNKNOWN_RESOURCE, "steps[2].resource", "another SECRET detail")
            )
        )
        val line = PlanValidationDiagnostics.buildLine(result)
        assertTrue("no field path leak", "durationSeconds" !in line)
        assertTrue("no message leak", "SECRET" !in line)
        assertTrue("no message leak 2", "another" !in line)
        assertTrue("type present", "EXCESSIVE_DURATION" in line)
        assertTrue("type present 2", "UNKNOWN_RESOURCE" in line)
        assertTrue("no newline in line", !line.contains('\n'))
    }

    @Test
    fun `logValidation writes through the emit seam`() {
        val captured = mutableListOf<Pair<Int, String>>()
        val previous = PlanValidationDiagnostics.emit
        PlanValidationDiagnostics.emit = { priority, message -> captured.add(priority to message) }
        try {
            PlanValidationDiagnostics.logValidation(ValidationResult(valid = true))
        } finally {
            PlanValidationDiagnostics.emit = previous
        }
        assertEquals(1, captured.size)
        assertEquals("result=VALID errorCount=0", captured[0].second)
    }
}
