package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SimpleTimingAgentTest {
    private val fmt: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @Test
    fun `preheat only schedules correctly`() = runBlocking {
        val agent = SimpleTimingAgent()
        val target = "2026-04-23T17:15:00+03:00"
        val request = ScheduleRequest(
            sessionId = "s1",
            targetTimeIso = target,
            ingredients = emptyList(),
            hardwareProfileId = "hp1",
            steps = listOf(RecipeStep(id = "preheat", type = "preheat", resource = "oven", durationSec = 600))
        )

        val result = agent.computeSchedule(request)
        assertEquals(1, result.events.size)
        val e = result.events.first()
        val expectedStart = ZonedDateTime.parse(target, fmt).minusSeconds(600).toInstant()
        val actualStart = ZonedDateTime.parse(e.startIso, fmt).toInstant()
        assertEquals(expectedStart.epochSecond, actualStart.epochSecond)
    }

    @Test
    fun `two same-resource tasks serialize`() = runBlocking {
        val agent = SimpleTimingAgent()
        val target = "2026-04-23T17:00:00+03:00"
        val steps = listOf(
            RecipeStep(id = "a", type = "cook", resource = "oven", durationSec = 600),
            RecipeStep(id = "b", type = "cook", resource = "oven", durationSec = 600)
        )
        val request = ScheduleRequest("s2", target, emptyList(), "hp1", steps)
        val result = agent.computeSchedule(request)
        assertEquals(2, result.events.size)
        val ends = result.events.map { ZonedDateTime.parse(it.endIso, fmt).toInstant().epochSecond }.sortedDescending()
        // first ends at target, second ends at target - 600s
        val targetEpoch = ZonedDateTime.parse(target, fmt).toInstant().epochSecond
        assertEquals(targetEpoch, ends[0])
        assertEquals(targetEpoch - 600, ends[1])
    }

    @Test
    fun `different resources can overlap`() = runBlocking {
        val agent = SimpleTimingAgent()
        val target = "2026-04-23T18:00:00+03:00"
        val steps = listOf(
            RecipeStep(id = "ovenTask", type = "roast", resource = "oven", durationSec = 600),
            RecipeStep(id = "stoveTask", type = "saute", resource = "stove", durationSec = 600)
        )
        val request = ScheduleRequest("s3", target, emptyList(), "hp1", steps)
        val result = agent.computeSchedule(request)
        assertEquals(2, result.events.size)
        val ends = result.events.map { ZonedDateTime.parse(it.endIso, fmt).toInstant().epochSecond }
        val targetEpoch = ZonedDateTime.parse(target, fmt).toInstant().epochSecond
        // both should end at target
        assertTrue(ends.contains(targetEpoch))
    }

    @Test
    fun `preheat before cook dependency`() = runBlocking {
        val agent = SimpleTimingAgent()
        val target = "2026-04-23T19:00:00+03:00"
        val steps = listOf(
            RecipeStep(id = "preheat", type = "preheat", resource = "oven", durationSec = 600),
            RecipeStep(id = "cook", type = "cook", resource = "oven", durationSec = 900, dependsOn = listOf("preheat"))
        )
        val request = ScheduleRequest("s4", target, emptyList(), "hp1", steps)
        val result = agent.computeSchedule(request)
        assertEquals(2, result.events.size)
        val cook = result.events.first { it.id == "cook" }
        val preheat = result.events.first { it.id == "preheat" }
        val cookStart = ZonedDateTime.parse(cook.startIso, fmt).toInstant().epochSecond
        val preheatEnd = ZonedDateTime.parse(preheat.endIso, fmt).toInstant().epochSecond
        assertEquals(cookStart, preheatEnd)
    }
}
