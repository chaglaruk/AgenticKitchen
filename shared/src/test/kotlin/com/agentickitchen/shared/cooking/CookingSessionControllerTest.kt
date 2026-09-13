package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookingSessionControllerTest {
    private class Clock(var value: Long = 0) : ClockDomain {
        override fun monotonicMillis() = value
        override fun epochMillis() = value
    }

    private fun event(id: String, start: Long, end: Long) = ScheduleEvent(
        id,
        "2026-01-01T00:00:${start.toString().padStart(2, '0')}Z",
        "2026-01-01T00:00:${end.toString().padStart(2, '0')}Z",
        id,
        "stove"
    )

    @Test
    fun `parallel completion skip pause and finish`() {
        val clock = Clock()
        val c = CookingSessionController(clock)
        assertEquals(2, c.start("x", listOf(event("a", 0, 10), event("b", 0, 20))).active.size)
        clock.value = 5000
        assertEquals(5, c.current().active.first { it.event.id == "a" }.remainingSeconds)
        c.complete("a")
        assertTrue(c.current().active.any { it.event.id == "b" })
        c.skip("b")
        assertTrue(cookingVisibleSkipped(c.current().skipped).contains("b"))
        assertEquals(CookingSessionStatus.COMPLETED, c.current().status)
    }

    @Test
    fun `scheduler zoned timestamps start cooking`() {
        val c = CookingSessionController(Clock())
        val event = ScheduleEvent(
            "a",
            "2026-01-01T12:00:00+03:00[Europe/Istanbul]",
            "2026-01-01T12:00:10+03:00[Europe/Istanbul]",
            "a",
            "stove"
        )
        assertEquals(CookingSessionStatus.RUNNING, c.start("x", listOf(event)).status)
    }

    @Test
    fun `future pause errors and invalid plans`() {
        val clock = Clock()
        val c = CookingSessionController(clock)
        assertTrue(c.pause().error != null)
        assertTrue(c.start("x", emptyList()).error != null)
        assertTrue(c.start("x", listOf(event("a", 10, 5))).error != null)
        c.start("x", listOf(event("a", 0, 10), event("b", 10, 20)))
        assertEquals(1, c.current().active.size)
        c.pause()
        clock.value = 9000
        assertEquals(0, c.current().elapsedSeconds)
        c.resume()
        assertEquals(0, c.current().elapsedSeconds)
        assertTrue(c.complete("missing").error != null)
    }

    @Test
    fun `plus one minute extends active step and delays subsequent schedule`() {
        val clock = Clock()
        val c = CookingSessionController(clock)
        c.start("x", listOf(event("a", 0, 10), event("b", 10, 20)))
        clock.value = 5_000
        assertEquals(5, c.current().active.single().remainingSeconds)

        val extended = c.complete(cookingAddMinuteCommand("a"))

        assertEquals(CookingSessionStatus.RUNNING, extended.status)
        assertEquals(65, extended.active.single().remainingSeconds)
        assertTrue(cookingVisibleCompleted(extended.completed).isEmpty())
        assertTrue(cookingVisibleSkipped(extended.skipped).isEmpty())
        clock.value = 11_000
        val afterOriginalBoundary = c.current()
        assertEquals("a", afterOriginalBoundary.active.single().event.id)
        assertEquals(59, afterOriginalBoundary.active.single().remainingSeconds)
        assertTrue(afterOriginalBoundary.upcoming.any { it.id == "b" })
    }

    @Test
    fun `timer extension survives restore through persisted metadata`() {
        val clock = Clock()
        val original = CookingSessionController(clock)
        original.start("x", listOf(event("a", 0, 10), event("b", 10, 20)))
        clock.value = 5_000
        val extended = original.complete(cookingAddMinuteCommand("a"))
        val persistedSkipped = extended.skipped

        val restoredClock = Clock(5_000)
        val restored = CookingSessionController(restoredClock).restore(
            recipe = "x",
            schedule = listOf(event("a", 0, 10), event("b", 10, 20)),
            status = CookingSessionStatus.PAUSED,
            startedAtMillis = 0,
            accumulatedElapsedSeconds = extended.elapsedSeconds,
            pausedAtMillis = 5_000,
            skipped = persistedSkipped
        )

        assertEquals(CookingSessionStatus.PAUSED, restored.status)
        assertEquals(65, restored.active.single().remainingSeconds)
        assertTrue(cookingVisibleSkipped(restored.skipped).isEmpty())
    }

    @Test
    fun `plus one minute works while paused without advancing elapsed time`() {
        val clock = Clock()
        val c = CookingSessionController(clock)
        c.start("x", listOf(event("a", 0, 10)))
        clock.value = 5_000
        c.pause()
        val extended = c.complete(cookingAddMinuteCommand("a"))
        assertEquals(CookingSessionStatus.PAUSED, extended.status)
        assertEquals(65, extended.active.single().remainingSeconds)
        clock.value = 50_000
        assertEquals(65, c.current().active.single().remainingSeconds)
        assertEquals(5, c.current().elapsedSeconds)
    }
}
