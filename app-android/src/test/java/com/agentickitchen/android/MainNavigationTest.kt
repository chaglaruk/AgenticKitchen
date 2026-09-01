package com.agentickitchen.android

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.RecipeImportResponse
import com.agentickitchen.shared.ai.RecipeImportSource
import com.agentickitchen.shared.inventory.RecipeImportPantrySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun completedSetupUsesAnOverlaySoNavigationStateStaysMountedDuringEditing() {
        assertEquals(SetupPresentation.INITIAL, setupPresentation(setupDone = false, isEditingSetup = false))
        assertEquals(SetupPresentation.INITIAL, setupPresentation(setupDone = false, isEditingSetup = true))
        assertEquals(SetupPresentation.HIDDEN, setupPresentation(setupDone = true, isEditingSetup = false))
        assertEquals(SetupPresentation.EDIT_OVERLAY, setupPresentation(setupDone = true, isEditingSetup = true))
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

    @Test
    fun recipeImportOwnsNavigationEvenWhenAnOlderRecipeOrConsumptionExists() {
        val review = RecipeImportState.Review(
            response = RecipeImportResponse(
                recipe = ImportedRecipe("Shared recipe", 2, emptyList(), emptyList()),
                confidence = 1.0,
                source = RecipeImportSource.ANDROID_SHARE
            ),
            pantry = RecipeImportPantrySummary(emptyList())
        )

        assertSame(
            Screen.Intelligence,
            automaticDestination(PlanState.RecipeActive(), review, hasPendingConsumption = true)
        )
        assertSame(
            Screen.Intelligence,
            automaticDestination(PlanState.OptionsReady(emptyList()), RecipeImportState.Loading("url"), hasPendingConsumption = false)
        )
    }

    @Test
    fun normalAutomaticNavigationRemainsUnchangedWithoutImport() {
        assertSame(
            Screen.Options,
            automaticDestination(PlanState.OptionsReady(emptyList()), RecipeImportState.Idle, hasPendingConsumption = false)
        )
        assertSame(
            Screen.Operations,
            automaticDestination(PlanState.RecipeActive(), RecipeImportState.Idle, hasPendingConsumption = false)
        )
        assertSame(
            Screen.Operations,
            automaticDestination(PlanState.Idle, RecipeImportState.Idle, hasPendingConsumption = true)
        )
        assertNull(automaticDestination(PlanState.Idle, RecipeImportState.Idle, hasPendingConsumption = false))
    }
}
