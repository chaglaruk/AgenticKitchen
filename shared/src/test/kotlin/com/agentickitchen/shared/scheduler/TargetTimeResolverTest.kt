package com.agentickitchen.shared.scheduler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TargetTimeResolverTest {

    private val utc = ZoneId.of("UTC")
    private val resolver = TargetTimeResolver(utc)

    @Test
    fun `after 20 minutes`() {
        val now = Instant.parse("2026-07-26T12:00:00Z")
        val choice = TargetTimeChoice.After(Duration.ofMinutes(20))
        val result = resolver.resolve(choice, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26T12:20:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `after 45 minutes`() {
        val now = Instant.parse("2026-07-26T12:00:00Z")
        val choice = TargetTimeChoice.After(Duration.ofMinutes(45))
        val result = resolver.resolve(choice, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26T12:45:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `exact future time same day`() {
        val now = Instant.parse("2026-07-26T12:00:00Z")
        val choice = TargetTimeChoice.Exact(LocalTime.of(18, 30))
        val result = resolver.resolve(choice, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26T18:30:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `exact past time becomes next day`() {
        val now = Instant.parse("2026-07-26T20:00:00Z")
        val choice = TargetTimeChoice.Exact(LocalTime.of(18, 30))
        val result = resolver.resolve(choice, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-27T18:30:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `this evening returns 1900 today`() {
        val now = Instant.parse("2026-07-26T10:00:00Z")
        val result = resolver.resolve(TargetTimeChoice.ThisEvening, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26T19:00:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `this evening after 1900 returns next day`() {
        val now = Instant.parse("2026-07-26T20:00:00Z")
        val result = resolver.resolve(TargetTimeChoice.ThisEvening, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-27T19:00:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `flexible weekday returns 1900`() {
        val now = Instant.parse("2026-07-27T10:00:00Z")
        val result = resolver.resolve(TargetTimeChoice.Flexible, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-27T19:00:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `flexible weekend returns 1300`() {
        val now = Instant.parse("2026-07-26T10:00:00Z")
        val result = resolver.resolve(TargetTimeChoice.Flexible, now)
        assertTrue(result.isSuccess)
        assertEquals("2026-07-26T13:00:00Z", result.getOrThrow().toInstant().toString())
    }

    @Test
    fun `DST spring forward boundary`() {
        val tz = ZoneId.of("America/New_York")
        val dstResolver = TargetTimeResolver(tz)
        val beforeDst = ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, tz).toInstant()
        val choice = TargetTimeChoice.After(Duration.ofHours(2))
        val result = dstResolver.resolve(choice, beforeDst)
        assertTrue(result.isSuccess)
        val resolved = result.getOrThrow()
        assertEquals("04:30", resolved.toLocalTime().toString())
    }

    @Test
    fun `invalid input returns failure`() {
        val now = Instant.parse("2026-07-26T12:00:00Z")
        val choice = TargetTimeChoice.After(Duration.ofDays(-1))
        val result = resolver.resolve(choice, now)
        assertTrue(result.isSuccess)
    }
}
