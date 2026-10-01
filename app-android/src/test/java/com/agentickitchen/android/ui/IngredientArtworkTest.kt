package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientArtworkTest {
    @Test
    fun `generic Turkish ingredients keep semantically correct artwork`() {
        assertEquals(IngredientVisualKind.CHICKEN, ingredientVisualFor("Tavuk"))
        assertEquals(IngredientVisualKind.MUSHROOM, ingredientVisualFor("Mantar"))
        assertEquals(IngredientVisualKind.POTATO, ingredientVisualFor("Patates"))
        assertEquals(IngredientVisualKind.FISH, ingredientVisualFor("Balık"))
    }
}
