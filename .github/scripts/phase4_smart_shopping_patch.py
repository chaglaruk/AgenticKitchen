from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


def write(path: str, content: str):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content, encoding="utf-8")


# Phase 3 persistence hardening: restore shortages and persist mutated READY plan.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''                plan = plan,\n                plannedUsage = plannedUsage\n            )\n''',
    '''                plan = plan,\n                plannedUsage = plannedUsage,\n                shortages = InventoryWorkflow.planUsage(plan, _inventory.value, reservedQuantities()).shortages\n            )\n''',
    "restore READY shortages",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''                    substitutionState = SubstitutionState.Idle\n                )\n                emitUiEvent(if (L.isTr) "Değişiklik plana uygulandı ve zamanlama yeniden hesaplandı." else "Substitution applied and the schedule was recalculated.")\n''',
    '''                    substitutionState = SubstitutionState.Idle\n                )\n                persistActiveSession()\n                reconcileTrackedShoppingForRecipe(active.recipe.id, active.recipe.name, usage.shortageDetails)\n                emitUiEvent(if (L.isTr) "Değişiklik plana uygulandı ve zamanlama yeniden hesaplandı." else "Substitution applied and the schedule was recalculated.")\n''',
    "persist substituted READY plan",
)

# Structured shortage details for deterministic shopping diffs.
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/inventory/InventoryWorkflow.kt",
    '''data class InventoryUsagePlan(\n    val usages: List<PlannedPantryUsage>,\n    val shortages: List<String>\n)\n''',
    '''data class PantryShortage(\n    val ingredientName: String,\n    val canonicalIngredientId: String?,\n    val requiredQuantity: Double,\n    val availableQuantity: Double,\n    val missingQuantity: Double,\n    val unit: String\n)\n\ndata class InventoryUsagePlan(\n    val usages: List<PlannedPantryUsage>,\n    val shortages: List<String>,\n    val shortageDetails: List<PantryShortage> = emptyList()\n)\n''',
    "structured shortage model",
)
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/inventory/InventoryWorkflow.kt",
    '''        val usages = mutableListOf<PlannedPantryUsage>()\n        val shortages = mutableListOf<String>()\n        plan.ingredients.forEach { ingredient ->\n''',
    '''        val usages = mutableListOf<PlannedPantryUsage>()\n        val shortages = mutableListOf<String>()\n        val shortageDetails = mutableListOf<PantryShortage>()\n        plan.ingredients.forEach { ingredient ->\n''',
    "shortage detail accumulator",
)
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/inventory/InventoryWorkflow.kt",
    '''            if (item == null) {\n                shortages += ingredient.name\n                return@forEach\n            }\n            val needed = runCatching { InventoryUnits.normalize(ingredient.quantity, ingredient.unit) }.getOrNull()\n            val current = runCatching { InventoryUnits.normalize(item.quantity, item.unit) }.getOrNull()\n            if (needed == null || current == null || !compatible(needed, current)) {\n                shortages += ingredient.name\n                return@forEach\n            }\n            val available = current.quantity - reservedByItem.getOrDefault(item.id, 0.0)\n            if (needed.quantity > available + 0.000_001) {\n                shortages += ingredient.name\n            } else {\n''',
    '''            val needed = runCatching { InventoryUnits.normalize(ingredient.quantity, ingredient.unit) }.getOrNull()\n            if (item == null) {\n                shortages += ingredient.name\n                if (needed != null && needed.dimension != UnitDimension.UNKNOWN) {\n                    shortageDetails += PantryShortage(\n                        ingredientName = ingredient.name,\n                        canonicalIngredientId = resolvedIngredientCanonicalId,\n                        requiredQuantity = needed.quantity,\n                        availableQuantity = 0.0,\n                        missingQuantity = needed.quantity,\n                        unit = needed.unit\n                    )\n                }\n                return@forEach\n            }\n            val current = runCatching { InventoryUnits.normalize(item.quantity, item.unit) }.getOrNull()\n            if (needed == null || current == null || !compatible(needed, current)) {\n                shortages += ingredient.name\n                if (needed != null && needed.dimension != UnitDimension.UNKNOWN) {\n                    shortageDetails += PantryShortage(\n                        ingredientName = ingredient.name,\n                        canonicalIngredientId = resolvedIngredientCanonicalId,\n                        requiredQuantity = needed.quantity,\n                        availableQuantity = 0.0,\n                        missingQuantity = needed.quantity,\n                        unit = needed.unit\n                    )\n                }\n                return@forEach\n            }\n            val available = (current.quantity - reservedByItem.getOrDefault(item.id, 0.0)).coerceAtLeast(0.0)\n            if (needed.quantity > available + 0.000_001) {\n                shortages += ingredient.name\n                shortageDetails += PantryShortage(\n                    ingredientName = ingredient.name,\n                    canonicalIngredientId = resolvedIngredientCanonicalId ?: item.canonicalIngredientId,\n                    requiredQuantity = needed.quantity,\n                    availableQuantity = available,\n                    missingQuantity = (needed.quantity - available).coerceAtLeast(0.0),\n                    unit = needed.unit\n                )\n            } else {\n''',
    "compute shortage amounts",
)
replace_once(
    "shared/src/main/kotlin/com/agentickitchen/shared/inventory/InventoryWorkflow.kt",
    '''        return InventoryUsagePlan(usages, shortages.distinct())\n''',
    '''        return InventoryUsagePlan(\n            usages = usages,\n            shortages = shortages.distinct(),\n            shortageDetails = shortageDetails\n                .filter { it.missingQuantity > 0.000_001 }\n                .distinctBy { (it.canonicalIngredientId ?: it.ingredientName.lowercase()) to it.unit }\n        )\n''',
    "return shortage details",
)

# Local shopping domain/planner/repository.
write("shared/src/main/kotlin/com/agentickitchen/shared/inventory/SmartShopping.kt", r'''package com.agentickitchen.shared.inventory

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
''')

write("shared/src/main/kotlin/com/agentickitchen/shared/inventory/SqlDelightShoppingListRepository.kt", r'''package com.agentickitchen.shared.inventory

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
''')

# SQL schema + v4 migration.
replace_once(
    "shared/src/main/sqldelight/com/agentickitchen/shared/db/AppDatabase.sq",
    '''CREATE TABLE ActiveCookingSession (\n''',
    '''CREATE TABLE ShoppingListItem (\n    id TEXT NOT NULL PRIMARY KEY,\n    canonicalIngredientId TEXT,\n    originalName TEXT NOT NULL,\n    quantity REAL NOT NULL,\n    unit TEXT NOT NULL,\n    category TEXT NOT NULL,\n    sourceRecipeId TEXT NOT NULL,\n    sourceRecipeName TEXT NOT NULL,\n    checked INTEGER NOT NULL DEFAULT 0,\n    createdAt TEXT NOT NULL,\n    updatedAt TEXT NOT NULL\n);\n\nselectAllShoppingListItems:\nSELECT * FROM ShoppingListItem ORDER BY checked ASC, category ASC, createdAt ASC;\n\nupsertShoppingListItem:\nINSERT OR REPLACE INTO ShoppingListItem(\n    id, canonicalIngredientId, originalName, quantity, unit, category,\n    sourceRecipeId, sourceRecipeName, checked, createdAt, updatedAt\n) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);\n\nupdateShoppingItemChecked:\nUPDATE ShoppingListItem SET checked = ? WHERE id = ?;\n\ndeleteShoppingListItem:\nDELETE FROM ShoppingListItem WHERE id = ?;\n\ndeleteShoppingItemsForRecipe:\nDELETE FROM ShoppingListItem WHERE sourceRecipeId = ?;\n\ndeleteCheckedShoppingItems:\nDELETE FROM ShoppingListItem WHERE checked != 0;\n\nCREATE TABLE ActiveCookingSession (\n''',
    "shopping SQL schema",
)
write("shared/src/main/sqldelight/com/agentickitchen/shared/db/4.sqm", r'''CREATE TABLE ShoppingListItem (
    id TEXT NOT NULL PRIMARY KEY,
    canonicalIngredientId TEXT,
    originalName TEXT NOT NULL,
    quantity REAL NOT NULL,
    unit TEXT NOT NULL,
    category TEXT NOT NULL,
    sourceRecipeId TEXT NOT NULL,
    sourceRecipeName TEXT NOT NULL,
    checked INTEGER NOT NULL DEFAULT 0,
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL
);
''')

# Shared tests: shortage math, planner idempotence/category, SQL persistence.
write("shared/src/test/kotlin/com/agentickitchen/shared/inventory/SmartShoppingTest.kt", r'''package com.agentickitchen.shared.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.db.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartShoppingTest {
    @Test fun shortageDetailsUseOnlyMissingAmountAfterStockAndReservation() {
        val plan = CookingPlanResponse(
            "Rice", 2,
            listOf(PlannedIngredientDto("Rice", 200.0, "g", "rice")),
            listOf(CookingStepDto("s", "cook", "Cook", "stovetop", 60)),
            emptyList()
        )
        val stock = PantryStockItem(
            id="rice-stock", canonicalIngredientId="rice", originalName="Rice", quantity=150.0,
            unit="g", unitDimension=UnitDimension.MASS, source="manual", createdAt="t", updatedAt="t"
        )
        val usage = InventoryWorkflow.planUsage(plan, listOf(stock), mapOf("rice-stock" to 20.0))
        assertEquals(listOf("Rice"), usage.shortages)
        assertEquals(1, usage.shortageDetails.size)
        assertEquals(70.0, usage.shortageDetails.single().missingQuantity, 0.0001)
        assertEquals("g", usage.shortageDetails.single().unit)
    }

    @Test fun plannerReusesRecipeItemInsteadOfDuplicatingAndClassifiesLocally() {
        val shortage = PantryShortage("Tomato", "tomato", 3.0, 1.0, 2.0, "adet")
        val first = SmartShoppingPlanner.planForRecipe(emptyList(), listOf(shortage), "recipe", "Soup", "t1") { "id-1" }
        val second = SmartShoppingPlanner.planForRecipe(first, listOf(shortage.copy(missingQuantity = 1.0)), "recipe", "Soup", "t2") { "id-2" }
        assertEquals("id-1", second.single().id)
        assertEquals(1.0, second.single().quantity, 0.0001)
        assertEquals(ShoppingCategory.PRODUCE, second.single().category)
    }

    @Test fun sqlRepositoryReplacesRecipeRowsAndPersistsCheckedState() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            AppDatabase.Schema.create(driver)
            val repository = SqlDelightShoppingListRepository(AppDatabase(driver))
            val item = ShoppingListItem("1", "rice", "Rice", 100.0, "g", ShoppingCategory.PANTRY, "recipe", "Rice bowl", false, "t1", "t1")
            repository.replaceRecipeShortages("recipe", listOf(item))
            assertEquals(listOf("1"), repository.getAll().map { it.id })
            repository.setChecked("1", true)
            assertTrue(repository.getAll().single().checked)
            repository.replaceRecipeShortages("recipe", emptyList())
            assertTrue(repository.getAll().isEmpty())
        } finally {
            driver.close()
        }
    }
}
''')

# Production DI.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppContainer.kt",
    '''import com.agentickitchen.shared.inventory.SqlDelightPantryInventoryRepository\n''',
    '''import com.agentickitchen.shared.inventory.SqlDelightPantryInventoryRepository\nimport com.agentickitchen.shared.inventory.SqlDelightShoppingListRepository\nimport com.agentickitchen.shared.inventory.ShoppingListRepository\n''',
    "AppContainer shopping imports",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppContainer.kt",
    '''    val pantryInventoryRepository: PantryInventoryRepository =\n        HistoryTrackingPantryInventoryRepository(pantryStorage, historyRepository)\n\n    val targetTimeResolver''',
    '''    val pantryInventoryRepository: PantryInventoryRepository =\n        HistoryTrackingPantryInventoryRepository(pantryStorage, historyRepository)\n    val shoppingListRepository: ShoppingListRepository = SqlDelightShoppingListRepository(database)\n\n    val targetTimeResolver''',
    "AppContainer shopping repository",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppViewModelFactory.kt",
    '''import com.agentickitchen.shared.inventory.PantryInventoryRepository\n''',
    '''import com.agentickitchen.shared.inventory.PantryInventoryRepository\nimport com.agentickitchen.shared.inventory.ShoppingListRepository\n''',
    "factory shopping import",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppViewModelFactory.kt",
    '''    private val providerFactory: AiProviderFactory,\n    private val targetTimeResolver: TargetTimeResolver\n''',
    '''    private val providerFactory: AiProviderFactory,\n    private val targetTimeResolver: TargetTimeResolver,\n    private val shoppingListRepository: ShoppingListRepository\n''',
    "factory constructor",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppViewModelFactory.kt",
    '''            providerFactory,\n            targetTimeResolver\n''',
    '''            providerFactory,\n            targetTimeResolver,\n            shoppingListRepository\n''',
    "factory VM args",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/app/AppViewModelFactory.kt",
    '''            container.providerFactory,\n            container.targetTimeResolver\n''',
    '''            container.providerFactory,\n            container.targetTimeResolver,\n            container.shoppingListRepository\n''',
    "factory container args",
)

# ViewModel shopping state/actions. Optional default keeps existing tests/source constructors compatible.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''import com.agentickitchen.shared.inventory.SubstitutionMutationValidator\n''',
    '''import com.agentickitchen.shared.inventory.SubstitutionMutationValidator\nimport com.agentickitchen.shared.inventory.InMemoryShoppingListRepository\nimport com.agentickitchen.shared.inventory.PantryShortage\nimport com.agentickitchen.shared.inventory.ShoppingListItem\nimport com.agentickitchen.shared.inventory.ShoppingListRepository\nimport com.agentickitchen.shared.inventory.SmartShoppingPlanner\n''',
    "ViewModel shopping imports",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    private val providerFactory: AiProviderFactory,\n    private val targetTimeResolver: TargetTimeResolver\n) : ViewModel() {\n''',
    '''    private val providerFactory: AiProviderFactory,\n    private val targetTimeResolver: TargetTimeResolver,\n    private val shoppingListRepository: ShoppingListRepository = InMemoryShoppingListRepository()\n) : ViewModel() {\n''',
    "ViewModel shopping repository",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    private val _shoppingImportState = MutableStateFlow<ShoppingImportState>(ShoppingImportState.Idle)\n    val shoppingImportState: StateFlow<ShoppingImportState> = _shoppingImportState.asStateFlow()\n''',
    '''    private val _shoppingImportState = MutableStateFlow<ShoppingImportState>(ShoppingImportState.Idle)\n    val shoppingImportState: StateFlow<ShoppingImportState> = _shoppingImportState.asStateFlow()\n    private val _shoppingList = MutableStateFlow(shoppingListRepository.getAll())\n    val shoppingList: StateFlow<List<ShoppingListItem>> = _shoppingList.asStateFlow()\n''',
    "ViewModel shopping state",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt",
    '''    fun startCooking() {\n''',
    r'''    fun addCurrentShortagesToShoppingList() {
        val active = _planState.value as? PlanState.RecipeActive ?: return
        val plan = active.cookingPlan ?: return
        val usage = InventoryWorkflow.planUsage(plan, _inventory.value, reservedQuantities())
        if (usage.shortageDetails.isEmpty()) {
            emitUiEvent(if (L.isTr) "Bu tarif için alınacak eksik malzeme yok." else "There are no missing items to buy for this recipe.")
            return
        }
        replaceRecipeShopping(active.recipe.id, active.recipe.name, usage.shortageDetails)
        emitUiEvent(if (L.isTr) "Eksik malzemeler alışveriş listesine eklendi." else "Missing ingredients were added to your shopping list.")
    }

    fun setShoppingItemChecked(id: String, checked: Boolean) {
        shoppingListRepository.setChecked(id, checked)
        refreshShoppingList()
    }

    fun deleteShoppingItem(id: String) {
        shoppingListRepository.delete(id)
        refreshShoppingList()
    }

    fun clearCheckedShoppingItems() {
        shoppingListRepository.clearChecked()
        refreshShoppingList()
    }

    private fun refreshShoppingList() {
        _shoppingList.value = shoppingListRepository.getAll()
    }

    private fun replaceRecipeShopping(sourceRecipeId: String, sourceRecipeName: String, shortages: List<PantryShortage>) {
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val items = SmartShoppingPlanner.planForRecipe(
            existing = shoppingListRepository.getAll(),
            shortages = shortages,
            sourceRecipeId = sourceRecipeId,
            sourceRecipeName = sourceRecipeName,
            timestamp = timestamp,
            idFactory = { UUID.randomUUID().toString() }
        )
        shoppingListRepository.replaceRecipeShortages(sourceRecipeId, items)
        refreshShoppingList()
    }

    private fun reconcileTrackedShoppingForRecipe(sourceRecipeId: String, sourceRecipeName: String, shortages: List<PantryShortage>) {
        if (shoppingListRepository.getAll().none { it.sourceRecipeId == sourceRecipeId }) return
        replaceRecipeShopping(sourceRecipeId, sourceRecipeName, shortages)
    }

    fun startCooking() {
''',
    "ViewModel shopping actions",
)

# Home UI list.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''import com.agentickitchen.shared.inventory.ShoppingImportMode\n''',
    '''import com.agentickitchen.shared.inventory.ShoppingImportMode\nimport com.agentickitchen.shared.inventory.ShoppingCategory\nimport com.agentickitchen.shared.inventory.ShoppingListItem\n''',
    "Home shopping imports",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''    shoppingImportState: ShoppingImportState = ShoppingImportState.Idle,\n    scannedIngredients: List<String>?,\n''',
    '''    shoppingImportState: ShoppingImportState = ShoppingImportState.Idle,\n    shoppingList: List<ShoppingListItem> = emptyList(),\n    scannedIngredients: List<String>?,\n''',
    "Home shopping state arg",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''    onClearShoppingImport: () -> Unit = {},\n    onConfigureGemini: () -> Unit = {},\n''',
    '''    onClearShoppingImport: () -> Unit = {},\n    onToggleShoppingItem: (String, Boolean) -> Unit = { _, _ -> },\n    onDeleteShoppingItem: (String) -> Unit = {},\n    onClearCheckedShoppingItems: () -> Unit = {},\n    onConfigureGemini: () -> Unit = {},\n''',
    "Home shopping callbacks",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''        if (chips.isNotEmpty()) {\n            item(span = { GridItemSpan(maxLineSpan) }) { CompactKitchenSummary(pantryIntel) }\n        }\n\n        item(span = { GridItemSpan(maxLineSpan) }) {\n''',
    '''        if (shoppingList.isNotEmpty()) {\n            item(span = { GridItemSpan(maxLineSpan) }) {\n                SmartShoppingListSection(\n                    items = shoppingList,\n                    onToggle = onToggleShoppingItem,\n                    onDelete = onDeleteShoppingItem,\n                    onClearChecked = onClearCheckedShoppingItems\n                )\n            }\n        }\n        if (chips.isNotEmpty()) {\n            item(span = { GridItemSpan(maxLineSpan) }) { CompactKitchenSummary(pantryIntel) }\n        }\n\n        item(span = { GridItemSpan(maxLineSpan) }) {\n''',
    "Home shopping section placement",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''private fun formatInventoryQuantity(item: PantryStockItem): String =\n''',
    r'''@Composable
private fun SmartShoppingListSection(
    items: List<ShoppingListItem>,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onClearChecked: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(if (L.isTr) "ALIŞVERİŞ" else "SHOPPING", color = colors.primary, style = MaterialTheme.typography.overline)
                Text(if (L.isTr) "Eksiklerin" else "What you need", color = colors.onSurface, style = MaterialTheme.typography.h5)
            }
            if (items.any { it.checked }) {
                TextButton(onClick = onClearChecked) {
                    Text(if (L.isTr) "Tamamlananları temizle" else "Clear completed", color = colors.primary)
                }
            }
        }
        ShoppingCategory.values().forEach { category ->
            val categoryItems = items.filter { it.category == category }
            if (categoryItems.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(shoppingCategoryLabel(category), color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                categoryItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggle(item.id, !item.checked) }) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = if (item.checked) {
                                    if (L.isTr) "Tamamlanmadı olarak işaretle" else "Mark not completed"
                                } else {
                                    if (L.isTr) "Tamamlandı olarak işaretle" else "Mark completed"
                                },
                                tint = if (item.checked) colors.success else colors.onSurfaceSub
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.originalName, color = if (item.checked) colors.onSurfaceSub else colors.onSurface, style = MaterialTheme.typography.body1)
                            Text(
                                "${BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()} ${LocalIngredientResolver.localizeUnit(item.unit, L.isTr)} · ${item.sourceRecipeName}",
                                color = colors.onSurfaceSub,
                                style = MaterialTheme.typography.caption
                            )
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Listeden sil" else "Remove from list", tint = colors.onSurfaceSub)
                        }
                    }
                    Divider(color = colors.divider)
                }
            }
        }
    }
}

private fun shoppingCategoryLabel(category: ShoppingCategory): String = when (category) {
    ShoppingCategory.PRODUCE -> if (L.isTr) "SEBZE & MEYVE" else "PRODUCE"
    ShoppingCategory.MEAT -> if (L.isTr) "ET & BALIK" else "MEAT & FISH"
    ShoppingCategory.DAIRY -> if (L.isTr) "SÜT ÜRÜNLERİ" else "DAIRY"
    ShoppingCategory.PANTRY -> if (L.isTr) "KİLER" else "PANTRY"
    ShoppingCategory.OTHER -> if (L.isTr) "DİĞER" else "OTHER"
}

private fun formatInventoryQuantity(item: PantryStockItem): String =
''',
    "Home shopping composable",
)
# LocalIngredientResolver import needed by composable.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt",
    '''import com.agentickitchen.shared.inventory.PantryStockItem\n''',
    '''import com.agentickitchen.shared.inventory.PantryStockItem\nimport com.agentickitchen.shared.inventory.LocalIngredientResolver\n''',
    "Home resolver import",
)

# Operations one-action add after substitution-first rows.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''    onApplySubstitution: () -> Unit = {},\n    onDismissSubstitution: () -> Unit = {}\n) {\n''',
    '''    onApplySubstitution: () -> Unit = {},\n    onDismissSubstitution: () -> Unit = {},\n    onAddShortagesToShoppingList: () -> Unit = {}\n) {\n''',
    "Operations shopping callback",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''                        onRequest = onRequestSubstitution,\n                        onApply = onApplySubstitution,\n                        onDismiss = onDismissSubstitution\n                    )\n''',
    '''                        onRequest = onRequestSubstitution,\n                        onApply = onApplySubstitution,\n                        onDismiss = onDismissSubstitution,\n                        onAddToShopping = onAddShortagesToShoppingList\n                    )\n''',
    "Operations shopping section wiring",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''    onRequest: (String) -> Unit,\n    onApply: () -> Unit,\n    onDismiss: () -> Unit\n) {\n''',
    '''    onRequest: (String) -> Unit,\n    onApply: () -> Unit,\n    onDismiss: () -> Unit,\n    onAddToShopping: () -> Unit\n) {\n''',
    "PantrySubstitution shopping arg",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/ui/OperationsScreen.kt",
    '''            state.shortages.forEach { shortage ->\n                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n                    Text(shortage, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))\n                    TextButton(onClick = { onRequest(shortage) }) {\n                        Text(if (L.isTr) "Alternatif bul" else "Find substitute", color = colors.primary)\n                    }\n                }\n            }\n            when (val substitution = state.substitutionState) {\n''',
    '''            state.shortages.forEach { shortage ->\n                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n                    Text(shortage, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))\n                    TextButton(onClick = { onRequest(shortage) }) {\n                        Text(if (L.isTr) "Alternatif bul" else "Find substitute", color = colors.primary)\n                    }\n                }\n            }\n            OutlinedButton(\n                onClick = onAddToShopping,\n                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),\n                border = BorderStroke(1.dp, colors.divider),\n                shape = RoundedCornerShape(999.dp)\n            ) {\n                Text(if (L.isTr) "Kalan eksikleri alışverişe ekle" else "Add remaining shortages to shopping", color = colors.primary)\n            }\n            when (val substitution = state.substitutionState) {\n''',
    "Operations shopping action",
)

# MainActivity state + callbacks.
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/MainActivity.kt",
    '''    val shoppingImportState by viewModel.shoppingImportState.collectAsState()\n''',
    '''    val shoppingImportState by viewModel.shoppingImportState.collectAsState()\n    val shoppingList by viewModel.shoppingList.collectAsState()\n''',
    "MainActivity shopping collect",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/MainActivity.kt",
    '''                            shoppingImportState = shoppingImportState,\n                            scannedIngredients = scannedIngredients,\n''',
    '''                            shoppingImportState = shoppingImportState,\n                            shoppingList = shoppingList,\n                            scannedIngredients = scannedIngredients,\n''',
    "Home shopping state wiring",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/MainActivity.kt",
    '''                            onClearShoppingImport = viewModel::clearShoppingImport,\n                            onConfigureGemini = onConfigureGemini,\n''',
    '''                            onClearShoppingImport = viewModel::clearShoppingImport,\n                            onToggleShoppingItem = viewModel::setShoppingItemChecked,\n                            onDeleteShoppingItem = viewModel::deleteShoppingItem,\n                            onClearCheckedShoppingItems = viewModel::clearCheckedShoppingItems,\n                            onConfigureGemini = onConfigureGemini,\n''',
    "Home shopping callbacks wiring",
)
replace_once(
    "app-android/src/main/java/com/agentickitchen/android/MainActivity.kt",
    '''                    onApplySubstitution = viewModel::applyPantrySubstitution,\n                    onDismissSubstitution = viewModel::dismissPantrySubstitution\n''',
    '''                    onApplySubstitution = viewModel::applyPantrySubstitution,\n                    onDismissSubstitution = viewModel::dismissPantrySubstitution,\n                    onAddShortagesToShoppingList = viewModel::addCurrentShortagesToShoppingList\n''',
    "Operations shopping callback wiring",
)
