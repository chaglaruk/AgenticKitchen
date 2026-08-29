package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryScreenTest {
    @Test
    fun statusLabelsCoverPersistedStates() {
        L.applyLanguage(L.English)

        assertEquals("Started", historyStatusLabel("started"))
        assertEquals("Completed", historyStatusLabel("completed"))
        assertEquals("Cancelled", historyStatusLabel("cancelled"))
        assertEquals("Cancelled", historyStatusLabel("canceled"))
        assertEquals("Ended", historyStatusLabel("ended"))
    }

    @Test
    fun malformedTimestampRemainsVisibleInsteadOfCrashing() {
        assertEquals("not-a-date", historyDateLabel("not-a-date"))
    }

    @Test fun conservativelyNormalizesKnownLegacyMixedLanguageNames() {
        assertEquals("Pirinç ve Domates Tavası", normalizeLegacyRecipeName("rice ve tomato Tavası", true))
        assertEquals("Rice and Tomato Sauté", normalizeLegacyRecipeName("pirinç ve domates Tavası", false))
        assertEquals("annemin ve özel Tavası", normalizeLegacyRecipeName("annemin ve özel Tavası", true))
    }

    @Test fun extractsIngredientNamesForDraftReuse() {
        assertEquals(listOf("Pirinç", "Domates", "Sarımsak"), historyIngredientsForReuse("200 g Pirinç, 2 piece Domates, 1 diş Sarımsak"))
    }
}
