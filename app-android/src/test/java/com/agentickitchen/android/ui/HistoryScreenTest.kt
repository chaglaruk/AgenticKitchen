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

    @Test fun localizesPersistedIngredientUnitsForTurkishHistory() {
        assertEquals(
            "150.0 g Makarna, 1.0 adet Soğan, 2.0 diş Sarımsak, 1.0 paket Ekmek",
            localizeHistoryIngredients(
                "150.0 g Makarna, 1.0 pieces Soğan, 2.0 clove Sarımsak, 1.0 package Ekmek",
                true
            )
        )
    }

    @Test fun localizesPersistedIngredientUnitsForEnglishHistory() {
        assertEquals(
            "150.0 g Pasta, 1.0 piece Onion, 2.0 clove Garlic, 1.0 package Bread",
            localizeHistoryIngredients(
                "150.0 g Pasta, 1.0 adet Onion, 2.0 diş Garlic, 1.0 paket Bread",
                false
            )
        )
    }

    @Test fun extractsIngredientNamesForDraftReuseAcrossKnownUnits() {
        assertEquals(
            listOf("Pirinç", "Domates", "Sarımsak", "Ekmek", "Maydanoz"),
            historyIngredientsForReuse("200 g Pirinç, 2 piece Domates, 1 clove Sarımsak, 1 package Ekmek, 1 bunch Maydanoz")
        )
    }
}
