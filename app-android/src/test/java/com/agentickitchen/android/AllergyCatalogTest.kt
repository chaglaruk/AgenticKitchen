package com.agentickitchen.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AllergyCatalogTest {
    @Test
    fun canonicalIdsAndLabelsAreStableAcrossLanguages() {
        assertEquals("milk", AllergyCatalog.canonicalId("Süt"))
        assertEquals("milk", AllergyCatalog.canonicalId("dairy"))
        assertEquals("Süt ve süt ürünleri", AllergyCatalog.label("milk", true))
        assertEquals("Milk and dairy", AllergyCatalog.label("milk", false))
    }

    @Test
    fun customAllergiesAreTrimmedNormalizedAndDeduplicated() {
        val allergies = AllergyCatalog.normalize(setOf("  Mustard  ", "mustard", "MUSTARD"))

        assertEquals(setOf("custom:mustard"), allergies)
        assertNull(AllergyCatalog.normalizeCustom("   "))
        assertEquals(emptySet<String>(), allergies - "custom:mustard")
    }
}
