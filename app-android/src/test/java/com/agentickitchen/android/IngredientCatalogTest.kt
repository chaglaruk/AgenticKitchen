package com.agentickitchen.android

import com.agentickitchen.android.ui.IngredientVisualKind
import com.agentickitchen.android.ui.ingredientVisualFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientCatalogTest {
    @Test
    fun catalogIsBroadBilingualAndUniquelyIdentified() {
        assertEquals(156, INGREDIENT_CATALOG.size)
        assertEquals(12, INGREDIENT_CATEGORIES.size)
        assertEquals(INGREDIENT_CATALOG.size, INGREDIENT_CATALOG.map { it.id }.toSet().size)
        assertTrue(INGREDIENT_CATALOG.all { it.nameTr.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(INGREDIENT_CATALOG.map { it.visualKind }.toSet().size >= 24)
        assertTrue(INGREDIENT_CATALOG.none { it.visualKind == IngredientVisualKind.PANTRY })
    }

    @Test
    fun normalizedSearchSupportsTurkishEnglishAliasesAndExclusions() {
        assertEquals("Soğan", searchIngredientCatalog("sogan", emptyList(), true).first().nameTr)
        assertEquals("Yoğurt", searchIngredientCatalog("yoğ", emptyList(), true).first().nameTr)
        assertEquals("Soft cheese", searchIngredientCatalog("soft cheese", emptyList(), false).first().nameEn)
        assertEquals("Bicarbonate of soda", searchIngredientCatalog("baking soda", emptyList(), false).first().nameEn)
        assertEquals("Chicken breast", searchIngredientCatalog("chick", emptyList(), false).first().nameEn)
        assertFalse(searchIngredientCatalog("chick", listOf("Chicken breast"), false).any { it.nameEn == "Chicken breast" })
    }

    @Test
    fun catalogNamesResolveToIntentionalArtworkFamilies() {
        assertEquals(IngredientVisualKind.TURKEY, catalogIngredientForName("Turkey")?.visualKind)
        assertEquals(IngredientVisualKind.RED_MEAT, catalogIngredientForName("Kıyma")?.visualKind)
        assertEquals(IngredientVisualKind.SEAFOOD, catalogIngredientForName("shrimp")?.visualKind)
        assertEquals(IngredientVisualKind.CHEESE, catalogIngredientForName("Mozzarella")?.visualKind)
        assertEquals(IngredientVisualKind.CUCUMBER, catalogIngredientForName("Salatalık")?.visualKind)
        assertEquals(IngredientVisualKind.SQUASH, catalogIngredientForName("Courgette")?.visualKind)
        assertEquals(IngredientVisualKind.FRUIT, catalogIngredientForName("Muz")?.visualKind)
        assertEquals(IngredientVisualKind.OIL, catalogIngredientForName("Olive oil")?.visualKind)
        assertEquals(IngredientVisualKind.FLOUR_BAKING, catalogIngredientForName("Un")?.visualKind)
        assertEquals(IngredientVisualKind.SUGAR_HONEY, catalogIngredientForName("Honey")?.visualKind)
        assertEquals(IngredientVisualKind.TURKEY, ingredientVisualFor("Turkey"))
        assertEquals(IngredientVisualKind.SEAFOOD, ingredientVisualFor("Karides"))
    }
}
