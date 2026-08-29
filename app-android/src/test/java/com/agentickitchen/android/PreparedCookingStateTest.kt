package com.agentickitchen.android

import com.agentickitchen.shared.cooking.CookingSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparedCookingStateTest {
    @Test
    fun `new prepared recipe discards terminal state from previous recipe`() {
        val state = preparedCookingState("Pirinç ve Soğan Tavası")
        assertEquals("Pirinç ve Soğan Tavası", state.recipeName)
        assertEquals(CookingSessionStatus.READY, state.status)
        assertTrue(state.completed.isEmpty())
        assertTrue(state.skipped.isEmpty())
        assertEquals(0L, state.elapsedSeconds)
    }

    @Test
    fun `running or paused cooking cannot be silently replaced`() {
        assertFalse(canReplacePreparedRecipe(CookingSessionStatus.RUNNING))
        assertFalse(canReplacePreparedRecipe(CookingSessionStatus.PAUSED))
        assertTrue(canReplacePreparedRecipe(CookingSessionStatus.READY))
        assertTrue(canReplacePreparedRecipe(CookingSessionStatus.COMPLETED))
    }
}
