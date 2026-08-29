package com.agentickitchen.android.ai

import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

internal enum class FirebaseAiTask {
    EXTRACTION,
    REASONING,
    VISION
}

internal object FirebaseAiModelDefaults {
    const val EXTRACTION = "gemini-3.5-flash-lite"
    const val REASONING = "gemini-3.7-flash"
    const val VISION = "gemini-3.7-flash"

    fun forTask(task: FirebaseAiTask): String = when (task) {
        FirebaseAiTask.EXTRACTION -> EXTRACTION
        FirebaseAiTask.REASONING -> REASONING
        FirebaseAiTask.VISION -> VISION
    }
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
        val fallback = FirebaseAiModelDefaults.forTask(task)
        return remoteConfig.getString(keyFor(task))
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

        val DEFAULTS: Map<String, Any> = mapOf(
            KEY_EXTRACTION to FirebaseAiModelDefaults.EXTRACTION,
            KEY_REASONING to FirebaseAiModelDefaults.REASONING,
            KEY_VISION to FirebaseAiModelDefaults.VISION
        )

        fun keyFor(task: FirebaseAiTask): String = when (task) {
            FirebaseAiTask.EXTRACTION -> KEY_EXTRACTION
            FirebaseAiTask.REASONING -> KEY_REASONING
            FirebaseAiTask.VISION -> KEY_VISION
        }

        fun isAllowedModelName(value: String): Boolean =
            value.length in 1..80 &&
                value.startsWith("gemini-") &&
                value.all { it.isLetterOrDigit() || it == '-' || it == '.' || it == '_' }
    }
}

internal class StaticFirebaseAiModelConfig(
    private val extraction: String = FirebaseAiModelDefaults.EXTRACTION,
    private val reasoning: String = FirebaseAiModelDefaults.REASONING,
    private val vision: String = FirebaseAiModelDefaults.VISION
) : FirebaseAiModelConfig {
    override fun modelFor(task: FirebaseAiTask): String = when (task) {
        FirebaseAiTask.EXTRACTION -> extraction
        FirebaseAiTask.REASONING -> reasoning
        FirebaseAiTask.VISION -> vision
    }

    override fun refresh() = Unit
}
