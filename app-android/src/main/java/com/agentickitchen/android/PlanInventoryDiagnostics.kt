package com.agentickitchen.android

import android.util.Log
import com.agentickitchen.shared.inventory.SelectedRecipeContractResult

/**
 * Release-visible, metadata-only diagnostics for the option-to-plan ingredient contract and the
 * inventory shortage guard on the prepared Cooking Plan.
 *
 * Emitted only for pantry-backed sessions after provider success and CookingPlanValidator, so a
 * release build can prove exactly why a prepared plan reached READY or was rejected. Every
 * field is whitelisted metadata: the contract outcome, sorted distinct reason enum names, and
 * counters/booleans. Ingredient names, pantry names, quantities, recipe names, prompts/
 * responses, and identifiers are never passed in and can therefore never leak.
 */
object PlanInventoryDiagnostics {
    private const val TAG = "AKPlanInventory"

    // Default writes straight to Logcat; unit tests replace this seam because
    // android.util.Log is not mocked in the local JVM.
    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    internal fun buildLine(
        contract: SelectedRecipeContractResult?,
        expectedCount: Int,
        planCount: Int,
        optionShortageCount: Int,
        planShortageCount: Int,
        allowedMissing: Int,
        strict: Boolean
    ): String {
        val line = StringBuilder("contract=")
        when {
            contract == null -> line.append("N/A")
            contract.valid -> line.append("VALID")
            else -> line.append("REJECTED")
        }
        if (contract != null && !contract.valid) {
            line.append(" reasons=")
                .append(contract.reasons.map { sanitize(it.name) }.distinct().sorted().joinToString(","))
        }
        line.append(" expectedCount=").append(expectedCount)
            .append(" planCount=").append(planCount)
            .append(" optionShortageCount=").append(optionShortageCount)
            .append(" planShortageCount=").append(planShortageCount)
            .append(" allowedMissing=").append(allowedMissing)
            .append(" strict=").append(strict)
        return line.toString()
    }

    internal fun logContract(
        contract: SelectedRecipeContractResult?,
        expectedCount: Int,
        planCount: Int,
        optionShortageCount: Int,
        planShortageCount: Int,
        allowedMissing: Int,
        strict: Boolean
    ) {
        val rejected = contract != null && !contract.valid
        emit(
            if (rejected) Log.WARN else Log.INFO,
            buildLine(contract, expectedCount, planCount, optionShortageCount, planShortageCount, allowedMissing, strict)
        )
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(32)
}
