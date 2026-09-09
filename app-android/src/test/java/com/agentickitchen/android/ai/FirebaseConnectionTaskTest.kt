package com.agentickitchen.android.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseConnectionTaskTest {
    @Test
    fun `managed connection probe uses reasoning capability`() {
        assertEquals(FirebaseAiTask.REASONING, FirebaseResponseKind.CONNECTION_TEST.task)
    }
}
