package com.agentickitchen.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Debug-only, metadata-only diagnostics.
 *
 * User content, prompts, AI responses, ingredients, questions, image data, credentials,
 * exception messages, stack traces, file paths, and payload lengths are deliberately discarded.
 */
object AppLogger {
    private const val TAG = "AK"
    private const val MAX_ENTRIES = 500
    private const val MAX_FILE_BYTES = 1_000_000L

    private var logFile: File? = null
    private var enabled = false
    private val ringBuffer = ConcurrentLinkedQueue<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        enabled = runCatching {
            val flags = context.packageManager.getApplicationInfo(context.packageName, 0).flags
            flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        }.getOrDefault(false)
        if (!enabled) return

        logFile = File(context.filesDir, "agentic_log.txt").also { file ->
            if (file.exists() && file.length() > MAX_FILE_BYTES) file.writeText("")
        }
        log("I", "Logger", "LOGGER_READY")
    }

    fun i(component: String, message: String) = log("I", component, eventCode(component, message))
    fun w(component: String, message: String) = log("W", component, eventCode(component, message))
    fun d(component: String, message: String) = log("D", component, eventCode(component, message))

    fun e(component: String, message: String, throwable: Throwable? = null) {
        @Suppress("UNUSED_VARIABLE")
        val discarded = throwable
        log("E", component, eventCode(component, message))
    }

    fun aiRequest(feature: String, promptPreview: String) {
        @Suppress("UNUSED_VARIABLE")
        val discarded = promptPreview
        log("I", "AI-${safeComponent(feature)}", "AI_REQUEST_STARTED")
    }

    fun aiResponse(feature: String, responsePreview: String) {
        @Suppress("UNUSED_VARIABLE")
        val discarded = responsePreview
        log("I", "AI-${safeComponent(feature)}", "AI_RESPONSE_RECEIVED")
    }

    fun aiError(feature: String, error: Throwable) {
        @Suppress("UNUSED_VARIABLE")
        val discarded = error
        log("W", "AI-${safeComponent(feature)}", "AI_REQUEST_FAILED")
    }

    fun getRecentLogs(count: Int = 50): List<String> =
        if (enabled) ringBuffer.toList().takeLast(count.coerceIn(0, MAX_ENTRIES)) else emptyList()

    fun getFullLog(): String = if (!enabled) {
        "(logging disabled)"
    } else {
        runCatching { logFile?.readText().orEmpty() }.getOrDefault("(log unavailable)")
    }

    private fun eventCode(component: String, message: String): String = when {
        component.startsWith("AI-") -> "AI_REQUEST_FAILED"
        component == "Recovery" -> "SESSION_RESTORE_FAILED"
        component == "Setup" -> "SETUP_COMPLETED"
        component == "Session" -> "RECIPE_OPTIONS_REQUESTED"
        component == "Inventory" -> "INVENTORY_MATCH_MISSED"
        component == "Logger" -> "LOGGER_READY"
        message.isBlank() -> "EVENT"
        else -> "EVENT"
    }

    private fun safeComponent(component: String): String =
        component.uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_-]"), "_")
            .take(32)
            .ifBlank { "APP" }

    private fun log(level: String, component: String, event: String) {
        if (!enabled) return
        val safeComponent = safeComponent(component)
        val safeEvent = event.uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_-]"), "_")
            .take(48)
            .ifBlank { "EVENT" }
        val entry = "${dateFormat.format(Date())} [$level] [$safeComponent] $safeEvent"

        when (level) {
            "I" -> Log.i(TAG, "[$safeComponent] $safeEvent")
            "W" -> Log.w(TAG, "[$safeComponent] $safeEvent")
            "E" -> Log.e(TAG, "[$safeComponent] $safeEvent")
            else -> Log.d(TAG, "[$safeComponent] $safeEvent")
        }

        ringBuffer.add(entry)
        while (ringBuffer.size > MAX_ENTRIES) ringBuffer.poll()
        runCatching { logFile?.appendText("$entry\n") }
    }
}
