package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import com.agentickitchen.shared.models.PantryIntelSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PantryAndThemeUiTest {
    @Test
    fun themeIdsResolveToTheApprovedVisualSystems() {
        assertEquals(ThemePreference.MODERN_MINIMAL_A.storageValue, themeSpec("editorial-light").id)
        assertEquals(ThemePreference.LUXE_APPLIANCE_DARK_K.storageValue, themeSpec("editorial-dark").id)
        assertFalse(themeSpec("editorial-dark").isLight)
        assertEquals(ThemePreference.FOLLOW_SYSTEM, ThemePreference.fromStored("unknown"))
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
