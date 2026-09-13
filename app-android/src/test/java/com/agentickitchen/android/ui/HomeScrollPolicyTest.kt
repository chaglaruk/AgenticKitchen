package com.agentickitchen.android.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScrollPolicyTest {
    @Test
    fun `home stops scrolling while autocomplete is expanded`() {
        assertFalse(homeScrollEnabled(autocompleteExpanded = true))
        assertTrue(homeScrollEnabled(autocompleteExpanded = false))
    }
}
