from pathlib import Path


def replace_once(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Anchor not found in {path}: {old[:140]!r}')
    if text.count(old) != 1:
        raise SystemExit(f'Anchor not unique in {path}: {text.count(old)} matches')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


# The legacy test used cup as the sentinel unknown unit. Cup is intentionally supported now.
replace_once(
    'shared/src/test/kotlin/com/agentickitchen/shared/inventory/RecipeImportPantryPlannerTest.kt',
    'ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "cup", "milk")),',
    'ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "ladle", "milk")),'
)

# Theme-safe colors and editable-unit null handling in generated review UI.
p = Path('app-android/src/main/java/com/agentickitchen/android/ui/RecipeImportDialog.kt')
text = p.read_text(encoding='utf-8')
text = text.replace('colors.error', 'MaterialTheme.colors.error')
text = text.replace('colors.warning', 'colors.accent')
text = text.replace('item.copy(unit = value.ifBlank { null })', 'item.copy(unit = value.trim().takeIf(String::isNotEmpty))')
text = text.replace(
    'import com.agentickitchen.shared.inventory.PantryStockItem\n',
    'import com.agentickitchen.shared.inventory.PantryStockItem\nimport com.agentickitchen.shared.inventory.LocalIngredientResolver\n'
)
text = text.replace(
    'if (i == index) item.copy(displayName = value) else item',
    'if (i == index) item.copy(displayName = value, canonicalIngredientId = LocalIngredientResolver.resolveCanonicalId(value)) else item'
)
p.write_text(text, encoding='utf-8')

# Imported culinary volume units should reach the scheduler as normalized base quantities.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''    private fun quantityToGrams(quantity: Double, unit: String): Int = when (unit.lowercase()) {
        "kg" -> (quantity * 1000).toInt()
        "g" -> quantity.toInt()
        "ml" -> quantity.toInt()
        "l" -> (quantity * 1000).toInt()
        else -> quantity.toInt().coerceAtLeast(1)
    }
''',
    '''    private fun quantityToGrams(quantity: Double, unit: String): Int =
        runCatching { InventoryUnits.normalize(quantity, unit).quantity.toInt().coerceAtLeast(1) }
            .getOrElse { quantity.toInt().coerceAtLeast(1) }
'''
)

# Once a plan is prepared, dismiss the import modal state; source metadata remains on the active recipe.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''                    _planState.value = activeRecipeState(
                        sessionId,
                        option,
                        schedule.events,
                        servings,
                        readyTimeIso,
                        plan,
                        usagePlan.usages,
                        usagePlan.shortages
                    )
                    _recipeImportState.value = RecipeImportState.Review(normalizedResponse, importedPantry)
                    persistActiveSession()
''',
    '''                    _planState.value = activeRecipeState(
                        sessionId,
                        option,
                        schedule.events,
                        servings,
                        readyTimeIso,
                        plan,
                        usagePlan.usages,
                        usagePlan.shortages
                    )
                    _recipeImportState.value = RecipeImportState.Idle
                    persistActiveSession()
'''
)

# Imported recipes do not have a generated-options screen to return to; return to Kitchen instead.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class DraftIngredientRemoved(
''',
    '''sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data object NavigateKitchen : UiEvent()
    data class DraftIngredientRemoved(
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''            inventoryRepository.deleteActiveSession(currentState.sessionId)
            _cookingState.value = CookingSessionState()
            if (lastOptions.isNotEmpty()) {
''',
    '''            inventoryRepository.deleteActiveSession(currentState.sessionId)
            _cookingState.value = CookingSessionState()
            if (currentState.recipe.type == "imported") {
                _planState.value = PlanState.Idle
                viewModelScope.launch { _uiEvent.emit(UiEvent.NavigateKitchen) }
                return
            }
            if (lastOptions.isNotEmpty()) {
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''                is UiEvent.ShowSnackbar -> scaffoldState.snackbarHostState.showSnackbar(event.message)
                is UiEvent.DraftIngredientRemoved -> {
''',
    '''                is UiEvent.ShowSnackbar -> scaffoldState.snackbarHostState.showSnackbar(event.message)
                UiEvent.NavigateKitchen -> currentScreen = Screen.Intelligence
                is UiEvent.DraftIngredientRemoved -> {
'''
)
