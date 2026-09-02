package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookingSessionControllerTest {
    private class Clock(var value: Long = 0) : ClockDomain { override fun monotonicMillis() = value; override fun epochMillis() = value }
    private fun event(id: String, start: Long, end: Long) = ScheduleEvent(id, "2026-01-01T00:00:${start.toString().padStart(2,'0')}Z", "2026-01-01T00:00:${end.toString().padStart(2,'0')}Z", id, "stove")
    @Test fun `parallel completion skip pause and finish`() { val clock=Clock(); val c=CookingSessionController(clock); assertEquals(2,c.start("x",listOf(event("a",0,10),event("b",0,20))).active.size); clock.value=5000; assertEquals(5,c.current().active.first{it.event.id=="a"}.remainingSeconds); c.complete("a"); assertTrue(c.current().active.any{it.event.id=="b"}); c.skip("b"); assertTrue(c.current().skipped.contains("b")); assertEquals(CookingSessionStatus.COMPLETED,c.current().status) }
    @Test fun `scheduler zoned timestamps start cooking`() { val c=CookingSessionController(Clock()); val event=ScheduleEvent("a","2026-01-01T12:00:00+03:00[Europe/Istanbul]","2026-01-01T12:00:10+03:00[Europe/Istanbul]","a","stove"); assertEquals(CookingSessionStatus.RUNNING,c.start("x",listOf(event)).status) }
    @Test fun `future pause errors and invalid plans`() { val clock=Clock(); val c=CookingSessionController(clock); assertTrue(c.pause().error != null); assertTrue(c.start("x",emptyList()).error != null); assertTrue(c.start("x",listOf(event("a",10,5))).error != null); val s=c.start("x",listOf(event("a",0,10),event("b",10,20))); assertEquals(1,s.active.size); c.pause(); clock.value=9000; assertEquals(0,c.current().elapsedSeconds); c.resume(); assertEquals(0,c.current().elapsedSeconds); assertTrue(c.complete("missing").error != null) }
}
