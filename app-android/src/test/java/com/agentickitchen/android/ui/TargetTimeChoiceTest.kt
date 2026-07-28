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
        assertEquals(TargetTimeChoice.Exact(LocalTime.of(19, 30)), exactTargetTimeChoice("19:30"))
        assertNull(exactTargetTimeChoice("19.30"))
    }

    @Test fun recipeRequestKeepsTypedTimeAndBoundsServings() {
        assertEquals(
            TargetTimeChoice.After(Duration.ofMinutes(45)),
            recipeRequestSelection(4, TargetTimeChoice.After(Duration.ofMinutes(45))).targetTime
        )
        assertEquals(12, recipeRequestSelection(99, TargetTimeChoice.Flexible).servings)
    }
}
