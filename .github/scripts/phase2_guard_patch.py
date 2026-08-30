from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


view_model_path = Path("app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt")
view_model = view_model_path.read_text(encoding="utf-8")
view_model = replace_once(
    view_model,
    '''        if (!canReplacePreparedRecipe(_cookingState.value.status)) {
            emitUiEvent(
                if (L.isTr) "Devam eden pişirmeyi bitirmeden yeni bir tarif hazırlayamazsın."
                else "Finish the current cooking session before preparing another recipe."
            )
            return
        }
        viewModelScope.launch {
''',
    '''        if (!canReplacePreparedRecipe(_cookingState.value.status)) {
            emitUiEvent(
                if (L.isTr) "Devam eden pişirmeyi bitirmeden yeni bir tarif hazırlayamazsın."
                else "Finish the current cooking session before preparing another recipe."
            )
            return
        }
        if (inventoryRecipeRequest != null && !option.canPrepareFromPantry) {
            emitUiEvent(
                if (L.isTr) "Bu fikir mevcut stoktan hazırlanamaz. Eksikleri tamamladıktan sonra tekrar dene."
                else "This idea cannot be prepared from the current pantry. Resolve its shortages and try again."
            )
            return
        }
        viewModelScope.launch {
''',
    "ViewModel pantry idea guard",
)
view_model_path.write_text(view_model, encoding="utf-8")


options_path = Path("app-android/src/main/java/com/agentickitchen/android/ui/OptionsScreen.kt")
options = options_path.read_text(encoding="utf-8")
options = replace_once(
    options,
    '''    val ready = options.count { it.matchTier == RecipeMatchTier.READY_NOW }
    val one = options.count { it.matchTier == RecipeMatchTier.MISSING_ONE }
    val two = options.count { it.matchTier == RecipeMatchTier.MISSING_TWO }
    return if (isTurkish) {
        "${options.size} sonuç · $ready hazır · $one tek eksik · $two iki eksik"
    } else {
        "${options.size} results · $ready ready · $one missing one · $two missing two"
    }
''',
    '''    val ready = options.count { it.matchTier == RecipeMatchTier.READY_NOW }
    val one = options.count { it.matchTier == RecipeMatchTier.MISSING_ONE }
    val two = options.count { it.matchTier == RecipeMatchTier.MISSING_TWO }
    val ai = options.count { it.matchTier == RecipeMatchTier.AI_IDEA }
    return if (isTurkish) {
        "${options.size} sonuç · $ready hazır · $one tek eksik · $two iki eksik · $ai AI fikri"
    } else {
        "${options.size} results · $ready ready · $one missing one · $two missing two · $ai AI ideas"
    }
''',
    "recipe coverage summary",
)
options_path.write_text(options, encoding="utf-8")


test_path = Path("app-android/src/test/java/com/agentickitchen/android/ui/RecipeOptionsUiTest.kt")
test_path.write_text(
    '''package com.agentickitchen.android.ui

import com.agentickitchen.android.RecipeOption
import com.agentickitchen.shared.inventory.RecipeMatchTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeOptionsUiTest {
    @Test fun coverageSummaryAccountsForEveryDisplayedTier() {
        val options = listOf(
            RecipeOption("ready", "easy", "Ready", "", matchTier = RecipeMatchTier.READY_NOW, pantryCoveragePercent = 100),
            RecipeOption("one", "easy", "One", "", matchTier = RecipeMatchTier.MISSING_ONE, pantryCoveragePercent = 50),
            RecipeOption("idea", "easy", "Idea", "", matchTier = RecipeMatchTier.AI_IDEA, pantryCoveragePercent = 0, canPrepareFromPantry = false)
        )

        assertEquals(
            "3 results · 1 ready · 1 missing one · 0 missing two · 1 AI ideas",
            recipeCoverageSummary(options, isTurkish = false)
        )
        assertEquals(
            "3 sonuç · 1 hazır · 1 tek eksik · 0 iki eksik · 1 AI fikri",
            recipeCoverageSummary(options, isTurkish = true)
        )
    }

    @Test fun coverageSummaryStaysHiddenForNonPantryOptions() {
        val options = listOf(RecipeOption("idea", "easy", "Idea", ""))
        assertNull(recipeCoverageSummary(options, isTurkish = false))
    }
}
''',
    encoding="utf-8",
)
