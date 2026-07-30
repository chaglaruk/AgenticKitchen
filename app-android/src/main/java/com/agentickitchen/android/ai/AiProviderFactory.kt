package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.google.ai.client.generativeai.GenerativeModel
import java.io.Closeable

interface AiProviderFactory : Closeable {
    fun provider(settings: HardwareSettings): LlmProvider?
    fun gemini(settings: HardwareSettings, model: String): GenerativeModel?
    fun vision(settings: HardwareSettings): HuggingFaceVisionService
}

class DefaultAiProviderFactory : AiProviderFactory {
    private var providerKey: String? = null
    private var cachedProvider: LlmProvider? = null
    private var visionKey: String? = null
    private var cachedVision: HuggingFaceVisionService? = null

    override fun provider(settings: HardwareSettings): LlmProvider? {
        val key = "${settings.aiProvider}:${settings.geminiApiKey}:${settings.hfApiKey}"
        if (key == providerKey) return cachedProvider
        closeProvider(cachedProvider)
        providerKey = key
        cachedProvider = when (settings.aiProvider) {
            "GEMINI" -> settings.geminiApiKey.takeIf { it.isNotBlank() }?.let { GeminiProvider(GenerativeModel("gemini-1.5-flash", it)) }
            "HUGGINGFACE" -> settings.hfApiKey.takeIf { it.isNotBlank() }?.let(::HuggingFaceService)
            "FREE" -> LocalRecipeProvider()
            else -> null
        }
        return cachedProvider
    }

    override fun gemini(settings: HardwareSettings, model: String) = settings.geminiApiKey.takeIf { it.isNotBlank() }?.let { GenerativeModel(model, it) }

    override fun vision(settings: HardwareSettings): HuggingFaceVisionService {
        if (settings.hfApiKey != visionKey) {
            cachedVision?.close()
            visionKey = settings.hfApiKey
            cachedVision = HuggingFaceVisionService(settings.hfApiKey)
        }
        return requireNotNull(cachedVision)
    }

    override fun close() {
        closeProvider(cachedProvider)
        cachedVision?.close()
        cachedProvider = null
        cachedVision = null
    }

    private fun closeProvider(provider: LlmProvider?) = when (provider) {
        is HuggingFaceService -> provider.close()
        else -> Unit
    }
}
