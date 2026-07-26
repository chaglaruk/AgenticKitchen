package com.agentickitchen.shared.db

class HistoryRepository(private val database: AppDatabase) : RecipeHistoryRepository {
    private val queries = database.appDatabaseQueries

    override fun getAllHistory(): List<RecipeHistory> {
        return queries.selectAll().executeAsList()
    }

    override fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String) {
        queries.insertRecipe(id, name, ingredients, timestamp, status)
    }

    override fun deleteRecipe(id: String) {
        queries.deleteRecipe(id)
    }

    fun clearHistory() {
        queries.clearHistory()
    }
}
