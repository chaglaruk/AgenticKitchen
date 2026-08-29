package com.agentickitchen.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingTextParserTest {
    @Test fun parsesTurkishCommaAndConjunctionListWithoutProvider() {
        val items = requireNotNull(parseShoppingTextLocally("2 paket makarna, 1 kilo tavuk ve 12 yumurta", true))
        assertEquals(listOf("Makarna", "Tavuk göğsü", "Yumurta"), items.map { it.displayName })
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

    @Test fun parsesMixedQuantifiedAndUnquantifiedKnownItemsLocally() {
        val items = requireNotNull(parseShoppingTextLocally("2 domates\npatates", true))

        assertEquals(listOf("Domates", "Patates"), items.map { it.displayName })
        assertEquals(2.0, items[0].quantity)
        assertEquals("piece", items[0].unit)
        assertNull(items[1].quantity)
        assertNull(items[1].unit)
        assertEquals("Miktar ve birimi kontrol et.", items[1].uncertaintyReason)
    }

    @Test fun returnsNullWhenAnyLineNeedsProviderInterpretation() {
        assertNull(parseShoppingTextLocally("biraz annemin özel sosu", true))
    }
}
