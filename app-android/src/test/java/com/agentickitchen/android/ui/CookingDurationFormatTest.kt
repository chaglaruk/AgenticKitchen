package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CookingDurationFormatTest {
    @Test fun formatsCookingDurations() {
        assertEquals("00:00", formatCookingDuration(0))
        assertEquals("00:07", formatCookingDuration(7))
        assertEquals("01:05", formatCookingDuration(65))
        assertEquals("59:59", formatCookingDuration(3_599))
        assertEquals("01:00:00", formatCookingDuration(3_600))
        assertEquals("01:01:05", formatCookingDuration(3_665))
    }

    @Test fun formatsPlanQuantitiesAndReadyTime() {
        assertEquals("500", formatPlanQuantity(500.0))
        assertEquals("1.25", formatPlanQuantity(1.25))
        assertEquals("19:30", formatReadyTime("2026-07-30T19:30:00+01:00"))
    }
}
