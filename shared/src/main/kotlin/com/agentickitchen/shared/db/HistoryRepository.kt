package com.agentickitchen.shared.db

class HistoryRepository(private val database: AppDatabase) : RecipeHistoryRepository {
    private val queries = database.appDatabaseQueries
    private val liveHistory = mutableListOf<RecipeHistory>()
    private var loaded = false

    override fun getAllHistory(): List<RecipeHistory> {
        if (!loaded) refresh()
        return liveHistory
    }

    override fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String) {
        queries.insertRecipe(id, name, ingredients, timestamp, status)
        refresh()
    }

    override fun updateStatus(id: String, status: String) {
        queries.updateRecipeStatus(status, id)
        val index = liveHistory.indexOfFirst { it.id == id }
        if (index >= 0) {
            liveHistory[index] = liveHistory[index].copy(status = status)
        } else if (loaded) {
            refresh()
        }
    }

    override fun deleteRecipe(id: String) {
        queries.deleteRecipe(id)
        liveHistory.removeAll { it.id == id }
    }

    fun clearHistory() {
        queries.clearHistory()
        liveHistory.clear()
        loaded = true
    }

    private fun refresh() {
        liveHistory.clear()
        liveHistory.addAll(queries.selectAll().executeAsList())
        loaded = true
    }
}
