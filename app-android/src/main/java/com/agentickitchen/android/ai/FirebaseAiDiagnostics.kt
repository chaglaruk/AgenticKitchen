package com.agentickitchen.android.ai

import android.util.Log
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.AiResult

/**
 * Release-visible, metadata-only diagnostics for Firebase AI transport calls.
 *
 * Writes directly to Logcat so that Play-distributed (non-debuggable) builds remain observable.
 * Every emitted field is whitelisted metadata constructed by this module: response kind, outcome,
 * model name, schema mode, sanitized failure category, and retryability. Prompt text, response
 * text, image data, ingredient/recipe names, credentials, tokens, account/device identifiers,
 * request bodies, raw exception messages, stack traces, and URLs are never passed in and can
 * therefore never leak into the output.
 */
object FirebaseAiDiagnostics {
    private const val TAG = "AKFirebaseAI"

    // Default writes straight to Logcat; unit tests replace this seam because
    // android.util.Log is not mocked in the local JVM.
    internal var emit: (priority: Int, message: String) -> Unit =
        { priority, message -> Log.println(priority, TAG, message) }

    /**
     * Sanitized failure category derived from the provider failure type and, where the provider
     * records one, its technical discriminator. Kept aligned with the catch-chain in
     * FirebaseAiProvider so each category identifies exactly one SDK exception family.
     */
    internal fun categoryFor(type: AiFailureType, technical: String? = null): String = when (type) {
        AiFailureType.ProviderUnavailable -> when (technical) {
            "firebase_app_check_or_permission" -> "PERMISSION_MISSING"
            "firebase_ai_not_configured" -> "API_NOT_CONFIGURED"
            "firebase_ai_disabled" -> "SERVICE_DISABLED"
            else -> "SERVER_EXCEPTION"
        }
        AiFailureType.QuotaExceeded, AiFailureType.RateLimited -> "QUOTA_EXCEEDED"
        AiFailureType.Timeout -> "REQUEST_TIMEOUT"
        AiFailureType.Unauthorized -> "INVALID_API_KEY"
        AiFailureType.SafetyBlocked -> "SAFETY_BLOCKED"
        AiFailureType.NetworkUnavailable -> "IO_NETWORK"
        AiFailureType.MissingCredential -> "MISSING_CREDENTIAL"
        AiFailureType.InvalidResponse, AiFailureType.InvalidPlan -> when (technical) {
            "empty_response" -> "EMPTY_RESPONSE"
            "json_decode_failure" -> "JSON_DECODE_FAILURE"
            "decode_argument_failure" -> "DECODE_ARGUMENT_FAILURE"
            "response_validation_failure" -> "RESPONSE_VALIDATION_FAILURE"
            else -> "INVALID_RESPONSE"
        }
        AiFailureType.Unknown -> when (technical) {
            "firebase_ai_exception" -> "FIREBASE_AI_EXCEPTION"
            else -> "UNKNOWN_EXCEPTION"
        }
    }

    internal fun logRequest(kind: FirebaseResponseKind, model: String) {
        emit(Log.INFO, buildLine(kind, result = "REQUEST", model = model))
    }

    internal fun logOutcome(kind: FirebaseResponseKind, result: AiResult<*>) {
        when (result) {
            is AiResult.Success ->
                emit(Log.INFO, buildLine(kind, result = "SUCCESS", model = result.model))
            is AiResult.Failure ->
                emit(
                    Log.WARN,
                    buildLine(
                        kind,
                        result = "FAILURE",
                        category = categoryFor(result.type, result.technicalMessage),
                        retryable = result.retryable
                    )
                )
        }
    }

    internal fun buildLine(
        kind: FirebaseResponseKind,
        result: String,
        model: String? = null,
        category: String? = null,
        retryable: Boolean? = null
    ): String {
        val line = StringBuilder("kind=").append(sanitize(kind.name))
            .append(" result=").append(sanitize(result))
        model?.let { line.append(" model=").append(sanitize(it)) }
        category?.let { line.append(" category=").append(sanitize(it)) }
        retryable?.let { line.append(" retryable=").append(it) }
        line.append(" schemaMode=").append(sanitize(kind.schemaMode.name))
        return line.toString()
    }

    private fun sanitize(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(64)
}
