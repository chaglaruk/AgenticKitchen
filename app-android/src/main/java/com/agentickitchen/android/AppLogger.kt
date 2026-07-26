package com.agentickitchen.android

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * AppLogger — Agentic Kitchen için merkezi loglama sistemi.
 *
 * Tüm loglar hem Android Logcat'e (TAG: "AK") hem de cihaz üzerindeki bir dosyaya yazılır.
 * Dosya yolu: /data/data/com.agentickitchen.android/files/agentic_log.txt
 *
 * Geliştirici bu logları şu komutlarla çekebilir:
 *   adb logcat -s AK        (gerçek zamanlı Logcat akışı)
 *   adb shell run-as com.agentickitchen.android cat files/agentic_log.txt  (dosya olarak)
 *
 * Tüm AI istekleri, yanıtları, hatalar ve kullanıcı eylemleri bu logger üzerinden kaydedilir.
 */
object AppLogger {

    private const val TAG = "AK"
    private const val MAX_ENTRIES = 500
    private var logFile: File? = null

    // Bellek içi ring-buffer (son 500 satır)
    private val ringBuffer = ConcurrentLinkedQueue<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** Application.onCreate içinde bir kez çağrılmalı */
    fun init(context: Context) {
        logFile = File(context.filesDir, "agentic_log.txt")
        // Dosya 1 MB'ı geçtiyse sıfırla
        logFile?.let {
            if (it.exists() && it.length() > 1_000_000) {
                it.writeText("")
            }
        }
        i("Logger", "AppLogger başlatıldı — dosya: ${logFile?.absolutePath}")
    }

    // ── Seviye metodları ──────────────────────────────────────────────

    /** Bilgi logu */
    fun i(component: String, message: String) = log("I", component, message)

    /** Uyarı logu */
    fun w(component: String, message: String) = log("W", component, message)

    /** Hata logu */
    fun e(component: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message | ${throwable.stackTraceToString().take(500)}" else message
        log("E", component, msg)
    }

    /** Debug logu */
    fun d(component: String, message: String) = log("D", component, message)

    // ── AI özel logları ──────────────────────────────────────────────

    /** AI isteği gönderilmeden önce */
    fun aiRequest(feature: String, promptPreview: String) {
        i("AI-$feature", "→ İstek gönderiliyor: ${promptPreview.take(120)}...")
    }

    /** AI yanıtı alındığında */
    fun aiResponse(feature: String, responsePreview: String) {
        i("AI-$feature", "← Yanıt alındı (${responsePreview.length} char): ${responsePreview.take(200)}...")
    }

    /** AI hatası */
    fun aiError(feature: String, error: Throwable) {
        e("AI-$feature", "✖ AI Hatası: ${error.message}", error)
    }

    // ── İç implementasyon ────────────────────────────────────────────

    private fun log(level: String, component: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "$timestamp [$level] [$component] $message"

        // Logcat
        when (level) {
            "I" -> Log.i(TAG, "[$component] $message")
            "W" -> Log.w(TAG, "[$component] $message")
            "E" -> Log.e(TAG, "[$component] $message")
            "D" -> Log.d(TAG, "[$component] $message")
        }

        // Ring buffer
        ringBuffer.add(entry)
        while (ringBuffer.size > MAX_ENTRIES) ringBuffer.poll()

        // Dosyaya yaz (fire-and-forget, UI thread'i bloklamaz çünkü küçük satırlar)
        try {
            logFile?.appendText("$entry\n")
        } catch (_: Exception) { /* dosya yazılamıyorsa sessizce geç */ }
    }

    /** Son N log satırını döndürür (UI'da göstermek için) */
    fun getRecentLogs(count: Int = 50): List<String> {
        return ringBuffer.toList().takeLast(count)
    }

    /** Dosyadaki tüm logları döndürür */
    fun getFullLog(): String {
        return try {
            logFile?.readText() ?: "(log dosyası yok)"
        } catch (e: Exception) {
            "(log dosyası okunamadı: ${e.message})"
        }
    }
}
