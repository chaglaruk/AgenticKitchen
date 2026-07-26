package com.agentickitchen.android.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.agentickitchen.android.AppViewModel
import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.scheduler.TargetTimeResolver

class AppViewModelFactory(
    private val preferences: AppPreferences,
    private val historyRepository: RecipeHistoryRepository,
    private val orchestrator: Orchestrator,
    private val pantryIntelAgent: PantryIntelAgent,
    private val providerFactory: AiProviderFactory,
    private val targetTimeResolver: TargetTimeResolver
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == AppViewModel::class.java) { "Unsupported ViewModel class: ${modelClass.name}" }
        return AppViewModel(preferences, historyRepository, orchestrator, pantryIntelAgent, providerFactory, targetTimeResolver) as T
    }

    companion object {
        fun from(container: AppContainer) = AppViewModelFactory(
            container.preferences,
            container.historyRepository,
            container.orchestrator,
            container.pantryIntelAgent,
            container.providerFactory,
            container.targetTimeResolver
        )
    }
}
