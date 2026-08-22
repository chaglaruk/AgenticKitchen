package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.shared.ai.KitchenAiProvider
import java.io.Closeable

interface AiProviderFactory : Closeable {
    fun provider(settings: HardwareSettings): KitchenAiProvider?
}

class DefaultAiProviderFactory(
    private val geminiFactory: (String) -> KitchenAiProvider = ::GeminiProvider,
    private val offlineProvider: KitchenAiProvider = InventoryAwareOfflineProvider(),
    private val enforceVisionSafety: Boolean = false
) : AiProviderFactory {
    private var geminiKey: String? = null
    private var geminiProvider: KitchenAiProvider? = null
    private val runtimeOfflineProvider: KitchenAiProvider = protect(offlineProvider)

    override fun provider(settings: HardwareSettings): KitchenAiProvider? = when (settings.aiProvider) {
        "GEMINI" -> settings.geminiApiKey.takeIf(String::isNotBlank)?.let { key ->
            if (key != geminiKey) {
                closeProvider(geminiProvider)
                geminiKey = key
                geminiProvider = protect(geminiFactory(key))
            }
            geminiProvider
        }
        "FREE" -> runtimeOfflineProvider
        else -> null
    }

    override fun close() {
        closeProvider(geminiProvider)
        closeProvider(runtimeOfflineProvider)
        geminiProvider = null
    }

    private fun protect(provider: KitchenAiProvider): KitchenAiProvider =
        if (enforceVisionSafety) SafetyEnforcingAiProvider(provider) else provider

    private fun closeProvider(provider: KitchenAiProvider?) {
        (provider as? Closeable)?.close()
    }
}
