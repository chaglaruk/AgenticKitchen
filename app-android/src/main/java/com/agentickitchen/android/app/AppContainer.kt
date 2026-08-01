package com.agentickitchen.android.app

import android.app.Application
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.DefaultAiProviderFactory
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.android.data.preferences.PreferencesManager
import com.agentickitchen.android.security.CredentialStore
import com.agentickitchen.android.security.SecureCredentialStore
import com.agentickitchen.shared.agents.SimpleIngredientAgent
import com.agentickitchen.shared.agents.SimpleOrchestrator
import com.agentickitchen.shared.agents.SimplePantryIntelAgent
import com.agentickitchen.shared.agents.SimpleTimingAgent
import com.agentickitchen.shared.db.AppDatabase
import com.agentickitchen.shared.db.HistoryRepository
import com.agentickitchen.shared.inventory.SqlDelightPantryInventoryRepository
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import java.io.Closeable

class AppContainer(private val app: Application) : Closeable {

    val credentialStore: CredentialStore = SecureCredentialStore(app)
    val preferences: AppPreferences = PreferencesManager(app, credentialStore)

    private val sqlDriver: SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, app, "agentic.db")

    val database: AppDatabase = AppDatabase(sqlDriver)

    val historyRepository: HistoryRepository = HistoryRepository(database)
    val pantryInventoryRepository = SqlDelightPantryInventoryRepository(database)

    val targetTimeResolver: TargetTimeResolver = TargetTimeResolver()

    private val timingAgent: SimpleTimingAgent = SimpleTimingAgent()
    private val ingredientAgent: SimpleIngredientAgent = SimpleIngredientAgent()

    val orchestrator: SimpleOrchestrator = SimpleOrchestrator(
        ingredientAgent = ingredientAgent,
        timingAgent = timingAgent
    )

    val pantryIntelAgent: SimplePantryIntelAgent = SimplePantryIntelAgent()
    val providerFactory: AiProviderFactory = DefaultAiProviderFactory()

    override fun close() {
        providerFactory.close()
        sqlDriver.close()
    }
}
