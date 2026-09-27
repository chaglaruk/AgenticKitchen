package com.agentickitchen.shared.validator

import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Deterministic exclusive-resource sequencing: repeated stove/oven/airfryer uses gain a direct
 * dependency on the previous same-resource use; pan/pot/counter are untouched; invalid graphs
 * are skipped (never concealed); existing validator rejections remain intact.
 */
class ExclusiveResourceSequencerTest {

    private val validator = CookingPlanValidator(
        availableEquipment = setOf("stove", "oven", "knife", "bowl", "pan", "baking_tray"),
        stoveMaxLevel = 9,
        ovenAvailable = true,
        airfryerAvailable = false,
        dietType = "none",
        allergens = emptySet(),
        servings = 2
    )

    private fun step(
        id: String,
        type: String,
        instruction: String,
        resource: String,
        duration: Int = 120,
        targetTemperatureC: Int? = null,
        powerLevel: Int? = null,
        dependsOn: List<String> = emptyList(),
        visionCheckpointRecommended: Boolean = false
    ) = CookingStepDto(id, type, instruction, resource, duration, targetTemperatureC, powerLevel, dependsOn, visionCheckpointRecommended)

    private fun plan(steps: List<CookingStepDto>) = CookingPlanResponse(
        recipeName = "Test",
        servings = 2,
        ingredients = listOf(PlannedIngredientDto("ekmek", 200.0, "g")),
        steps = steps,
        safetyNotes = emptyList()
    )

    @Test
    fun `two independent stove steps get direct dependency on the first`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("s1", "cook", "Heat the pan", "stove"),
                    step("s2", "cook", "Fry in the pan", "stove")
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.NORMALIZED, outcome.result)
        assertEquals(1, outcome.edgesAdded)
        assertEquals(1, outcome.stoveEdges)
        assertEquals(listOf("s1"), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `three stove steps chain A to B to C`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("a", "cook", "Stove use one", "stove"),
                    step("b", "cook", "Stove use two", "stove"),
                    step("c", "cook", "Stove use three", "stove")
                )
            )
        )

        assertEquals(2, outcome.edgesAdded)
        assertEquals(listOf("a"), outcome.plan.steps[1].dependsOn)
        assertEquals(listOf("b"), outcome.plan.steps[2].dependsOn)
    }

    @Test
    fun `repeated oven steps are directly chained`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("o1", "cook", "Toast in oven", "oven", 600),
                    step("o2", "cook", "Bake in oven", "oven", 600)
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.NORMALIZED, outcome.result)
        assertEquals(1, outcome.ovenEdges)
        assertEquals(listOf("o1"), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `repeated airfryer steps are directly chained`() {
        val airfryerValidator = CookingPlanValidator(
            availableEquipment = setOf("airfryer"),
            stoveMaxLevel = 9,
            ovenAvailable = false,
            airfryerAvailable = true,
            dietType = "none",
            allergens = emptySet(),
            servings = 2
        )
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("a1", "cook", "Airfry round one", "airfryer", 600),
                    step("a2", "cook", "Airfry round two", "airfryer", 600)
                )
            )
        )

        assertEquals(1, outcome.airfryerEdges)
        assertEquals(listOf("a1"), outcome.plan.steps[1].dependsOn)
        assertFalse(airfryerValidator.validate(outcome.plan).errors.any { it.type == ErrorType.PARALLEL_RESOURCE_CONFLICT })
    }

    @Test
    fun `pan steps are untouched`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("p1", "cook", "Pan use one", "pan", 300),
                    step("p2", "cook", "Pan use two", "pan", 300)
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.UNCHANGED, outcome.result)
        assertEquals(0, outcome.edgesAdded)
        assertEquals(emptyList<String>(), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `pot steps are untouched`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("p1", "cook", "Pot use one", "pot", 300),
                    step("p2", "cook", "Pot use two", "pot", 300)
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.UNCHANGED, outcome.result)
        assertEquals(emptyList<String>(), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `counter steps are untouched`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("c1", "prep", "Counter one", "counter", 120),
                    step("c2", "prep", "Counter two", "counter", 120)
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.UNCHANGED, outcome.result)
        assertEquals(emptyList<String>(), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `existing correct direct chain is unchanged and not duplicated`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("o1", "cook", "Oven one", "oven", 600),
                    step("o2", "cook", "Oven two", "oven", 600, dependsOn = listOf("o1"))
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.UNCHANGED, outcome.result)
        assertEquals(listOf("o1"), outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `transitive ordering gains the direct same-resource edge without losing original deps`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("o1", "cook", "Oven one", "oven", 600),
                    step("m1", "combine", "Fill the tray", "counter", 120, dependsOn = listOf("o1")),
                    step("o2", "cook", "Oven two", "oven", 600, dependsOn = listOf("m1"))
                )
            )
        )

        assertEquals(ExclusiveResourceSequencer.Result.NORMALIZED, outcome.result)
        assertEquals(1, outcome.edgesAdded)
        // the direct same-resource edge is added; the original intermediate dependency is kept
        assertTrue("o1" in outcome.plan.steps[2].dependsOn)
        assertTrue("m1" in outcome.plan.steps[2].dependsOn)
    }

    @Test
    fun `independent same-resource steps keep stable original ordering`() {
        val outcome = ExclusiveResourceSequencer.sequence(
            plan(
                listOf(
                    step("prep", "prep", "Prep", "knife", 60),
                    step("stove_a", "cook", "First stove use", "stove", 300),
                    step("mid", "combine", "Middle work", "counter", 60),
                    step("stove_b", "cook", "Second stove use", "stove", 300)
                )
            )
        )

        // stove_a is listed before stove_b, so stove_b must depend on stove_a
        assertTrue("stove_a" in outcome.plan.steps[3].dependsOn)
        assertFalse("stove_b" in outcome.plan.steps[1].dependsOn)
    }

    @Test
    fun `existing dependency cycle is not concealed and validator still rejects`() {
        val cyclic = plan(
            listOf(
                step("s1", "prep", "One", "knife", 120, dependsOn = listOf("s2")),
                step("s2", "prep", "Two", "knife", 120, dependsOn = listOf("s1"))
            )
        )

        val outcome = ExclusiveResourceSequencer.sequence(cyclic)
        assertEquals(ExclusiveResourceSequencer.Result.SKIPPED_INVALID_GRAPH, outcome.result)
        assertEquals(cyclic, outcome.plan)
        assertTrue(validator.validate(cyclic).errors.any { it.type == ErrorType.DEPENDENCY_CYCLE })
    }

    @Test
    fun `missing dependency is not concealed and validator still rejects`() {
        val broken = plan(
            listOf(
                step("s1", "prep", "One", "knife", 120, dependsOn = listOf("ghost"))
            )
        )

        val outcome = ExclusiveResourceSequencer.sequence(broken)
        assertEquals(ExclusiveResourceSequencer.Result.SKIPPED_INVALID_GRAPH, outcome.result)
        assertEquals(broken, outcome.plan)
        assertTrue(validator.validate(broken).errors.any { it.type == ErrorType.MISSING_DEPENDENCY })
    }

    @Test
    fun `normalized plan passes the parallel resource conflict validator case`() {
        val drifting = plan(
            listOf(
                step("s1", "cook", "Stove use one", "stove", 300),
                step("s2", "cook", "Stove use two", "stove", 300)
            )
        )
        assertTrue(
            validator.validate(drifting).errors.any { it.type == ErrorType.PARALLEL_RESOURCE_CONFLICT }
        )

        val outcome = ExclusiveResourceSequencer.sequence(drifting)
        assertFalse(
            validator.validate(outcome.plan).errors.any { it.type == ErrorType.PARALLEL_RESOURCE_CONFLICT }
        )
    }

    @Test
    fun `non dependency step fields and ingredients are structurally unchanged`() {
        val original = plan(
            listOf(
                step("s1", "cook", "Stove one", "stove", 300, targetTemperatureC = 180, powerLevel = 7),
                step("s2", "cook", "Stove two", "stove", 240, visionCheckpointRecommended = true)
            )
        )

        val outcome = ExclusiveResourceSequencer.sequence(original)
        val before = original.steps
        val after = outcome.plan.steps
        assertEquals(before.map { it.id }, after.map { it.id })
        assertEquals(before.map { it.type }, after.map { it.type })
        assertEquals(before.map { it.instruction }, after.map { it.instruction })
        assertEquals(before.map { it.resource }, after.map { it.resource })
        assertEquals(before.map { it.durationSeconds }, after.map { it.durationSeconds })
        assertEquals(before.map { it.targetTemperatureC }, after.map { it.targetTemperatureC })
        assertEquals(before.map { it.powerLevel }, after.map { it.powerLevel })
        assertEquals(before.map { it.visionCheckpointRecommended }, after.map { it.visionCheckpointRecommended })
        assertEquals(original.ingredients, outcome.plan.ingredients)
        assertEquals(original.recipeName, outcome.plan.recipeName)
        assertEquals(original.servings, outcome.plan.servings)
        assertEquals(original.safetyNotes, outcome.plan.safetyNotes)
        // the only structural change: the added same-resource dependency
        assertEquals(listOf("s1"), after[1].dependsOn)
    }

    @Test
    fun `substitution style mutated plan is normalized the same way`() {
        val mutated = plan(
            listOf(
                step("s1", "prep", "Prep", "knife", 120),
                step("s2", "cook", "Pan use one", "pan", 300),
                step("s3", "cook", "Oven use", "oven", 600),
                step("s4", "cook", "Stove use two", "stove", 300)
            )
        )

        val outcome = ExclusiveResourceSequencer.sequence(mutated)
        // pan is not exclusive: s2 untouched; stove has a single use: no edge; oven single: no edge
        assertEquals(ExclusiveResourceSequencer.Result.UNCHANGED, outcome.result)
        assertEquals(0, outcome.edgesAdded)
    }
}
