package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.shared.ai.KitchenAiProvider
import java.io.Closeable

interface AiProviderFactory : Closeable {
    fun provider(settings: HardwareSettings): KitchenAiProvider?
}

class DefaultAiProviderFactory(
    private val geminiFactory: (String) -> KitchenAiProvider = ::GeminiProvider,
    offlineProvider: KitchenAiProvider = LocalRecipeProvider()
) : AiProviderFactory {
    private var geminiKey: String? = null
    private var geminiProvider: KitchenAiProvider? = null
    private val safeOfflineProvider: KitchenAiProvider = SafetyEnforcingAiProvider(offlineProvider)

    override fun provider(settings: HardwareSettings): KitchenAiProvider? = when (settings.aiProvider) {
        "GEMINI" -> settings.geminiApiKey.takeIf(String::isNotBlank)?.let { key ->
            if (key != geminiKey) {
                closeProvider(geminiProvider)
                geminiKey = key
                geminiProvider = SafetyEnforcingAiProvider(geminiFactory(key))
            }
            geminiProvider
        }
        "FREE" -> safeOfflineProvider
        else -> null
    }

    override fun close() {
        closeProvider(geminiProvider)
        closeProvider(safeOfflineProvider)
        geminiProvider = null
    }

    private fun closeProvider(provider: KitchenAiProvider?) {
        (provider as? Closeable)?.close()
    }
}
