package com.agentickitchen.android

import com.agentickitchen.shared.inventory.SelectedRecipeContractReason
import com.agentickitchen.shared.inventory.SelectedRecipeContractResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release-safe sanitization tests for the option-to-plan / inventory diagnostics channel:
 * only whitelisted metadata, deterministic ordering, no ingredient/recipe/pantry name leakage.
 */
class PlanInventoryDiagnosticsTest {

    private fun contract(vararg reasons: SelectedRecipeContractReason) = SelectedRecipeContractResult(
        valid = reasons.isEmpty(),
        reasons = reasons.toList()
    )

    @Test
    fun `valid contract line matches the whitelisted format`() {
        val line = PlanInventoryDiagnostics.buildLine(
            contract = contract(),
            expectedCount = 4,
            planCount = 4,
            optionShortageCount = 1,
            planShortageCount = 1,
            allowedMissing = 1,
            strict = false
        )
        assertEquals(
            "contract=VALID expectedCount=4 planCount=4 optionShortageCount=1 planShortageCount=1 allowedMissing=1 strict=false",
            line
        )
    }

    @Test
    fun `rejected line carries sorted distinct reason enums`() {
        val line = PlanInventoryDiagnostics.buildLine(
            contract = contract(
                SelectedRecipeContractReason.QUANTITY_DRIFT,
                SelectedRecipeContractReason.UNEXPECTED_INGREDIENT,
                SelectedRecipeContractReason.QUANTITY_DRIFT
            ),
            expectedCount = 4,
            planCount = 4,
            optionShortageCount = 1,
            planShortageCount = 2,
            allowedMissing = 1,
            strict = false
        )
        assertEquals(
            "contract=REJECTED reasons=QUANTITY_DRIFT,UNEXPECTED_INGREDIENT expectedCount=4 planCount=4 optionShortageCount=1 planShortageCount=2 allowedMissing=1 strict=false",
            line
        )
    }

    @Test
    fun `null contract reports N_A for chip based sessions`() {
        val line = PlanInventoryDiagnostics.buildLine(
            contract = null,
            expectedCount = 0,
            planCount = 6,
            optionShortageCount = 0,
            planShortageCount = 0,
            allowedMissing = 0,
            strict = false
        )
        assertTrue(line.contains("contract=N/A"))
        assertFalse(line.contains("reasons="))
    }

    @Test
    fun `no ingredient or recipe names can leak`() {
        val line = PlanInventoryDiagnostics.buildLine(
            contract = contract(SelectedRecipeContractReason.INCOMPATIBLE_UNIT),
            expectedCount = 4,
            planCount = 5,
            optionShortageCount = 1,
            planShortageCount = 2,
            allowedMissing = 1,
            strict = false
        )
        assertFalse(line.contains("tereyagi"))
        assertFalse(line.contains("ekmek"))
        assertFalse(line.contains("Yumurtali"))
        assertTrue(line.contains("INCOMPATIBLE_UNIT"))
        assertFalse(line.contains('\n'))
    }

    @Test
    fun `logContract writes through the emit seam with warning priority on rejection`() {
        val captured = mutableListOf<Pair<Int, String>>()
        val previous = PlanInventoryDiagnostics.emit
        PlanInventoryDiagnostics.emit = { priority, message -> captured.add(priority to message) }
        try {
            PlanInventoryDiagnostics.logContract(contract(), 4, 4, 1, 1, 1, false)
            PlanInventoryDiagnostics.logContract(contract(SelectedRecipeContractReason.QUANTITY_DRIFT), 4, 4, 1, 2, 1, false)
        } finally {
            PlanInventoryDiagnostics.emit = previous
        }
        assertEquals(2, captured.size)
        assertTrue(captured[0].second.startsWith("contract=VALID"))
        assertTrue(captured[1].second.startsWith("contract=REJECTED"))
    }
}
