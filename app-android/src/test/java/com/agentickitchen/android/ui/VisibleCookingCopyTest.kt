package com.agentickitchen.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
