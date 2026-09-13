package com.agentickitchen.shared.recipes

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.db.AppDatabase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SqlDelightSavedRecipeRepository(
    database: AppDatabase,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : SavedRecipeRepository {
    private val queries = database.appDatabaseQueries

    override fun getAll(): List<SavedRecipe> =
        queries.selectAllSavedRecipes().executeAsList().mapNotNull(::decode)

    override fun getById(id: String): SavedRecipe? =
        queries.selectSavedRecipeById(id).executeAsOneOrNull()?.let(::decode)

    override fun upsert(recipe: SavedRecipe) {
        queries.upsertSavedRecipe(
            id = recipe.id,
            name = recipe.recipe.name,
            recipeJson = json.encodeToString(recipe.recipe),
            sourceKind = recipe.source.name,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt,
            lastCookedAt = recipe.lastCookedAt,
            cookCount = recipe.cookCount.toLong()
        )
    }

    override fun delete(id: String) {
        queries.deleteSavedRecipe(id)
    }

    override fun recordCooked(id: String, at: String) {
        queries.recordSavedRecipeCooked(
            lastCookedAt = at,
            updatedAt = at,
            id = id
        )
    }

    private fun decode(row: com.agentickitchen.shared.db.SavedRecipe): SavedRecipe? {
        val recipe = runCatching {
            json.decodeFromString<ImportedRecipe>(row.recipeJson)
        }.getOrNull() ?: return null
        return SavedRecipe(
            id = row.id,
            recipe = recipe,
            source = runCatching { SavedRecipeSource.valueOf(row.sourceKind) }
                .getOrDefault(SavedRecipeSource.IMPORTED),
            createdAt = row.createdAt,
            updatedAt = row.updatedAt,
            lastCookedAt = row.lastCookedAt,
            cookCount = row.cookCount.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        )
    }
}
