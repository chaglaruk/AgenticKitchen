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
 *
 * Timer extensions are stored as namespaced metadata markers in the persisted skipped-step set so the
 * existing persistence schema can replay +1-minute adjustments after process death without changing a
 * user's real completed/skipped decisions. Public helpers below filter those markers from UI counts.
 */
enum class CookingSessionStatus { READY, RUNNING, PAUSED, COMPLETED, ENDED, ERROR }
data class LiveOperation(val event: ScheduleEvent, val remainingSeconds: Long)
data class CookingSessionState(
    val recipeName: String = "",
    val status: CookingSessionStatus = CookingSessionStatus.READY,
    val active: List<LiveOperation> = emptyList(),
    val upcoming: List<ScheduleEvent> = emptyList(),
    val completed: Set<String> = emptySet(),
    val skipped: Set<String> = emptySet(),
    val elapsedSeconds: Long = 0,
    val error: String? = null
)

private const val ADD_MINUTE_COMMAND_PREFIX = "__ak_add_minute__:"
private const val TIMER_EXTENSION_MARKER_PREFIX = "__ak_timer_extension__:"
private const val ONE_MINUTE_MILLIS = 60_000L

/** Command consumed by [CookingSessionController.complete] to extend the active timer by one minute. */
fun cookingAddMinuteCommand(eventId: String): String = "$ADD_MINUTE_COMMAND_PREFIX$eventId"

/** Real completed step ids only; persistence metadata is intentionally hidden from presentation logic. */
fun cookingVisibleCompleted(ids: Set<String>): Set<String> = ids.filterNot(::isCookingMetadataId).toSet()

/** Real skipped step ids only; persistence metadata is intentionally hidden from presentation logic. */
fun cookingVisibleSkipped(ids: Set<String>): Set<String> = ids.filterNot(::isCookingMetadataId).toSet()

private fun isCookingMetadataId(id: String): Boolean = id.startsWith(TIMER_EXTENSION_MARKER_PREFIX)

private data class TimerExtensionRecord(val sequence: Int, val eventId: String)

private fun timerExtensionRecord(marker: String): TimerExtensionRecord? {
    if (!marker.startsWith(TIMER_EXTENSION_MARKER_PREFIX)) return null
    val payload = marker.removePrefix(TIMER_EXTENSION_MARKER_PREFIX)
    val separator = payload.indexOf(':')
    if (separator <= 0 || separator == payload.lastIndex) return null
    val sequence = payload.substring(0, separator).toIntOrNull() ?: return null
    return TimerExtensionRecord(sequence, payload.substring(separator + 1))
}

private fun remainingWholeSeconds(remainingMillis: Long): Long {
    val clamped = remainingMillis.coerceAtLeast(0)
    return (clamped / 1000L) + if (clamped % 1000L == 0L) 0L else 1L
}

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
        if (state.status == CookingSessionStatus.RUNNING || state.status == CookingSessionStatus.PAUSED) {
            return state.copy(error = "Cooking is already running")
        }
        if (schedule.isEmpty()) {
            return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has no scheduled steps")
        }
        val parsed = parseSchedule(schedule) ?: return CookingSessionState(
            recipeName = recipe,
            status = CookingSessionStatus.ERROR,
            error = "Cooking plan has invalid step times"
        )
        if (parsed.values.any { it.second <= it.first }) {
            return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has invalid step duration")
        }
        val first = parsed.values.minOf { it.first }
        events = schedule
        starts = parsed.mapValues { it.value.first - first }
        ends = parsed.mapValues { it.value.second - first }
        startedAt = clock.monotonicMillis()
        pausedAt = null
        pausedMillis = 0L
        state = CookingSessionState(recipeName = recipe, status = CookingSessionStatus.RUNNING)
        return refresh()
    }

    fun pause(): CookingSessionState {
        if (state.status != CookingSessionStatus.RUNNING) return state.copy(error = "Cooking is not running")
        pausedAt = clock.monotonicMillis()
        state = state.copy(status = CookingSessionStatus.PAUSED, error = null)
        return refresh()
    }

    fun resume(): CookingSessionState {
        val paused = pausedAt ?: return state.copy(error = "Cooking is not paused")
        pausedMillis += clock.monotonicMillis() - paused
        pausedAt = null
        state = state.copy(status = CookingSessionStatus.RUNNING, error = null)
        return refresh()
    }

    fun complete(id: String): CookingSessionState {
        if (id.startsWith(ADD_MINUTE_COMMAND_PREFIX)) {
            return extendActiveStepByMinute(id.removePrefix(ADD_MINUTE_COMMAND_PREFIX))
        }
        return finish(id, false)
    }

    fun skip(id: String): CookingSessionState = finish(id, true)

    fun end(): CookingSessionState {
        state = state.copy(status = CookingSessionStatus.ENDED, active = emptyList(), upcoming = emptyList(), error = null)
        return state
    }

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
        val parsed = parseSchedule(schedule) ?: return CookingSessionState(
            recipeName = recipe,
            status = CookingSessionStatus.ERROR,
            error = "Cooking plan has invalid step times"
        )
        if (parsed.values.any { it.second <= it.first }) {
            return CookingSessionState(recipeName = recipe, status = CookingSessionStatus.ERROR, error = "Cooking plan has invalid step duration")
        }
        val first = parsed.values.minOf { it.first }
        events = schedule
        starts = parsed.mapValues { it.value.first - first }
        ends = parsed.mapValues { it.value.second - first }

        skipped.mapNotNull(::timerExtensionRecord)
            .sortedBy(TimerExtensionRecord::sequence)
            .forEach { replayTimerExtension(it.eventId) }

        val now = clock.monotonicMillis()
        val clampedAccumulatedMs = accumulatedElapsedSeconds.coerceAtLeast(0) * 1000L

        when (status) {
            CookingSessionStatus.RUNNING -> {
                val epochNow = clock.epochMillis()
                val deadMillis = when {
                    lastRunningStartMillis != null && lastRunningStartMillis > 0 && epochNow >= lastRunningStartMillis ->
                        epochNow - lastRunningStartMillis
                    startedAtMillis > 0 && epochNow >= startedAtMillis -> {
                        val fallbackElapsedMs = epochNow - startedAtMillis
                        (fallbackElapsedMs - clampedAccumulatedMs).coerceAtLeast(0)
                    }
                    else -> 0L
                }
                val totalElapsedMs = (clampedAccumulatedMs + deadMillis).coerceAtLeast(0)
                startedAt = now - totalElapsedMs
                pausedAt = null
                pausedMillis = 0L
            }
            CookingSessionStatus.PAUSED -> {
                // Paused elapsed is frozen. pausedAtMillis is informational only and must not advance it.
                @Suppress("UNUSED_VARIABLE") val ignoredPersistedPausedAt = pausedAtMillis
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

    private fun parseSchedule(schedule: List<ScheduleEvent>): Map<String, Pair<Long, Long>>? = try {
        schedule.associate {
            it.id to (
                ZonedDateTime.parse(it.startIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli() to
                    ZonedDateTime.parse(it.endIso, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant().toEpochMilli()
                )
        }
    } catch (_: Exception) {
        null
    }

    private fun extendActiveStepByMinute(eventId: String): CookingSessionState {
        if (state.status !in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            return state.copy(error = "Cooking is not active")
        }
        refresh()
        if (state.active.none { it.event.id == eventId }) {
            return state.copy(error = "Cooking step is not active")
        }
        if (!applyMinuteExtension(eventId)) {
            return state.copy(error = "Unknown cooking step")
        }
        val nextSequence = state.skipped.count(::isCookingMetadataId) + 1
        val marker = "$TIMER_EXTENSION_MARKER_PREFIX$nextSequence:$eventId"
        state = state.copy(skipped = state.skipped + marker, error = null)
        return refresh()
    }

    private fun replayTimerExtension(eventId: String) {
        applyMinuteExtension(eventId)
    }

    /**
     * Extending the current operation also delays events that were scheduled to start at/after its
     * previous end. Concurrent work that already started remains independent. This keeps the visible
     * countdown and the scheduler on one timeline.
     */
    private fun applyMinuteExtension(eventId: String): Boolean {
        val pivotEnd = ends[eventId] ?: return false
        if (eventId !in starts) return false
        val futureIds = starts.filter { (id, start) -> id != eventId && start >= pivotEnd }.keys
        starts = starts.mapValues { (id, start) -> if (id in futureIds) start + ONE_MINUTE_MILLIS else start }
        ends = ends.mapValues { (id, end) ->
            when {
                id == eventId -> end + ONE_MINUTE_MILLIS
                id in futureIds -> end + ONE_MINUTE_MILLIS
                else -> end
            }
        }
        return true
    }

    private fun finish(id: String, skipped: Boolean): CookingSessionState {
        if (id !in starts) return state.copy(error = "Unknown cooking step")
        state = if (skipped) {
            state.copy(skipped = state.skipped + id, error = null)
        } else {
            state.copy(completed = state.completed + id, error = null)
        }
        return refresh()
    }

    private fun refresh(): CookingSessionState {
        if (state.status !in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) return state
        val elapsed = ((pausedAt ?: clock.monotonicMillis()) - startedAt - pausedMillis).coerceAtLeast(0)
        val visibleCompleted = cookingVisibleCompleted(state.completed).filterTo(mutableSetOf()) { it in starts }
        val visibleSkipped = cookingVisibleSkipped(state.skipped).filterTo(mutableSetOf()) { it in starts }
        val done = visibleCompleted + visibleSkipped
        val active = events
            .filter { it.id !in done && starts.getValue(it.id) <= elapsed && ends.getValue(it.id) > elapsed }
            .map { event ->
                LiveOperation(event, remainingWholeSeconds(ends.getValue(event.id) - elapsed))
            }
        val autoCompleted = events
            .filter { it.id !in done && ends.getValue(it.id) <= elapsed }
            .map { it.id }
        val completed = state.completed + autoCompleted
        val visibleCompletedAfterRefresh = cookingVisibleCompleted(completed).filterTo(mutableSetOf()) { it in starts }
        val visibleDone = visibleCompletedAfterRefresh + visibleSkipped
        state = state.copy(
            active = active,
            upcoming = events.filter { it.id !in visibleDone && starts.getValue(it.id) > elapsed },
            completed = completed,
            elapsedSeconds = elapsed / 1000
        )
        if (visibleDone.size == events.size) {
            state = state.copy(status = CookingSessionStatus.COMPLETED, active = emptyList(), upcoming = emptyList())
        }
        return state
    }
}
