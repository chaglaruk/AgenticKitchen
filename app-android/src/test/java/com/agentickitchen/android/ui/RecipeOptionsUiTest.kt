package com.agentickitchen.android.ui

import com.agentickitchen.android.RecipeOption
import com.agentickitchen.shared.inventory.RecipeMatchTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeOptionsUiTest {
    @Test fun coverageSummaryAccountsForEveryDisplayedTier() {
        val options = listOf(
            RecipeOption("ready", "easy", "Ready", "", matchTier = RecipeMatchTier.READY_NOW, pantryCoveragePercent = 100),
            RecipeOption("one", "easy", "One", "", matchTier = RecipeMatchTier.MISSING_ONE, pantryCoveragePercent = 50),
            RecipeOption("idea", "easy", "Idea", "", matchTier = RecipeMatchTier.AI_IDEA, pantryCoveragePercent = 0, canPrepareFromPantry = false)
        )

        assertEquals(
            "3 results · 1 ready · 1 missing one · 0 missing two · 1 AI ideas",
            recipeCoverageSummary(options, isTurkish = false)
        )
        assertEquals(
            "3 sonuç · 1 hazır · 1 tek eksik · 0 iki eksik · 1 fikir",
            recipeCoverageSummary(options, isTurkish = true)
        )
    }

    @Test fun coverageSummaryStaysHiddenForNonPantryOptions() {
        val options = listOf(RecipeOption("idea", "easy", "Idea", ""))
        assertNull(recipeCoverageSummary(options, isTurkish = false))
    }
}
