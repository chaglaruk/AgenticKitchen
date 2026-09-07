package com.agentickitchen.android.ui.kitchen

import com.agentickitchen.android.KitchenScanState
import com.agentickitchen.android.RecipeImportState
import com.agentickitchen.android.ShoppingImportState
import com.agentickitchen.shared.inventory.InventoryAdjustmentRecord
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.ShoppingListItem
import com.agentickitchen.shared.models.PantryIntelReport

data class KitchenUiState(
    val chips: List<String>,
    val inventory: List<PantryStockItem>,
    val inventoryAdjustments: Map<String, List<InventoryAdjustmentRecord>>,
    val shoppingList: List<ShoppingListItem>,
    val pantryIntel: PantryIntelReport,
    val scannedIngredients: List<String>?,
    val kitchenScanState: KitchenScanState,
    val shoppingImportState: ShoppingImportState,
    val recipeImportState: RecipeImportState
)

data class KitchenUiActions(
    val onAddChip: (String) -> Unit,
    val onAddMultipleChips: (List<String>) -> Unit,
    val onRemoveChip: (String) -> Unit,
    val onClearAll: () -> Unit,
    val onStart: () -> Unit,
    val onOpenPantryEditor: (PantryStockItem?) -> Unit,
    val onDeleteInventoryItem: (PantryStockItem) -> Unit,
    val onOpenIngredientLibrary: () -> Unit,
    val onOpenIngredientCamera: () -> Unit,
    val onOpenKitchenScan: () -> Unit,
    val onOpenShoppingImport: () -> Unit,
    val onOpenRecipeImport: () -> Unit,
    val onOpenCookWithPantry: () -> Unit,
    val onToggleShoppingItem: (String, Boolean) -> Unit,
    val onDeleteShoppingItem: (String) -> Unit,
    val onClearCheckedShoppingItems: () -> Unit,
    val onEditSetup: () -> Unit
)
