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

    @Test fun returnsNullWhenAnyLineNeedsProviderInterpretation() {
        assertNull(parseShoppingTextLocally("biraz annemin özel sosu", true))
    }
}
