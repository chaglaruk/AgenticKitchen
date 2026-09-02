package com.agentickitchen.android.ui

import com.agentickitchen.shared.cooking.CookingSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationsDisplayStateTest {
    @Test
    fun idleReadyStateDoesNotRenderCookingPanel() {
        assertFalse(shouldShowCookingPanel(CookingSessionStatus.READY, false))
    }

    @Test
    fun readyStateWithActivePlanRendersCookingPanel() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.READY, true))
    }

    @Test
    fun runningAndPausedStatesRenderWithoutPlanContext() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.RUNNING, false))
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.PAUSED, false))
    }

    @Test
    fun terminalStatesWithoutActivePlanDoNotRenderCookingPanel() {
        assertFalse(shouldShowCookingPanel(CookingSessionStatus.COMPLETED, false))
        assertFalse(shouldShowCookingPanel(CookingSessionStatus.ENDED, false))
    }

    @Test
    fun terminalStatesWithActivePlanRenderCookingPanel() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.COMPLETED, true))
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.ENDED, true))
    }

    @Test
    fun errorStateRemainsVisibleWithoutPlanContext() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.ERROR, false))
    }
}
