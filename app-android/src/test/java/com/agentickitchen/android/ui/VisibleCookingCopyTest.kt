package com.agentickitchen.android.ui

import com.agentickitchen.shared.cooking.CookingSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleCookingCopyTest {
    @Test
    fun `offline source label is localized for visible recipe cards`() {
        assertEquals("ÇEVRİMDIŞI", localizedRecipeSourceLabel("Offline", true))
        assertEquals("OFFLINE", localizedRecipeSourceLabel("Offline", false))
        assertEquals("Google Gemini", localizedRecipeSourceLabel("Google Gemini", true))
        assertNull(localizedRecipeSourceLabel(null, true))
        assertNull(localizedRecipeSourceLabel("", true))
        assertNull(localizedRecipeSourceLabel("   ", false))
    }

    @Test
    fun `dependency labels never expose internal step ids`() {
        assertEquals("Önceki adımın ardından", cookingDependencyLabel(1, true))
        assertEquals("Önceki adımlar tamamlanınca", cookingDependencyLabel(2, true))
        assertEquals("After the previous step", cookingDependencyLabel(1, false))
        assertEquals("After the previous steps", cookingDependencyLabel(3, false))
        assertNull(cookingDependencyLabel(0, true))
        assertNull(cookingDependencyLabel(-1, false))
    }

    @Test
    fun `scheduled waits keep session controls while cooking remains active`() {
        assertTrue(shouldShowReferenceScheduledWaitControls(CookingSessionStatus.RUNNING, hasActiveOperation = false))
        assertTrue(shouldShowReferenceScheduledWaitControls(CookingSessionStatus.PAUSED, hasActiveOperation = false))
        assertFalse(shouldShowReferenceScheduledWaitControls(CookingSessionStatus.RUNNING, hasActiveOperation = true))
        assertFalse(shouldShowReferenceScheduledWaitControls(CookingSessionStatus.READY, hasActiveOperation = false))
        assertFalse(shouldShowReferenceScheduledWaitControls(CookingSessionStatus.COMPLETED, hasActiveOperation = false))
    }

    @Test
    fun `plan units are localized before reaching visible cooking UI`() {
        assertEquals("adet", localizedPlanUnit("piece", true))
        assertEquals("adet", localizedPlanUnit("pieces", true))
        assertEquals("diş", localizedPlanUnit("clove", true))
        assertEquals("paket", localizedPlanUnit("package", true))
        assertEquals("demet", localizedPlanUnit("bunch", true))
        assertEquals("çay kaşığı", localizedPlanUnit("tsp", true))
        assertEquals("yemek kaşığı", localizedPlanUnit("tbsp", true))
        assertEquals("su bardağı", localizedPlanUnit("cup", true))
        assertEquals("piece", localizedPlanUnit("adet", false))
        assertEquals("package", localizedPlanUnit("paket", false))
        assertEquals("g", localizedPlanUnit("g", true))
        assertEquals("ml", localizedPlanUnit("ml", true))
        assertEquals("kg", localizedPlanUnit(" kg. ", true))
    }
}
