package com.agentickitchen.android

import com.agentickitchen.android.ui.IngredientVisualKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientCatalogTest {
    @Test
    fun catalogIsBroadBilingualAndUniquelyIdentified() {
        assertTrue(INGREDIENT_CATALOG.size >= 150)
        assertTrue(INGREDIENT_CATEGORIES.size >= 12)
        assertEquals(INGREDIENT_CATALOG.size, INGREDIENT_CATALOG.map { it.id }.toSet().size)
        assertTrue(INGREDIENT_CATALOG.all { it.nameTr.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(INGREDIENT_CATALOG.all { it.visualKind != IngredientVisualKind.PANTRY || it.categoryId == "baking_pantry" })
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
}
