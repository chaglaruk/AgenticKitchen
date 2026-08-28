package com.agentickitchen.android.ui

import com.agentickitchen.shared.cooking.CookingSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationsDisplayStateTest {
    @Test
    fun idleReadyStateDoesNotRenderCookingPanel() {
        assertFalse(shouldShowCookingPanel(CookingSessionStatus.READY, "", false))
    }

    @Test
    fun readyStateWithRecipeNameRendersCookingPanel() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.READY, "Tomato Rice", false))
    }

    @Test
    fun readyStateWithActivePlanRendersCookingPanel() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.READY, "", true))
    }

    @Test
    fun nonReadyStateRemainsVisibleWithoutRecipeContext() {
        assertTrue(shouldShowCookingPanel(CookingSessionStatus.ERROR, "", false))
    }
}
