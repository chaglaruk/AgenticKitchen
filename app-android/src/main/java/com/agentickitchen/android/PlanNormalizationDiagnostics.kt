package com.agentickitchen.android

import android.util.Log
import com.agentickitchen.shared.validator.ExclusiveResourceSequencer

/**
 * Release-visible, metadata-only diagnostics for the exclusive-resource sequencing boundary.
 *
 * Emitted after every Cooking Plan normalization so a release build can prove whether v8
 * actually repaired a dependency edge before validation. Every field is whitelisted
 * app-constructed metadata: the edge count, the outcome enum, and per-resource edge counts.
 * Step ids, instructions, recipe names, ingredient names, prompts/responses, and identifiers
 * are never passed in and can therefore never leak into the output.
 */
object PlanNormalizationDiagnostics {
    private const val TAG = "AKPlanNormalization"

    // Default writes straight to Logcat; unit tests replace this seam because
    // android.util.Log is not mocked in the local JVM.
    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    internal fun buildLine(outcome: ExclusiveResourceSequencer.Outcome): String {
        val line = StringBuilder("exclusiveEdgesAdded=").append(sanitize(outcome.edgesAdded.toString()))
            .append(" result=").append(sanitize(outcome.result.name))
            .append(" stoveEdges=").append(sanitize(outcome.stoveEdges.toString()))
            .append(" ovenEdges=").append(sanitize(outcome.ovenEdges.toString()))
            .append(" airfryerEdges=").append(sanitize(outcome.airfryerEdges.toString()))
        return line.toString()
    }

    internal fun log(outcome: ExclusiveResourceSequencer.Outcome) {
        val rejected = outcome.result == ExclusiveResourceSequencer.Result.SKIPPED_INVALID_GRAPH
        emit(if (rejected) Log.WARN else Log.INFO, buildLine(outcome))
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(32)
}
