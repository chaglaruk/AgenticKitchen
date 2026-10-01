package com.agentickitchen.shared.scheduler

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TargetTimeResolver(private val zoneId: ZoneId = ZoneId.systemDefault()) {

    fun resolve(choice: TargetTimeChoice, now: Instant = Instant.now()): Result<ZonedDateTime> {
        return try {
            val zonedNow = now.atZone(zoneId)
            val resolved = when (choice) {
                is TargetTimeChoice.Exact -> resolveExact(choice.localTime, zonedNow)
                is TargetTimeChoice.After -> zonedNow.plus(choice.duration)
                TargetTimeChoice.ThisEvening -> resolveThisEvening(zonedNow)
                TargetTimeChoice.Flexible -> resolveFlexible(zonedNow)
            }
            Result.success(resolved)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to resolve target time: ${e.message}"))
        }
    }

    private fun resolveExact(time: LocalTime, now: ZonedDateTime): ZonedDateTime {
        var candidate = now.with(time)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun resolveThisEvening(now: ZonedDateTime): ZonedDateTime {
        val evening = LocalTime.of(19, 0)
        var candidate = now.with(evening)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun resolveFlexible(now: ZonedDateTime): ZonedDateTime {
        val defaultMealTime: LocalTime = when (now.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> LocalTime.of(13, 0)
            else -> LocalTime.of(19, 0)
        }
        var candidate = now.with(defaultMealTime)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
