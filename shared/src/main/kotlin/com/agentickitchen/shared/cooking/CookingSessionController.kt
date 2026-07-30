package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun interface MonotonicClock { fun nowMillis(): Long }
enum class CookingSessionStatus { READY, RUNNING, PAUSED, COMPLETED, ENDED, ERROR }
data class LiveOperation(val event: ScheduleEvent, val remainingSeconds: Long)
data class CookingSessionState(val recipeName: String = "", val status: CookingSessionStatus = CookingSessionStatus.READY, val active: List<LiveOperation> = emptyList(), val upcoming: List<ScheduleEvent> = emptyList(), val completed: Set<String> = emptySet(), val skipped: Set<String> = emptySet(), val elapsedSeconds: Long = 0, val error: String? = null)

class CookingSessionController(private val clock: MonotonicClock = MonotonicClock { System.nanoTime() / 1_000_000 }) {
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
        val first = parsed.values.minOf { it.first }; events = schedule; starts = parsed.mapValues { it.value.first - first }; ends = parsed.mapValues { it.value.second - first }; startedAt = clock.nowMillis(); pausedAt = null; pausedMillis = 0; state = CookingSessionState(recipeName = recipe, status = CookingSessionStatus.RUNNING); return refresh()
    }
    fun pause(): CookingSessionState { if (state.status != CookingSessionStatus.RUNNING) return state.copy(error = "Cooking is not running"); pausedAt = clock.nowMillis(); state = state.copy(status = CookingSessionStatus.PAUSED); return refresh() }
    fun resume(): CookingSessionState { val paused = pausedAt ?: return state.copy(error = "Cooking is not paused"); pausedMillis += clock.nowMillis() - paused; pausedAt = null; state = state.copy(status = CookingSessionStatus.RUNNING); return refresh() }
    fun complete(id: String): CookingSessionState = finish(id, false)
    fun skip(id: String): CookingSessionState = finish(id, true)
    fun end(): CookingSessionState { state = state.copy(status = CookingSessionStatus.ENDED, active = emptyList(), upcoming = emptyList()); return state }
    private fun finish(id: String, skipped: Boolean): CookingSessionState { if (id !in starts) return state.copy(error = "Unknown cooking step"); state = if (skipped) state.copy(skipped = state.skipped + id) else state.copy(completed = state.completed + id); return refresh() }
    private fun refresh(): CookingSessionState { if (state.status !in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) return state; val elapsed = ((pausedAt ?: clock.nowMillis()) - startedAt - pausedMillis).coerceAtLeast(0); val done = state.completed + state.skipped; val active = events.filter { it.id !in done && starts.getValue(it.id) <= elapsed && ends.getValue(it.id) > elapsed }.map { LiveOperation(it, ((ends.getValue(it.id)-elapsed)/1000).coerceAtLeast(0)) }; val completed = state.completed + events.filter { it.id !in done && ends.getValue(it.id) <= elapsed }.map { it.id }; state = state.copy(active = active, upcoming = events.filter { it.id !in completed + state.skipped && starts.getValue(it.id) > elapsed }, completed = completed, elapsedSeconds = elapsed/1000); if (state.completed.size + state.skipped.size == events.size) state = state.copy(status = CookingSessionStatus.COMPLETED); return state }
}
