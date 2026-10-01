package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CookingCountdownRecoveryRoundingTest {
    private class TestClock(
        var monotonic: Long,
        var epoch: Long
    ) : ClockDomain {
        override fun monotonicMillis(): Long = monotonic
        override fun epochMillis(): Long = epoch
    }

    private val baseTime = java.time.Instant.parse("2026-01-01T12:00:00Z")
    private val fixedEpoch = 1767225600000L

    private fun event(durationSeconds: Long) = ScheduleEvent(
        id = "step-1",
        startIso = baseTime.toString(),
        endIso = baseTime.plusSeconds(durationSeconds).toString(),
        instruction = "Cook",
        resource = "stove"
    )

    @Test
    fun `paused visible countdown does not gain a second after whole-second recovery`() {
        val schedule = listOf(event(240))
        val originalClock = TestClock(monotonic = 1_000L, epoch = fixedEpoch)
        val original = CookingSessionController(originalClock)
        original.start("Soup", schedule)

        // Pause 15.250 seconds into the step. Persistence currently stores elapsedSeconds,
        // so recovery reconstructs 15.000 seconds. The displayed countdown must not jump
        // backwards by one second because of that sub-second truncation.
        originalClock.monotonic = 16_250L
        originalClock.epoch = fixedEpoch + 15_250L
        val paused = original.pause()
        val beforeRecovery = paused.active.single().remainingSeconds
        assertEquals(225L, beforeRecovery)
        assertEquals(15L, paused.elapsedSeconds)

        val restoredClock = TestClock(monotonic = 100_000L, epoch = fixedEpoch + 60_000L)
        val restoredController = CookingSessionController(restoredClock)
        val restored = restoredController.restore(
            recipe = "Soup",
            schedule = schedule,
            status = CookingSessionStatus.PAUSED,
            startedAtMillis = fixedEpoch,
            accumulatedElapsedSeconds = paused.elapsedSeconds,
            pausedAtMillis = fixedEpoch + 15_250L
        )

        assertEquals(CookingSessionStatus.PAUSED, restored.status)
        assertEquals(beforeRecovery, restored.active.single().remainingSeconds)

        restoredClock.monotonic += 30_000L
        restoredClock.epoch += 30_000L
        assertEquals(beforeRecovery, restoredController.current().active.single().remainingSeconds)
    }
}
