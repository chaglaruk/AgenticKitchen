package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.db.AppDatabase

class SqlDelightShoppingListRepository(private val database: AppDatabase) : ShoppingListRepository {
    private val queries = database.appDatabaseQueries

    override fun getAll(): List<ShoppingListItem> = queries.selectAllShoppingListItems().executeAsList().map { row ->
        ShoppingListItem(
            id = row.id,
            canonicalIngredientId = row.canonicalIngredientId,
            originalName = row.originalName,
            quantity = row.quantity,
            unit = row.unit,
            category = runCatching { ShoppingCategory.valueOf(row.category) }.getOrDefault(ShoppingCategory.OTHER),
            sourceRecipeId = row.sourceRecipeId,
            sourceRecipeName = row.sourceRecipeName,
            checked = row.checked != 0L,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    }

    override fun replaceRecipeShortages(sourceRecipeId: String, items: List<ShoppingListItem>) {
        database.transaction {
            queries.deleteShoppingItemsForRecipe(sourceRecipeId)
            items.forEach { item ->
                queries.upsertShoppingListItem(
                    id = item.id,
                    canonicalIngredientId = item.canonicalIngredientId,
                    originalName = item.originalName,
                    quantity = item.quantity,
                    unit = item.unit,
                    category = item.category.name,
                    sourceRecipeId = item.sourceRecipeId,
                    sourceRecipeName = item.sourceRecipeName,
                    checked = if (item.checked) 1L else 0L,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt
                )
            }
        }
    }

    override fun setChecked(id: String, checked: Boolean) {
        queries.updateShoppingItemChecked(if (checked) 1L else 0L, id)
    }

    override fun delete(id: String) { queries.deleteShoppingListItem(id) }
    override fun clearChecked() { queries.deleteCheckedShoppingItems() }
}
