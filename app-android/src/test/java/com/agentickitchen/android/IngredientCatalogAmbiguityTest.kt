package com.agentickitchen.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientCatalogAmbiguityTest {
    @Test
    fun ambiguousGenericAliasesStayGenericWhileSpecificNamesCanonicalize() {
        assertNull(catalogIngredientForName("tavuk"))
        assertNull(catalogIngredientForName("balık"))
        assertNull(catalogIngredientForName("mantar"))

        assertEquals("Tavuk", canonicalIngredientName("tavuk", true))
        assertEquals("Balık", canonicalIngredientName("balık", true))
        assertEquals("Mantar", canonicalIngredientName("mantar", true))
        assertEquals("Chicken", canonicalIngredientName("chicken", false))
        assertEquals("Fish", canonicalIngredientName("fish", false))
        assertEquals("Mushroom", canonicalIngredientName("mushroom", false))

        assertEquals("Tavuk göğsü", canonicalIngredientName("chicken breast", true))
        assertEquals("Chicken breast", canonicalIngredientName("Tavuk göğsü", false))
        assertEquals("Tavuk göğsü", canonicalIngredientName("chicken fillet", true))
        assertEquals("Kültür mantarı", canonicalIngredientName("button mushrooms", true))
    }

    @Test
    fun genericSearchesExposeSpecificChoicesWithoutCreatingGenericCatalogRows() {
        val chickenNames = searchIngredientCatalog("tavuk", emptyList(), true, limit = 10).map { it.nameTr }
        assertTrue(
            listOf("Tavuk göğsü", "Tavuk but", "Tavuk baget", "Tavuk kanadı", "Bütün tavuk")
                .all(chickenNames::contains)
        )

        val fishNames = searchIngredientCatalog("balık", emptyList(), true, limit = 20).map { it.nameTr }
        assertTrue(
            listOf("Somon", "Ton balığı", "Hamsi", "Levrek", "Çipura", "Alabalık", "Uskumru", "Palamut", "Lüfer")
                .all(fishNames::contains)
        )

        val mushroomNames = searchIngredientCatalog("mantar", emptyList(), true, limit = 10).map { it.nameTr }
        assertTrue(listOf("Kültür mantarı", "İstiridye mantarı", "Kestane mantarı").all(mushroomNames::contains))
    }
}
