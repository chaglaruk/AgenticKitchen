package com.agentickitchen.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object AppLogger {

    private const val TAG = "AK"
    private const val MAX_ENTRIES = 500
    private var logFile: File? = null
    private var isRelease = true

    private val ringBuffer = ConcurrentLinkedQueue<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        isRelease = try {
            val flags = context.packageManager.getApplicationInfo(context.packageName, 0).flags
            flags and ApplicationInfo.FLAG_DEBUGGABLE == 0
        } catch (_: Exception) {
            true
        }
        if (isRelease) return
        logFile = File(context.filesDir, "agentic_log.txt")
        logFile?.let {
            if (it.exists() && it.length() > 1_000_000) {
                it.writeText("")
            }
        }
        i("Logger", "AppLogger started — file: ${logFile?.absolutePath}")
    }

    fun i(component: String, message: String) = log("I", component, message)

    fun w(component: String, message: String) = log("W", component, message)

    fun e(component: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message | ${throwable.stackTraceToString().take(500)}" else message
        log("E", component, msg)
    }

    fun d(component: String, message: String) = log("D", component, message)

    fun aiRequest(feature: String, promptPreview: String) {
        if (!isRelease) {
            i("AI-$feature", "Request: ${promptPreview.take(120)}...")
        }
    }

    fun aiResponse(feature: String, responsePreview: String) {
        if (!isRelease) {
            i("AI-$feature", "Response (${responsePreview.length} chars): ${responsePreview.take(200)}...")
        }
    }

    fun aiError(feature: String, error: Throwable) {
        e("AI-$feature", "AI Error: ${error.message}", error)
    }

    private fun log(level: String, component: String, message: String) {
        if (isRelease) return
        val timestamp = dateFormat.format(Date())
        val entry = "$timestamp [$level] [$component] $message"

        when (level) {
            "I" -> Log.i(TAG, "[$component] $message")
            "W" -> Log.w(TAG, "[$component] $message")
            "E" -> Log.e(TAG, "[$component] $message")
            "D" -> Log.d(TAG, "[$component] $message")
        }

        ringBuffer.add(entry)
        while (ringBuffer.size > MAX_ENTRIES) ringBuffer.poll()

        try {
            logFile?.appendText("$entry\n")
        } catch (_: Exception) { }
    }

    fun getRecentLogs(count: Int = 50): List<String> {
        return ringBuffer.toList().takeLast(count)
    }

    fun getFullLog(): String {
        return try {
            logFile?.readText() ?: "(no log file)"
        } catch (e: Exception) {
            "(could not read log: ${e.message})"
        }
    }
}
