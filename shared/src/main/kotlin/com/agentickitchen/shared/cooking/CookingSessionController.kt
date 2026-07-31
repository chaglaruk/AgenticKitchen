package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface ClockDomain {
    fun monotonicMillis(): Long
    fun epochMillis(): Long
}

/**
 * Persisted-time field semantics (all values are epoch milliseconds, NOT monotonic):
 *
 * - startedAtMillis: epoch milliseconds corresponding to the session's original effective start.
 *   Used as the baseline for legacy RUNNING fallback when lastRunningStartMillis is unavailable.
 *
 * - accumulatedElapsedSeconds: authoritative elapsed duration recorded at the last persistence point.
 *   This is the ground-truth duration that must never be double-counted.
 *
 * - lastRunningStartMillis: epoch milliseconds at which the current running segment began.
 *   When valid, elapsed = accumulated + max(0, epochNow - lastRunningStartMillis).
 *   Do NOT describe monotonic values as persistable across process death.
 *
 * - pausedAtMillis: epoch milliseconds at which the persisted paused state was recorded.
 *   For PAUSED sessions, elapsed remains frozen at accumulatedElapsedSeconds regardless of wall-clock movement.
 */
enum class CookingSessionStatus { READY, RUNNING, PAUSED, COMPLETED, ENDED, ERROR }
data class LiveOperation(val event: ScheduleEvent, val remainingSeconds: Long)
data class CookingSessionState(val recipeName: String = "", val status: CookingSessionStatus = CookingSessionStatus.READY, val active: List<LiveOperation> = emptyList(), val upcoming: List<ScheduleEvent> = emptyList(), val completed: Set<String> = emptySet(), val skipped: Set<String> = emptySet(), val elapsedSeconds: Long = 0, val error: String? = null)

class CookingSessionController(
    private val clock: ClockDomain = object : ClockDomain {
        override fun monotonicMillis(): Long = System.nanoTime() / 1_000_000
        override fun epochMillis(): Long = System.currentTimeMillis()
    }
) {
    private var events = emptyList<ScheduleEvent>()
    private var starts = emptyMap<String, Long>()
    private var ends = emptyMap<String, Long>()
    private var startedAt = 0L
    private var pausedAt: Long? = null
    private var pausedMillis = 0L
    private var state = CookingSessionState()

    fun current(): CookingSessionState = refresh()
    fun start(recipe: String, schedule: List<ScheduleEvent>): CookingSessionState {
        if (state.status == CookingSessionStatus.RUNNING || state.status == CookingSessionStatus.PAUSED) return state.copy(error = "Cooking is already running")
        if (schedule.isEmpty()) return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has no scheduled steps")
        val parsed = try {
            schedule.associate {
                it.id to (
                    ZonedDateTime.parse(it.startIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli() to
                        ZonedDateTime.parse(it.endIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli()
                    )
            }
        } catch (_: Exception) {
            return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has invalid step times")
        }
        if (parsed.values.any { it.second <= it.first }) return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has invalid step duration")
        val first = parsed.values.minOf { it.first }; events = schedule; starts = parsed.mapValues { it.value.first - first }; ends = parsed.mapValues { it.value.second - first }; startedAt = clock.monotonicMillis(); pausedAt = null; pausedMillis = 0; state = CookingSessionState(recipeName = recipe, status = CookingSessionStatus.RUNNING); return refresh()
    }
    fun pause(): CookingSessionState { if (state.status != CookingSessionStatus.RUNNING) return state.copy(error = "Cooking is not running"); pausedAt = clock.monotonicMillis(); state = state.copy(status = CookingSessionStatus.PAUSED); return refresh() }
    fun resume(): CookingSessionState { val paused = pausedAt ?: return state.copy(error = "Cooking is not paused"); pausedMillis += clock.monotonicMillis() - paused; pausedAt = null; state = state.copy(status = CookingSessionStatus.RUNNING); return refresh() }
    fun complete(id: String): CookingSessionState = finish(id, false)
    fun skip(id: String): CookingSessionState = finish(id, true)
    fun end(): CookingSessionState { state = state.copy(status = CookingSessionStatus.ENDED, active = emptyList(), upcoming = emptyList()); return state }
    fun restore(
        recipe: String,
        schedule: List<ScheduleEvent>,
        status: CookingSessionStatus,
        startedAtMillis: Long,
        accumulatedElapsedSeconds: Long,
        lastRunningStartMillis: Long? = null,
        pausedAtMillis: Long? = null,
        completed: Set<String> = emptySet(),
        skipped: Set<String> = emptySet()
    ): CookingSessionState {
        if (schedule.isEmpty()) {
            state = CookingSessionState(recipeName = recipe, status = status, completed = completed, skipped = skipped)
            return state
        }
        val parsed = try {
            schedule.associate {
                it.id to (
                    ZonedDateTime.parse(it.startIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli() to
                        ZonedDateTime.parse(it.endIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli()
                    )
            }
        } catch (_: Exception) {
            return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has invalid step times")
        }
        val first = parsed.values.minOf { it.first }
        events = schedule
        starts = parsed.mapValues { it.value.first - first }
        ends = parsed.mapValues { it.value.second - first }

        val now = clock.monotonicMillis()
        val clampedAccumulatedMs = (accumulatedElapsedSeconds.coerceAtLeast(0)) * 1000L

        when (status) {
            CookingSessionStatus.RUNNING -> {
                val epochNow = clock.epochMillis()
                var deadMillis = 0L
                if (lastRunningStartMillis != null && lastRunningStartMillis > 0 && epochNow >= lastRunningStartMillis) {
                    // Normal case: lastRunningStartMillis is valid
                    deadMillis = epochNow - lastRunningStartMillis
                } else if (startedAtMillis > 0 && epochNow >= startedAtMillis) {
                    // Legacy fallback: no valid lastRunningStartMillis but startedAtMillis is available
                    // Use non-double-counting fallback: max(accumulated, epochNow - startedAtMillis)
                    val fallbackElapsedMs = epochNow - startedAtMillis
                    deadMillis = (fallbackElapsedMs - clampedAccumulatedMs).coerceAtLeast(0)
                } else {
                    // Neither timestamp is usable - preserve only accumulated duration
                    deadMillis = 0L
                }
                val totalElapsedMs = (clampedAccumulatedMs + deadMillis).coerceAtLeast(0)
                startedAt = now - totalElapsedMs
                pausedAt = null
                pausedMillis = 0L
            }
            CookingSessionStatus.PAUSED -> {
                // PAUSED sessions must remain frozen at accumulatedElapsedSeconds.
                // Future or malformed pausedAtMillis must not advance elapsed time.
                // We ignore pausedAtMillis entirely for elapsed computation - it's only informational.
                pausedAt = now
                startedAt = now - clampedAccumulatedMs
                pausedMillis = 0L
            }
            else -> {
                startedAt = now - clampedAccumulatedMs
                pausedAt = null
                pausedMillis = 0L
            }
        }

        state = CookingSessionState(
            recipeName = recipe,
            status = status,
            completed = completed,
            skipped = skipped
        )
        return refresh()
    }
    private fun finish(id: String, skipped: Boolean): CookingSessionState { if (id !in starts) return state.copy(error = "Unknown cooking step"); state = if (skipped) state.copy(skipped = state.skipped + id) else state.copy(completed = state.completed + id); return refresh() }
    private fun refresh(): CookingSessionState { if (state.status !in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) return state; val elapsed = ((pausedAt ?: clock.monotonicMillis()) - startedAt - pausedMillis).coerceAtLeast(0); val done = state.completed + state.skipped; val active = events.filter { it.id !in done && starts.getValue(it.id) <= elapsed && ends.getValue(it.id) > elapsed }.map { LiveOperation(it, ((ends.getValue(it.id)-elapsed)/1000).coerceAtLeast(0)) }; val completed = state.completed + events.filter { it.id !in done && ends.getValue(it.id) <= elapsed }.map { it.id }; state = state.copy(active = active, upcoming = events.filter { it.id !in completed + state.skipped && starts.getValue(it.id) > elapsed }, completed = completed, elapsedSeconds = elapsed/1000); if (state.completed.size + state.skipped.size == events.size) state = state.copy(status = CookingSessionStatus.COMPLETED); return state }
}
