package com.agentickitchen.shared.inventory

enum class ShoppingCategory { PRODUCE, MEAT, DAIRY, PANTRY, OTHER }

data class ShoppingListItem(
    val id: String,
    val canonicalIngredientId: String?,
    val originalName: String,
    val quantity: Double,
    val unit: String,
    val category: ShoppingCategory,
    val sourceRecipeId: String,
    val sourceRecipeName: String,
    val checked: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

interface ShoppingListRepository {
    fun getAll(): List<ShoppingListItem>
    fun replaceRecipeShortages(sourceRecipeId: String, items: List<ShoppingListItem>)
    fun setChecked(id: String, checked: Boolean)
    fun delete(id: String)
    fun clearChecked()
}

class InMemoryShoppingListRepository : ShoppingListRepository {
    private val items = linkedMapOf<String, ShoppingListItem>()
    override fun getAll(): List<ShoppingListItem> = items.values.sortedWith(
        compareBy<ShoppingListItem> { it.checked }.thenBy { it.category.ordinal }.thenBy { it.createdAt }
    )
    override fun replaceRecipeShortages(sourceRecipeId: String, items: List<ShoppingListItem>) {
        this.items.entries.removeIf { it.value.sourceRecipeId == sourceRecipeId }
        items.forEach { this.items[it.id] = it }
    }
    override fun setChecked(id: String, checked: Boolean) {
        val item = items[id] ?: return
        items[id] = item.copy(checked = checked)
    }
    override fun delete(id: String) { items.remove(id) }
    override fun clearChecked() { items.entries.removeIf { it.value.checked } }
}

object SmartShoppingPlanner {
    fun planForRecipe(
        existing: List<ShoppingListItem>,
        shortages: List<PantryShortage>,
        sourceRecipeId: String,
        sourceRecipeName: String,
        timestamp: String,
        idFactory: () -> String
    ): List<ShoppingListItem> {
        val existingForRecipe = existing.filter { it.sourceRecipeId == sourceRecipeId }
        return shortages
            .filter { it.missingQuantity.isFinite() && it.missingQuantity > 0.000_001 }
            .map { shortage ->
                val previous = existingForRecipe.firstOrNull {
                    LocalIngredientResolver.matches(
                        it.originalName, it.canonicalIngredientId,
                        shortage.ingredientName, shortage.canonicalIngredientId
                    ) && it.unit.equals(shortage.unit, ignoreCase = true)
                }
                ShoppingListItem(
                    id = previous?.id ?: idFactory(),
                    canonicalIngredientId = shortage.canonicalIngredientId,
                    originalName = shortage.ingredientName,
                    quantity = shortage.missingQuantity,
                    unit = shortage.unit,
                    category = classify(shortage.ingredientName, shortage.canonicalIngredientId),
                    sourceRecipeId = sourceRecipeId,
                    sourceRecipeName = sourceRecipeName,
                    checked = previous?.checked ?: false,
                    createdAt = previous?.createdAt ?: timestamp,
                    updatedAt = timestamp
                )
            }
    }

    fun classify(name: String, canonicalId: String?): ShoppingCategory {
        val id = canonicalId ?: LocalIngredientResolver.resolveCanonicalId(name)
        val normalized = with(LocalIngredientResolver) { name.normalized() }
        return when {
            id in setOf("tomato", "onion", "garlic") || normalized.containsAny("domates", "tomato", "sogan", "onion", "sarimsak", "garlic", "pepper", "biber", "potato", "patates", "carrot", "havuc", "apple", "elma", "lemon", "limon") -> ShoppingCategory.PRODUCE
            id in setOf("chicken", "chicken_breast") || normalized.containsAny("chicken", "tavuk", "beef", "dana", "lamb", "kuzu", "pork", "fish", "balik", "turkey", "hindi") -> ShoppingCategory.MEAT
            id in setOf("milk", "butter", "cheese") || normalized.containsAny("milk", "sut", "butter", "tereyag", "cheese", "peynir", "yogurt", "yogurt", "cream", "krema") -> ShoppingCategory.DAIRY
            id in setOf("rice", "pasta", "flour", "olive_oil", "salt", "black_pepper") || normalized.containsAny("rice", "pirinc", "pasta", "makarna", "flour", "un", "oil", "yag", "salt", "tuz", "spice", "baharat") -> ShoppingCategory.PANTRY
            else -> ShoppingCategory.OTHER
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any(::contains)
}
