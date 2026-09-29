package com.agentickitchen.android.ai

import com.agentickitchen.shared.ai.AiFailureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused tests for the release-visible Firebase AI diagnostics channel:
 * sanitized category mapping and metadata-only output lines.
 *
 * The firebase-ai SDK exception classes have internal constructors, so the
 * exception-to-(AiFailureType, technical) mapping is exercised by FirebaseAiProvider's
 * catch chain; these tests verify the category contract on that mapping's outputs,
 * one entry per SDK exception family.
 */
class FirebaseAiDiagnosticsTest {

    @Test
    fun `provider unavailable technical discriminators map to distinct categories`() {
        assertEquals(
            "PERMISSION_MISSING",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.ProviderUnavailable, "firebase_app_check_or_permission")
        )
        assertEquals(
            "API_NOT_CONFIGURED",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.ProviderUnavailable, "firebase_ai_not_configured")
        )
        assertEquals(
            "SERVICE_DISABLED",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.ProviderUnavailable, "firebase_ai_disabled")
        )
        assertEquals(
            "SERVER_EXCEPTION",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.ProviderUnavailable, null)
        )
    }

    @Test
    fun `failure types map to their categories`() {
        assertEquals("QUOTA_EXCEEDED", FirebaseAiDiagnostics.categoryFor(AiFailureType.QuotaExceeded))
        assertEquals("QUOTA_EXCEEDED", FirebaseAiDiagnostics.categoryFor(AiFailureType.RateLimited))
        assertEquals("REQUEST_TIMEOUT", FirebaseAiDiagnostics.categoryFor(AiFailureType.Timeout))
        assertEquals("INVALID_API_KEY", FirebaseAiDiagnostics.categoryFor(AiFailureType.Unauthorized))
        assertEquals("SAFETY_BLOCKED", FirebaseAiDiagnostics.categoryFor(AiFailureType.SafetyBlocked))
        assertEquals("IO_NETWORK", FirebaseAiDiagnostics.categoryFor(AiFailureType.NetworkUnavailable))
        assertEquals("MISSING_CREDENTIAL", FirebaseAiDiagnostics.categoryFor(AiFailureType.MissingCredential))
        assertEquals("INVALID_RESPONSE", FirebaseAiDiagnostics.categoryFor(AiFailureType.InvalidResponse))
        assertEquals("INVALID_RESPONSE", FirebaseAiDiagnostics.categoryFor(AiFailureType.InvalidPlan))
    }

    @Test
    fun `unknown failures split firebase ai from generic`() {
        assertEquals(
            "FIREBASE_AI_EXCEPTION",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.Unknown, "firebase_ai_exception")
        )
        assertEquals("UNKNOWN_EXCEPTION", FirebaseAiDiagnostics.categoryFor(AiFailureType.Unknown))
        assertEquals(
            "UNKNOWN_EXCEPTION",
            FirebaseAiDiagnostics.categoryFor(AiFailureType.Unknown, "unrecognized")
        )
    }

    @Test
    fun `failure line contains only whitelisted metadata`() {
        val line = FirebaseAiDiagnostics.buildLine(
            kind = FirebaseResponseKind.COOKING_PLAN,
            result = "FAILURE",
            category = "SERVER_EXCEPTION",
            retryable = true
        )
        assertEquals(
            "kind=COOKING_PLAN result=FAILURE category=SERVER_EXCEPTION retryable=true schemaMode=JSON_ONLY",
            line
        )
        assertTrue("no exception text placeholder", "message" !in line && "exception" !in line)
    }

    @Test
    fun `success line contains kind result model and schema mode`() {
        val line = FirebaseAiDiagnostics.buildLine(
            kind = FirebaseResponseKind.RECIPE_OPTIONS,
            result = "SUCCESS",
            model = "gemini-3.8-flash"
        )
        assertEquals(
            "kind=RECIPE_OPTIONS result=SUCCESS model=gemini-3.8-flash schemaMode=STRICT_SCHEMA",
            line
        )
    }

    @Test
    fun `cooking plan and recipe photo are the only JSON_ONLY kinds`() {
        FirebaseResponseKind.entries.forEach { kind ->
            if (kind == FirebaseResponseKind.COOKING_PLAN || kind == FirebaseResponseKind.RECIPE_IMPORT_PHOTO) {
                assertEquals(FirebaseSchemaMode.JSON_ONLY, kind.schemaMode)
                assertEquals(null, kind.schema)
            } else {
                assertEquals(FirebaseSchemaMode.STRICT_SCHEMA, kind.schemaMode)
                assertTrue(kind.schema != null)
            }
        }
    }

    @Test
    fun `model names are sanitized against newline injection`() {
        val line = FirebaseAiDiagnostics.buildLine(
            kind = FirebaseResponseKind.COOKING_PLAN,
            result = "SUCCESS",
            model = "gemini-3.8-flash\nFAKE injection"
        )
        assertFalse("no newline in emitted line", line.contains('\n'))
        assertTrue("injection neutralized", line.contains("gemini-3.8-flash_FAKE_injection"))
    }
}
