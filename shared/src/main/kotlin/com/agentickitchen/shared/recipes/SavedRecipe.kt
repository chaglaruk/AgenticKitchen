package com.agentickitchen.shared.recipes

import com.agentickitchen.shared.ai.ImportedRecipe

enum class SavedRecipeSource {
    IMPORTED,
    GENERATED_AI,
    GENERATED_OFFLINE,
    COOKED,
    MANUAL
}

data class SavedRecipe(
    val id: String,
    val recipe: ImportedRecipe,
    val source: SavedRecipeSource,
    val createdAt: String,
    val updatedAt: String,
    val lastCookedAt: String? = null,
    val cookCount: Int = 0
)

interface SavedRecipeRepository {
    fun getAll(): List<SavedRecipe>
    fun getById(id: String): SavedRecipe?
    fun upsert(recipe: SavedRecipe)
    fun delete(id: String)
    fun recordCooked(id: String, at: String)
}

class InMemorySavedRecipeRepository : SavedRecipeRepository {
    private val recipes = linkedMapOf<String, SavedRecipe>()

    override fun getAll(): List<SavedRecipe> = recipes.values.sortedWith(
        compareByDescending<SavedRecipe> { it.updatedAt }
            .thenBy { it.recipe.name.lowercase() }
    )

    override fun getById(id: String): SavedRecipe? = recipes[id]

    override fun upsert(recipe: SavedRecipe) {
        recipes[recipe.id] = recipe
    }

    override fun delete(id: String) {
        recipes.remove(id)
    }

    override fun recordCooked(id: String, at: String) {
        val recipe = recipes[id] ?: return
        recipes[id] = recipe.copy(
            lastCookedAt = at,
            updatedAt = at,
            cookCount = recipe.cookCount + 1
        )
    }
}
