package com.agentickitchen.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingTextParserTest {
    @Test fun parsesTurkishCommaAndConjunctionListWithoutProvider() {
        val items = requireNotNull(parseShoppingTextLocally("2 paket makarna, 1 kilo tavuk ve 12 yumurta", true))
        assertEquals(listOf("Makarna", "Tavuk", "Yumurta"), items.map { it.displayName })
        assertEquals(listOf("pasta", null, "egg"), items.map { it.canonicalIngredientId })
        assertEquals(listOf("package", "kg", "piece"), items.map { it.unit })
    }

    @Test fun parsesEnglishNewlineListInEnglish() {
        val items = requireNotNull(parseShoppingTextLocally("2 litres milk\n6 tomatoes\n1 kg potatoes", false))
        assertEquals(listOf("Milk", "Tomato", "Potato"), items.map { it.displayName })
        assertEquals(listOf("l", "piece", "kg"), items.map { it.unit })
    }

    @Test fun parsesUnquantifiedKnownListLocallyWithoutInventingAmounts() {
        val items = requireNotNull(parseShoppingTextLocally("milk\nbread\neggs", false))

        assertEquals(listOf("Milk", "Bread", "Egg"), items.map { it.displayName })
        assertEquals(listOf(null, null, null), items.map { it.quantity })
        assertEquals(listOf(null, null, null), items.map { it.unit })
        assertEquals(listOf("unknown", "unknown", "unknown"), items.map { it.unitDimension })
        assertEquals(listOf("Check the amount and unit.", "Check the amount and unit.", "Check the amount and unit."), items.map { it.uncertaintyReason })
    }

    @Test fun parsesQuantifiedGenericIngredientsWithoutInventingSubtypes() {
        val chicken = requireNotNull(parseShoppingTextLocally("1 kilo tavuk", true)).single()
        assertEquals("Tavuk", chicken.displayName)
        assertNull(chicken.canonicalIngredientId)
        assertEquals(1.0, chicken.quantity)
        assertEquals("kg", chicken.unit)

        val fish = requireNotNull(parseShoppingTextLocally("1 kilo balık", true)).single()
        assertEquals("Balık", fish.displayName)
        assertNull(fish.canonicalIngredientId)
        assertEquals(1.0, fish.quantity)
        assertEquals("kg", fish.unit)
    }

    @Test fun parsesUnquantifiedGenericIngredientsAsEditableReviewRows() {
        val items = requireNotNull(parseShoppingTextLocally("tavuk\nbalık\nmantar", true))

        assertEquals(listOf("Tavuk", "Balık", "Mantar"), items.map { it.displayName })
        assertTrue(items.all { it.canonicalIngredientId == null })
        assertTrue(items.all { it.quantity == null && it.unit == null })
        assertEquals(listOf("Miktar ve birimi kontrol et.", "Miktar ve birimi kontrol et.", "Miktar ve birimi kontrol et."), items.map { it.uncertaintyReason })
    }

    @Test fun explicitSpecificNamesStillUseConcreteCatalogIds() {
        val items = requireNotNull(parseShoppingTextLocally("chicken breast\nbutton mushrooms", true))

        assertEquals(listOf("Tavuk göğsü", "Kültür mantarı"), items.map { it.displayName })
        assertEquals(listOf("chicken-breast", "button-mushroom"), items.map { it.canonicalIngredientId })
    }

    @Test fun parsesMixedQuantifiedAndUnquantifiedKnownItemsLocally() {
        val items = requireNotNull(parseShoppingTextLocally("1 kilo tavuk\nbalık\n2 domates", true))

        assertEquals(listOf("Tavuk", "Balık", "Domates"), items.map { it.displayName })
        assertEquals(listOf(null, null, "tomato"), items.map { it.canonicalIngredientId })
        assertEquals(1.0, items[0].quantity)
        assertEquals("kg", items[0].unit)
        assertNull(items[1].quantity)
        assertNull(items[1].unit)
        assertEquals(2.0, items[2].quantity)
        assertEquals("piece", items[2].unit)
    }

    @Test fun returnsNullWhenAnyLineNeedsProviderInterpretation() {
        assertNull(parseShoppingTextLocally("1 kilo tavuk\nbiraz annemin özel sosu", true))
    }
}
