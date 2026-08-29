package com.agentickitchen.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavigationTest {
    @Test
    fun setupBackIsInterceptedOnlyWhenEditingCompletedSetup() {
        assertFalse(shouldHandleSetupBack(setupDone = false, isEditingSetup = true))
        assertFalse(shouldHandleSetupBack(setupDone = true, isEditingSetup = false))
        assertTrue(shouldHandleSetupBack(setupDone = true, isEditingSetup = true))
    }

    @Test
    fun secondaryScreensReturnToKitchenRoot() {
        assertSame(Screen.Intelligence, backDestination(Screen.Options, hasActiveRecipe = false))
        assertSame(Screen.Intelligence, backDestination(Screen.History, hasActiveRecipe = false))
        assertSame(Screen.Intelligence, backDestination(Screen.Settings, hasActiveRecipe = false))
    }

    @Test
    fun operationsReturnsToOptionsOnlyWhenRecipeContextIsActive() {
        assertSame(Screen.Options, backDestination(Screen.Operations, hasActiveRecipe = true))
        assertSame(Screen.Intelligence, backDestination(Screen.Operations, hasActiveRecipe = false))
    }

    @Test
    fun kitchenRootRemainsTheRootDestination() {
        assertSame(Screen.Intelligence, backDestination(Screen.Intelligence, hasActiveRecipe = false))
    }
}
