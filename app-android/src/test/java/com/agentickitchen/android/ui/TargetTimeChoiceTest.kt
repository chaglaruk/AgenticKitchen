package com.agentickitchen.android.ui

import com.agentickitchen.shared.scheduler.TargetTimeChoice
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class TargetTimeChoiceTest {
    @Test fun mapsRelativeChoices() {
        assertTrue(targetTimeChoice("Now (20m)") is TargetTimeChoice.After)
        assertTrue(targetTimeChoice("In 45 Minutes") is TargetTimeChoice.After)
        assertTrue(targetTimeChoice("In 1 Hour") is TargetTimeChoice.After)
    }

    @Test fun mapsExactTime() {
        assertTrue(targetTimeChoice("19:30") is TargetTimeChoice.Exact)
    }
}
