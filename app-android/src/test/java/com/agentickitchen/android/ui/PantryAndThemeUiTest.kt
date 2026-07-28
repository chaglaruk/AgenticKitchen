package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import com.agentickitchen.shared.models.PantryIntelSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PantryAndThemeUiTest {
    @Test
    fun themeIdsResolveToTheTwoEditorialAppearances() {
        assertEquals("editorial-light", themeSpec("editorial-light").id)
        assertEquals("editorial-dark", themeSpec("editorial-dark").id)
        assertFalse(themeSpec("editorial-dark").isLight)
        (listOf("editorial", "heritage", "zen", "signal", "green", "blue", "orange", "dark", "unknown"))
            .forEach { theme -> assertEquals("editorial-light", themeSpec(theme).id) }
    }

    @Test
    fun pantryCategoriesUseNaturalTurkishAndEnglishLabels() {
        L.applyLanguage(L.Turkish)
        assertEquals("Sebze ve yeşillikler", pantryCategoryLabel("vegetation"))
        assertEquals("Balık ve deniz ürünleri", pantryCategoryLabel("protein_aqua"))
        assertEquals("Et, tavuk ve yumurta", pantryCategoryLabel("protein_land"))
        assertEquals("Tahıllar ve nişastalar", pantryCategoryLabel("carb_matrix"))
        assertEquals("Baharatlar ve aromatikler", pantryCategoryLabel("spice_payload"))
        assertEquals("Sıvılar ve soslar", pantryCategoryLabel("liquids"))
        assertEquals("Diğer", pantryCategoryLabel("unknown"))

        L.applyLanguage(L.English)
        assertEquals("Vegetables and greens", pantryCategoryLabel("vegetation"))
        assertEquals("Fish and seafood", pantryCategoryLabel("protein_aqua"))
        assertEquals("Meat, poultry and eggs", pantryCategoryLabel("protein_land"))
        assertEquals("Grains and starches", pantryCategoryLabel("carb_matrix"))
        assertEquals("Spices and aromatics", pantryCategoryLabel("spice_payload"))
        assertEquals("Liquids and sauces", pantryCategoryLabel("liquids"))
        assertEquals("Other", pantryCategoryLabel("unknown"))
        L.applyLanguage(L.Turkish)
    }

    @Test
    fun pantryGuidanceAvoidsOperationalLanguage() {
        L.applyLanguage(L.English)
        val guidance = listOf(
            "needs_liquid",
            "needs_aromatic",
            "needs_protein",
            "balanced_payload",
            "hybrid_finish_lane",
            "controlled_roast_lane",
            "rapid_pan_lane",
            "adaptive_lane"
        ).map { pantrySignalText(PantryIntelSignal(it, "fallback")) }

        guidance.forEach { text ->
            listOf("lane", "payload", "anchor", "operation", "control", "volatility")
                .forEach { term -> assertFalse(text.contains(term, ignoreCase = true)) }
        }
        L.applyLanguage(L.Turkish)
    }
}
