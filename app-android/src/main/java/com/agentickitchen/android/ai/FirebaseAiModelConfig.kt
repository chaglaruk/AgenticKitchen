package com.agentickitchen.android.ai

import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

internal enum class FirebaseAiTask {
    EXTRACTION,
    REASONING,
    VISION
}

internal interface FirebaseAiModelConfig {
    fun modelFor(task: FirebaseAiTask): String
    fun refresh()
}

internal class FirebaseRemoteModelConfig(firebaseApp: FirebaseApp) : FirebaseAiModelConfig {
    private val remoteConfig = FirebaseRemoteConfig.getInstance(firebaseApp)

    init {
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                .build()
        )
        remoteConfig.setDefaultsAsync(DEFAULTS)
        refresh()
    }

    override fun modelFor(task: FirebaseAiTask): String {
        val key = keyFor(task)
        val fallback = defaultFor(task)
        return remoteConfig.getString(key)
            .trim()
            .takeIf(::isAllowedModelName)
            ?: fallback
    }

    override fun refresh() {
        remoteConfig.fetchAndActivate()
    }

    private companion object {
        const val FETCH_INTERVAL_SECONDS = 3_600L
        const val KEY_EXTRACTION = "firebase_ai_model_extraction"
        const val KEY_REASONING = "firebase_ai_model_reasoning"
        const val KEY_VISION = "firebase_ai_model_vision"

        const val DEFAULT_EXTRACTION = "gemini-3.5-flash-lite"
        const val DEFAULT_REASONING = "gemini-3.7-flash"
        const val DEFAULT_VISION = "gemini-3.7-flash"

        val DEFAULTS: Map<String, Any> = mapOf(
            KEY_EXTRACTION to DEFAULT_EXTRACTION,
            KEY_REASONING to DEFAULT_REASONING,
            KEY_VISION to DEFAULT_VISION
        )

        fun keyFor(task: FirebaseAiTask): String = when (task) {
            FirebaseAiTask.EXTRACTION -> KEY_EXTRACTION
            FirebaseAiTask.REASONING -> KEY_REASONING
            FirebaseAiTask.VISION -> KEY_VISION
        }

        fun defaultFor(task: FirebaseAiTask): String = when (task) {
            FirebaseAiTask.EXTRACTION -> DEFAULT_EXTRACTION
            FirebaseAiTask.REASONING -> DEFAULT_REASONING
            FirebaseAiTask.VISION -> DEFAULT_VISION
        }

        fun isAllowedModelName(value: String): Boolean =
            value.length in 1..80 &&
                value.startsWith("gemini-") &&
                value.all { it.isLetterOrDigit() || it == '-' || it == '.' || it == '_' }
    }
}

internal class StaticFirebaseAiModelConfig(
    private val extraction: String = "gemini-3.5-flash-lite",
    private val reasoning: String = "gemini-3.7-flash",
    private val vision: String = "gemini-3.7-flash"
) : FirebaseAiModelConfig {
    override fun modelFor(task: FirebaseAiTask): String = when (task) {
        FirebaseAiTask.EXTRACTION -> extraction
        FirebaseAiTask.REASONING -> reasoning
        FirebaseAiTask.VISION -> vision
    }

    override fun refresh() = Unit
}
