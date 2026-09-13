package com.agentickitchen.shared.db

interface RecipeHistoryRepository {
    fun getAllHistory(): List<RecipeHistory>
    fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String)
    fun updateStatus(id: String, status: String) = Unit
    fun deleteRecipe(id: String)
}
