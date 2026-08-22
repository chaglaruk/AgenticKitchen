package com.agentickitchen.android.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiEndpointContractTest {
    @Test
    fun interactionsEndpointUsesCurrentBetaVersion() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta2/interactions",
            GeminiProvider.ENDPOINT
        )
    }
}
