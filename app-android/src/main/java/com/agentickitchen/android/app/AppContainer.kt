package com.agentickitchen.android.app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.agentickitchen.shared.agents.SimpleIngredientAgent
import com.agentickitchen.shared.agents.SimpleOrchestrator
import com.agentickitchen.shared.agents.SimplePantryIntelAgent
import com.agentickitchen.shared.agents.SimpleTimingAgent
import com.agentickitchen.shared.db.AppDatabase
import com.agentickitchen.shared.db.HistoryRepository
import com.agentickitchen.shared.scheduler.TargetTimeResolver

class AppContainer(private val app: Application) {

    val prefs: SharedPreferences = app.getSharedPreferences("agentic_prefs", Context.MODE_PRIVATE)

    private val sqlDriver: SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, app, "agentic.db")

    val database: AppDatabase = AppDatabase(sqlDriver)

    val historyRepository: HistoryRepository = HistoryRepository(database)

    val targetTimeResolver: TargetTimeResolver = TargetTimeResolver()

    private val timingAgent: SimpleTimingAgent = SimpleTimingAgent()

    private val ingredientAgent: SimpleIngredientAgent = SimpleIngredientAgent()

    val orchestrator: SimpleOrchestrator = SimpleOrchestrator(
        ingredientAgent = ingredientAgent,
        timingAgent = timingAgent
    )

    val pantryIntelAgent: SimplePantryIntelAgent = SimplePantryIntelAgent()
}
