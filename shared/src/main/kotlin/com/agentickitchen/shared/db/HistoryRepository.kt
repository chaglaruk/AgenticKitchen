package com.agentickitchen.shared.db

class HistoryRepository(private val database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun getAllHistory(): List<RecipeHistory> {
        return queries.selectAll().executeAsList()
    }

    fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String) {
        queries.insertRecipe(id, name, ingredients, timestamp, status)
    }

    fun deleteRecipe(id: String) {
        queries.deleteRecipe(id)
    }

    fun clearHistory() {
        queries.clearHistory()
    }
}
