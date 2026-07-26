package com.agentickitchen.shared.agents

import com.agentickitchen.shared.models.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.Duration

/**
 * Basit tersine zamanlama implementasyonu. Deterministik ve unit-test için yeterli.
 */
class SimpleTimingAgent : TimingAgent {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    override suspend fun computeSchedule(request: ScheduleRequest): ScheduleResult {
        val targetInstant = ZonedDateTime.parse(request.targetTimeIso, formatter).toInstant()

        val stepsById = request.steps.associateBy { it.id }

        // build dependency maps
        val dependencies = mutableMapOf<String, MutableSet<String>>()
        val dependents = mutableMapOf<String, MutableSet<String>>()
        for (step in request.steps) {
            dependencies.getOrPut(step.id) { mutableSetOf() }
            for (d in step.dependsOn) {
                dependencies.getOrPut(step.id) { mutableSetOf() }.add(d)
                dependents.getOrPut(d) { mutableSetOf() }.add(step.id)
            }
        }

        // Topological sort (Kahn)
        val indegree = dependencies.mapValues { it.value.size }.toMutableMap()
        val queue = ArrayDeque<String>()
        for ((id, deg) in indegree) if (deg == 0) queue.add(id)
        val topo = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            topo.add(n)
            for (m in dependents.getOrDefault(n, emptySet())) {
                indegree[m] = indegree.getOrDefault(m, 0) - 1
                if (indegree[m] == 0) queue.add(m)
            }
        }
        if (topo.size != stepsById.size) throw IllegalArgumentException("Cycle detected in steps")

        val reverseTopo = topo.asReversed()

        val resourceSchedules = mutableMapOf<String, MutableList<Pair<Instant, Instant>>>()
        val startMap = mutableMapOf<String, Instant>()
        val endMap = mutableMapOf<String, Instant>()

        fun overlaps(aStart: Instant, aEnd: Instant, bStart: Instant, bEnd: Instant): Boolean =
            aStart < bEnd && bStart < aEnd

        for (id in reverseTopo) {
            val step = stepsById[id] ?: continue
            val durationSec = step.durationSec?.toLong() ?: 60L
            val dur = Duration.ofSeconds(durationSec)

            val depStarts = dependents.getOrDefault(id, emptySet()).mapNotNull { startMap[it] }
            val latestFinish = if (depStarts.isEmpty()) targetInstant else depStarts.minOrNull()!!

            var end = latestFinish
            var start = end.minus(dur)

            val resource = step.resource
            val schedule = resourceSchedules.computeIfAbsent(resource) { mutableListOf() }

            // resolve resource conflicts by shifting earlier
            while (schedule.any { overlaps(start, end, it.first, it.second) }) {
                val overlapping = schedule.filter { overlaps(start, end, it.first, it.second) }
                val earliestOverlap = overlapping.minByOrNull { it.first }!!
                end = earliestOverlap.first
                start = end.minus(dur)
            }

            startMap[id] = start
            endMap[id] = end
            schedule.add(Pair(start, end))
        }

        val events = startMap.keys.map { id ->
            val s = startMap[id]!!
            val e = endMap[id]!!
            val step = stepsById[id]!!
            ScheduleEvent(
                id = id,
                startIso = ZonedDateTime.ofInstant(s, ZoneId.systemDefault()).format(formatter),
                endIso = ZonedDateTime.ofInstant(e, ZoneId.systemDefault()).format(formatter),
                instruction = step.instruction ?: humanLabel(step),
                resource = step.resource,
                parallelizable = false
            )
        }.sortedBy { it.startIso }

        return ScheduleResult(events = events)
    }

    private fun humanLabel(step: RecipeStep): String {
        val resource = when (step.resource) {
            "oven"      -> "Fırın"
            "stovetop"  -> "Ocak"
            "grill"     -> "Mangal"
            "airfryer"  -> "Hava Fritözü"
            "microwave" -> "Mikrodalga"
            "camping"   -> "Piknik Tüpü"
            else        -> step.resource.replaceFirstChar { it.uppercaseChar() }
        }
        return when (step.type) {
            "preheat"  -> {
                val temp = step.targetTempC?.let { " (${it}°C'ye)" } ?: ""
                "$resource ısıtılıyor$temp"
            }
            "cook"     -> "$resource'da pişiriliyor"
            "prep"     -> "Malzemeler hazırlanıyor"
            "rest"     -> "Dinlendiriliyor"
            "mix"      -> "Karıştırılıyor"
            "marinate" -> "Marine ediliyor"
            else       -> step.type.replaceFirstChar { it.uppercaseChar() } + " — $resource"
        }
    }
}
