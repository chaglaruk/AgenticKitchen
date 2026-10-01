package com.agentickitchen.shared.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationFormatterTest {

    @Test
    fun `seconds only`() {
        assertEquals("45s", DurationFormatter.format(Duration.ofSeconds(45)))
    }

    @Test
    fun `minutes only`() {
        assertEquals("5m", DurationFormatter.format(Duration.ofMinutes(5)))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("5m 45s", DurationFormatter.format(Duration.ofSeconds(345)))
    }

    @Test
    fun `hours and minutes`() {
        assertEquals("1h 10m", DurationFormatter.format(Duration.ofMinutes(70)))
    }

    @Test
    fun `hours only`() {
        assertEquals("2h", DurationFormatter.format(Duration.ofHours(2)))
    }

    @Test
    fun `zero duration`() {
        assertEquals("0s", DurationFormatter.format(Duration.ZERO))
    }

    @Test
    fun `turkish seconds`() {
        assertEquals("45sn", DurationFormatter.format(Duration.ofSeconds(45), turkish = true))
    }

    @Test
    fun `turkish minutes`() {
        assertEquals("5dk", DurationFormatter.format(Duration.ofMinutes(5), turkish = true))
    }

    @Test
    fun `turkish hours and minutes`() {
        assertEquals("1sa 10dk", DurationFormatter.format(Duration.ofMinutes(70), turkish = true))
    }

    @Test
    fun `compact format hours`() {
        assertEquals("1h 10m", DurationFormatter.formatCompact(Duration.ofMinutes(70)))
    }

    @Test
    fun `compact format minutes`() {
        assertEquals("5m", DurationFormatter.formatCompact(Duration.ofMinutes(5)))
    }
}
