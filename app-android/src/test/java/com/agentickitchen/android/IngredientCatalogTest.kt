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
        assertTrue(INGREDIENT_CATALOG.size >= 190)
        assertTrue(INGREDIENT_CATEGORIES.size >= 12)
        assertEquals(INGREDIENT_CATALOG.size, INGREDIENT_CATALOG.map { it.id }.toSet().size)
        assertTrue(INGREDIENT_CATALOG.all { it.nameTr.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(INGREDIENT_CATALOG.map { it.visualKind }.toSet().size >= 24)
        assertTrue(INGREDIENT_CATALOG.none { it.visualKind == IngredientVisualKind.PANTRY })
        assertTrue(INGREDIENT_CATALOG.none { it.nameTr == "Beyaz balık" })
        assertTrue(listOf("Patates", "Tatlı patates", "Kültür mantarı").all { name -> catalogIngredientForName(name) != null })
    }

    @Test
    fun normalizedSearchSupportsTurkishEnglishAliasesAndExclusions() {
        assertEquals("Soğan", searchIngredientCatalog("sogan", emptyList(), true).first().nameTr)
        assertEquals("Yoğurt", searchIngredientCatalog("yoğ", emptyList(), true).first().nameTr)
        assertEquals("Soft cheese", searchIngredientCatalog("soft cheese", emptyList(), false).first().nameEn)
        assertEquals("Bicarbonate of soda", searchIngredientCatalog("baking soda", emptyList(), false).first().nameEn)
        assertEquals("Chicken breast", searchIngredientCatalog("chick", emptyList(), false).first().nameEn)
        assertFalse(searchIngredientCatalog("chick", listOf("Chicken breast"), false).any { it.nameEn == "Chicken breast" })
        assertTrue(searchIngredientCatalog("tavuk", emptyList(), true).take(3).all { "Tavuk" in it.nameTr || it.nameTr == "Bütün tavuk" })
        assertTrue(searchIngredientCatalog("balık", emptyList(), true).any { it.nameTr == "Somon" })
        assertEquals("Patates", searchIngredientCatalog("patates", emptyList(), true).first().nameTr)
        assertTrue(searchIngredientCatalog("mantar", emptyList(), true).all { "mantar" in it.nameTr.lowercase() })
        assertEquals("Potato", searchIngredientCatalog("potato", emptyList(), false).first().nameEn)
        assertTrue(searchIngredientCatalog("mushroom", emptyList(), false).all { "mushroom" in it.nameEn.lowercase() })
    }

    @Test
    fun currentLanguageWinsAndKnownManualInputIsCanonicalized() {
        assertEquals("Kültür mantarı", canonicalIngredientName("button mushrooms", true))
        assertEquals("Chicken breast", canonicalIngredientName("Tavuk göğsü", false))
        assertEquals("annemin sosu", canonicalIngredientName("  annemin sosu  ", true))
        assertEquals("Pide", searchIngredientCatalog("pide", emptyList(), true).first().nameTr)
        assertEquals("Pita bread", searchIngredientCatalog("pita", emptyList(), false).first().nameEn)
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
        assertEquals(IngredientVisualKind.POTATO, ingredientVisualFor("Patates"))
        assertEquals(IngredientVisualKind.POTATO, ingredientVisualFor("Sweet potato"))
        assertEquals(IngredientVisualKind.MUSHROOM, ingredientVisualFor("Kültür mantarı"))
        assertEquals(IngredientVisualKind.MUSHROOM, ingredientVisualFor("Oyster mushrooms"))
        assertEquals(IngredientVisualKind.PANTRY, ingredientVisualFor("annemin sosu"))
    }
}
