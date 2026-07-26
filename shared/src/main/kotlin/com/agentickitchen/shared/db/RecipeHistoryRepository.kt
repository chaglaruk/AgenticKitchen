package com.agentickitchen.shared.db

interface RecipeHistoryRepository {
    fun getAllHistory(): List<RecipeHistory>
    fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String)
    fun deleteRecipe(id: String)
}
