package com.agentickitchen.android

import com.agentickitchen.shared.inventory.RecipeImportPlanGuard
import com.agentickitchen.shared.inventory.SelectedRecipeContractReason
import com.agentickitchen.shared.inventory.SelectedRecipeContractResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeImportDiagnosticsTest {
    @Test
    fun `valid bill contract exposes only stage status and counts`() {
        assertEquals(
            "stage=BILL_CONTRACT result=VALID expectedCount=3 planCount=3",
            RecipeImportDiagnostics.billContractLine(
                SelectedRecipeContractResult(true, emptyList()),
                expectedCount = 3,
                planCount = 3
            )
        )
    }

    @Test
    fun `rejected bill reasons are enum names sorted and deduplicated`() {
        val line = RecipeImportDiagnostics.billContractLine(
            SelectedRecipeContractResult(
                false,
                listOf(
                    SelectedRecipeContractReason.QUANTITY_DRIFT,
                    SelectedRecipeContractReason.MISSING_EXPECTED_INGREDIENT,
                    SelectedRecipeContractReason.QUANTITY_DRIFT
                )
            ),
            expectedCount = 3,
            planCount = 2
        )
        assertEquals(
            "stage=BILL_CONTRACT result=REJECTED reasons=MISSING_EXPECTED_INGREDIENT,QUANTITY_DRIFT expectedCount=3 planCount=2",
            line
        )
    }

    @Test
    fun `source guard reasons are known codes sorted and deduplicated`() {
        val line = RecipeImportDiagnostics.sourceGuardLine(
            RecipeImportPlanGuard.Result(
                false,
                listOf("servings_changed", "ingredient_amount_changed", "servings_changed")
            )
        )
        assertEquals(
            "stage=SOURCE_GUARD result=REJECTED reasons=ingredient_amount_changed,servings_changed",
            line
        )
    }

    @Test
    fun `diagnostics cannot leak arbitrary recipe content`() {
        val secret = "SECRET recipe 200 ml https://private.example"
        val line = RecipeImportDiagnostics.sourceGuardLine(
            RecipeImportPlanGuard.Result(false, listOf(secret))
        )
        assertEquals(
            "stage=SOURCE_GUARD result=REJECTED reasons=unknown_guard_reason",
            line
        )
        assertFalse(line.contains("SECRET"))
        assertFalse(line.contains("200"))
        assertFalse(line.contains("https"))
    }

    @Test
    fun `valid source guard exposes no recipe data`() {
        val line = RecipeImportDiagnostics.sourceGuardLine(RecipeImportPlanGuard.Result(true, emptyList()))
        assertEquals("stage=SOURCE_GUARD result=VALID", line)
        assertTrue(!line.contains('\n'))
    }
}
