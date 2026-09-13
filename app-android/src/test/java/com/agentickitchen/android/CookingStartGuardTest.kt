package com.agentickitchen.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingStartGuardTest {
    @Test fun preparedRecipeCanStartOnlyAfterShortagesAreResolved() {
        assertTrue(canStartPreparedCooking(emptyList()))
        assertFalse(canStartPreparedCooking(listOf("Milk")))
        assertFalse(canStartPreparedCooking(listOf("Milk", "Onion")))
    }
}
