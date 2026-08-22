package com.agentickitchen.android.ui

import com.agentickitchen.shared.scheduler.TargetTimeChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class TargetTimeChoiceTest {
    @Test fun mapsTypedPresetChoices() {
        val choices = targetTimePresetOptions(isTurkish = false).associateBy { it.id }

        assertEquals(TargetTimeChoice.After(Duration.ofMinutes(20)), choices.getValue("after_20").choice)
        assertEquals(TargetTimeChoice.After(Duration.ofMinutes(45)), choices.getValue("after_45").choice)
        assertEquals(TargetTimeChoice.After(Duration.ofHours(1)), choices.getValue("after_60").choice)
        assertEquals(TargetTimeChoice.ThisEvening, choices.getValue("evening").choice)
        assertEquals(TargetTimeChoice.Flexible, choices.getValue("flexible").choice)
    }

    @Test fun mapsOnlyValidExactTimeInput() {
        assertEquals(TargetTimeChoice.Exact(LocalTime.MIDNIGHT), exactTargetTimeChoice("00:00"))
        assertEquals(TargetTimeChoice.Exact(LocalTime.of(9, 5)), exactTargetTimeChoice("09:05"))
        assertEquals(TargetTimeChoice.Exact(LocalTime.of(23, 59)), exactTargetTimeChoice("23:59"))
        assertNull(exactTargetTimeChoice("24:00"))
        assertNull(exactTargetTimeChoice("12:60"))
        assertNull(exactTargetTimeChoice("99:99"))
        assertNull(exactTargetTimeChoice("1930"))
        assertNull(exactTargetTimeChoice("19:3"))
    }

    @Test fun formatsExactTimeDigitsWithAColon() {
        assertEquals("09:05", formatExactTimeInput("0905"))
        assertEquals("23:59", formatExactTimeInput("23:59"))
        assertEquals("12", formatExactTimeInput("12"))
        assertEquals("12:3", formatExactTimeInput("12x3"))
    }

    @Test fun recipeRequestKeepsTypedTimeAndBoundsServings() {
        assertEquals(
            TargetTimeChoice.After(Duration.ofMinutes(45)),
            recipeRequestSelection(4, TargetTimeChoice.After(Duration.ofMinutes(45))).targetTime
        )
        assertEquals(12, recipeRequestSelection(99, TargetTimeChoice.Flexible).servings)
    }

    @Test fun localizesKnownRecipeDifficultyLabels() {
        assertEquals("KOLAY", recipeTypeLabel("EASY", isTurkish = true))
        assertEquals("ORTA", recipeTypeLabel("medium", isTurkish = true))
        assertEquals("ZOR", recipeTypeLabel("Hard", isTurkish = true))
        assertEquals("EASY", recipeTypeLabel("easy", isTurkish = false))
        assertEquals("TAVA YEMEĞİ", recipeTypeLabel("Tava Yemeği", isTurkish = true))
    }
}
