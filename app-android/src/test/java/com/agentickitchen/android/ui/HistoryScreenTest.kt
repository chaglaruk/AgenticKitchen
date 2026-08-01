package com.agentickitchen.android.ui

import com.agentickitchen.android.L
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryScreenTest {
    @Test
    fun statusLabelsCoverPersistedStates() {
        L.applyLanguage(L.English)

        assertEquals("Started", historyStatusLabel("started"))
        assertEquals("Completed", historyStatusLabel("completed"))
        assertEquals("Cancelled", historyStatusLabel("cancelled"))
        assertEquals("Cancelled", historyStatusLabel("canceled"))
        assertEquals("Ended", historyStatusLabel("ended"))
    }

    @Test
    fun malformedTimestampRemainsVisibleInsteadOfCrashing() {
        assertEquals("not-a-date", historyDateLabel("not-a-date"))
    }
}
