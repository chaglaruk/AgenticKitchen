package com.agentickitchen.android.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.agentickitchen.shared.db.AppDatabase

class DatabaseManager(context: Context) {

    private val driver: SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, context, "agentic.db")

    val database: AppDatabase = AppDatabase(driver)
}
