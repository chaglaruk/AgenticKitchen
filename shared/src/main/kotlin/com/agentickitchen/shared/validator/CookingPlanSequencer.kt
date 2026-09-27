package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse

/**
 * Deterministic exclusive-resource sequencing for generated Cooking Plans.
 *
 * Gemini occasionally emits multiple uses of the same exclusive heat resource without the
 * direct same-resource dependency the parallel-conflict validator requires. This normalizer
 * repairs exactly that: for each of stove/oven/airfryer, every later use gains a direct
 * dependency on the previous use of the same resource, computed along a stable topological
 * order that preserves the model's original list order among independent steps.
 *
 * It never conceals unrelated structural defects: duplicate step ids, missing dependencies,
 * and dependency cycles are left untouched (SKIPPED_INVALID_GRAPH) for CookingPlanValidator
 * to reject fail-closed. Existing dependencies are never removed, and adding edges along a
 * topological order cannot introduce a cycle.
 */
object ExclusiveResourceSequencer {

    /** Same exclusive heat-resource definition the parallel-conflict validator enforces. */
    val EXCLUSIVE_HEAT_RESOURCES = linkedSetOf("stove", "oven", "airfryer")

    enum class Result { UNCHANGED, NORMALIZED, SKIPPED_INVALID_GRAPH }

    data class Outcome(
        val plan: CookingPlanResponse,
        val result: Result,
        val edgesAdded: Int,
        val stoveEdges: Int,
        val ovenEdges: Int,
        val airfryerEdges: Int
    )

    fun sequence(plan: CookingPlanResponse): Outcome {
        val steps = plan.steps
        val indexOf = HashMap<String, Int>(steps.size * 2)
        steps.forEachIndexed { index, step -> indexOf[step.id] = index }

        val duplicateIds = steps.size != indexOf.size
        val missingDependency = steps.any { step -> step.dependsOn.any { it !in indexOf } }
        if (duplicateIds || missingDependency) {
            return Outcome(plan, Result.SKIPPED_INVALID_GRAPH, 0, 0, 0, 0)
        }

        val dependents = Array(steps.size) { mutableListOf<Int>() }
        val inDegree = IntArray(steps.size)
        steps.forEachIndexed { index, step ->
            step.dependsOn.forEach { dep ->
                val depIndex = indexOf.getValue(dep)
                dependents[depIndex].add(index)
                inDegree[index]++
            }
        }

        // Stable Kahn's algorithm: among simultaneously ready steps, always take the one that
        // appears first in the model's original list order.
        val ready = sortedSetOf<Int>(compareBy { it })
        steps.indices.forEach { index -> if (inDegree[index] == 0) ready.add(index) }
        val topological = mutableListOf<Int>()
        while (ready.isNotEmpty()) {
            val current = ready.first()
            ready.remove(current)
            topological.add(current)
            dependents[current].forEach { dependent ->
                if (--inDegree[dependent] == 0) ready.add(dependent)
            }
        }
        if (topological.size != steps.size) {
            return Outcome(plan, Result.SKIPPED_INVALID_GRAPH, 0, 0, 0, 0)
        }

        val newDependencies = Array(steps.size) { steps[it].dependsOn.toMutableList() }
        val edgesByResource = linkedMapOf("stove" to 0, "oven" to 0, "airfryer" to 0)
        var edgesAdded = 0

        for (resource in EXCLUSIVE_HEAT_RESOURCES) {
            val resourceSteps = topological.filter { steps[it].resource == resource }
            for (k in 1 until resourceSteps.size) {
                val previous = steps[resourceSteps[k - 1]].id
                val currentDeps = newDependencies[resourceSteps[k]]
                if (previous !in currentDeps) {
                    currentDeps.add(previous)
                    edgesAdded++
                    edgesByResource[resource] = edgesByResource.getValue(resource) + 1
                }
            }
        }

        if (edgesAdded == 0) {
            return Outcome(plan, Result.UNCHANGED, 0, 0, 0, 0)
        }

        val sequencedSteps = steps.mapIndexed { index, step ->
            step.copy(dependsOn = newDependencies[index].distinct())
        }
        return Outcome(
            plan = plan.copy(steps = sequencedSteps),
            result = Result.NORMALIZED,
            edgesAdded = edgesAdded,
            stoveEdges = edgesByResource.getValue("stove"),
            ovenEdges = edgesByResource.getValue("oven"),
            airfryerEdges = edgesByResource.getValue("airfryer")
        )
    }
}
