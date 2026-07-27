package com.agentickitchen.shared.cooking

import com.agentickitchen.shared.models.ScheduleEvent
import java.time.Instant

fun interface MonotonicClock { fun nowMillis(): Long }

enum class CookingSessionStatus { READY, RUNNING, PAUSED, COMPLETED, ENDED, ERROR }
data class LiveOperation(val event: ScheduleEvent, val remainingSeconds: Long)
data class CookingSessionState(val recipeName: String = "", val status: CookingSessionStatus = CookingSessionStatus.READY, val active: List<LiveOperation> = emptyList(), val upcoming: List<ScheduleEvent> = emptyList(), val completed: Set<String> = emptySet(), val elapsedSeconds: Long = 0, val error: String? = null)

class CookingSessionController(private val clock: MonotonicClock = MonotonicClock { System.nanoTime() / 1_000_000 }) {
    private var events = emptyList<ScheduleEvent>(); private var base = 0L; private var pausedAt: Long? = null; private var pausedTotal = 0L; private var state = CookingSessionState()
    fun current() = refresh()
    fun start(recipe: String, schedule: List<ScheduleEvent>): CookingSessionState {
        if (state.status == CookingSessionStatus.RUNNING) return state.copy(error = "Cooking is already running")
        if (schedule.isEmpty()) return CookingSessionState(recipe, CookingSessionStatus.ERROR, error = "Cooking plan has no scheduled steps")
        events = schedule; base = clock.nowMillis(); pausedAt = null; pausedTotal = 0; state = CookingSessionState(recipe, CookingSessionStatus.RUNNING); return refresh()
    }
    fun pause(): CookingSessionState { if (state.status == CookingSessionStatus.RUNNING) { pausedAt = clock.nowMillis(); state = state.copy(status = CookingSessionStatus.PAUSED) }; return refresh() }
    fun resume(): CookingSessionState { pausedAt?.let { pausedTotal += clock.nowMillis()-it; pausedAt=null }; if(state.status==CookingSessionStatus.PAUSED) state=state.copy(status=CookingSessionStatus.RUNNING); return refresh() }
    fun complete(id: String, skipped: Boolean = false): CookingSessionState { if (id !in events.map { it.id }) return state.copy(error="Unknown cooking step"); state=state.copy(completed=state.completed+id); return refresh() }
    fun end() = state.copy(status = CookingSessionStatus.ENDED).also { state=it }
    private fun refresh(): CookingSessionState { if (state.status !in setOf(CookingSessionStatus.RUNNING,CookingSessionStatus.PAUSED)) return state; val now=pausedAt?:clock.nowMillis(); val elapsed=(now-base-pausedTotal).coerceAtLeast(0); val first=events.minOf { Instant.parse(it.startIso).toEpochMilli() }; val active=events.filter { it.id !in state.completed && Instant.parse(it.startIso).toEpochMilli()-first<=elapsed && Instant.parse(it.endIso).toEpochMilli()-first>elapsed }.map { LiveOperation(it, ((Instant.parse(it.endIso).toEpochMilli()-first-elapsed)/1000).coerceAtLeast(0)) }; val done=state.completed + events.filter { Instant.parse(it.endIso).toEpochMilli()-first<=elapsed }.map{it.id}; state=state.copy(active=active,upcoming=events.filter{it.id !in done && Instant.parse(it.startIso).toEpochMilli()-first>elapsed},completed=done,elapsedSeconds=elapsed/1000); if(done.size==events.size) state=state.copy(status=CookingSessionStatus.COMPLETED); return state }
}
