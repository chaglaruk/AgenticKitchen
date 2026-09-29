package com.agentickitchen.android

import android.util.Log
import com.agentickitchen.shared.inventory.RecipeImportPlanGuard
import com.agentickitchen.shared.inventory.SelectedRecipeContractResult

/** Release-visible, metadata-only diagnostics for imported-recipe plan contracts. */
object RecipeImportDiagnostics {
    private const val TAG = "AKRecipeImport"
    private val sourceReasonCodes = setOf(
        "recipe_name_changed",
        "servings_changed",
        "ingredient_count_changed",
        "source_amount_unreviewed",
        "ingredient_missing",
        "ingredient_ambiguous",
        "ingredient_unit_changed",
        "ingredient_amount_changed",
        "ingredient_added"
    )

    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    internal fun billContractLine(
        contract: SelectedRecipeContractResult,
        expectedCount: Int,
        planCount: Int
    ): String = buildString {
        append("stage=BILL_CONTRACT result=")
        append(if (contract.valid) "VALID" else "REJECTED")
        if (!contract.valid) {
            append(" reasons=")
            append(contract.reasons.map { it.name }.distinct().sorted().joinToString(","))
        }
        append(" expectedCount=").append(expectedCount.coerceAtLeast(0))
        append(" planCount=").append(planCount.coerceAtLeast(0))
    }

    internal fun sourceGuardLine(result: RecipeImportPlanGuard.Result): String = buildString {
        append("stage=SOURCE_GUARD result=")
        append(if (result.valid) "VALID" else "REJECTED")
        if (!result.valid) {
            append(" reasons=")
            append(
                result.reasons
                    .map { if (it in sourceReasonCodes) it else "unknown_guard_reason" }
                    .distinct()
                    .sorted()
                    .joinToString(",")
            )
        }
    }

    internal fun logBillContract(
        contract: SelectedRecipeContractResult,
        expectedCount: Int,
        planCount: Int
    ) {
        emit(
            if (contract.valid) Log.INFO else Log.WARN,
            billContractLine(contract, expectedCount, planCount)
        )
    }

    internal fun logSourceGuard(result: RecipeImportPlanGuard.Result) {
        emit(if (result.valid) Log.INFO else Log.WARN, sourceGuardLine(result))
    }
}
