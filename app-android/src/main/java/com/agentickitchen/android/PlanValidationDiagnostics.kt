package com.agentickitchen.android

import android.util.Log
import com.agentickitchen.shared.validator.ValidationResult

/**
 * Release-visible, metadata-only diagnostics for Cooking Plan validation.
 *
 * The Firebase AI diagnostics channel proves provider success; this channel reports the
 * subsequent app-side CookingPlanValidator outcome so Play-distributed (non-debuggable)
 * builds can distinguish provider failures from fail-closed plan rejections.
 *
 * Every emitted field is whitelisted metadata: the outcome, the count of distinct error
 * types, and the sorted distinct ErrorType enum names. ValidationError field paths and
 * messages, recipe names, ingredients, instructions, prompts/responses, identifiers, and
 * exception details are never passed in and can therefore never leak into the output.
 */
object PlanValidationDiagnostics {
    private const val TAG = "AKPlanValidation"

    // Default writes straight to Logcat; unit tests replace this seam because
    // android.util.Log is not mocked in the local JVM.
    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    internal fun buildLine(validation: ValidationResult): String {
        if (validation.valid) return "result=VALID errorCount=0"
        val types = validation.errors.map { it.type.name }.distinct().sorted()
        return "result=REJECTED errorCount=${types.size} errors=${types.joinToString(",")}"
    }

    internal fun logValidation(validation: ValidationResult) {
        emit(if (validation.valid) Log.INFO else Log.WARN, buildLine(validation))
    }
}
