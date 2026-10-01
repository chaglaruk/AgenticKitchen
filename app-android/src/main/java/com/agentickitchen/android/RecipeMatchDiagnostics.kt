package com.agentickitchen.android

import android.util.Log
import com.agentickitchen.shared.inventory.RecipeMatchResult
import com.agentickitchen.shared.inventory.RecipeMatcher

/**
 * Release-visible, metadata-only diagnostics for the local pantry matcher.
 *
 * Emits one line per ranked recipe candidate so non-debuggable Play builds can prove exactly
 * why each option received READY_NOW / MISSING_ONE / MISSING_TWO / AI_IDEA. Every field is
 * whitelisted, app-constructed metadata: rank index, proposed ingredient count, shortage
 * count, tier enum, coverage percent, and two booleans. Recipe names, ingredient names,
 * pantry item names, quantities, prompts/responses, and identifiers are never passed in and
 * can therefore never leak into the output.
 */
object RecipeMatchDiagnostics {
    private const val TAG = "AKRecipeMatch"

    // Default writes straight to Logcat; unit tests replace this seam because
    // android.util.Log is not mocked in the local JVM.
    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    internal fun buildLine(index: Int, proposedCount: Int, result: RecipeMatchResult): String {
        val line = StringBuilder("index=").append(sanitize(index.toString()))
            .append(" proposedCount=").append(sanitize(proposedCount.toString()))
            .append(" shortageCount=").append(sanitize(result.shortages.size.toString()))
            .append(" tier=").append(sanitize(result.tier.name))
            .append(" coverage=").append(sanitize(result.pantryCoveragePercent.toString()))
            .append(" canPrepare=").append(RecipeMatcher.canPrepareFromPantry(result))
            .append(" equipmentFit=").append(result.equipmentFit)
        return line.toString()
    }

    internal fun logMatches(ranked: List<RecipeMatchResult>, proposedCountById: Map<String, Int>) {
        ranked.forEachIndexed { index, result ->
            val proposedCount = proposedCountById[result.candidateId] ?: 0
            emit(Log.INFO, buildLine(index + 1, proposedCount, result))
        }
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(32)
}
