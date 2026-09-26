package com.agentickitchen.shared.ai.prompt

import kotlin.test.Test
import kotlin.test.assertContains

/**
 * The Cooking Plan prompt must teach the exact validation contract the app enforces
 * fail-closed, so that schema-free (JSON_ONLY) plans are generated validator-clean.
 */
class CookingPlanPromptContractTest {

    private val prompt = PromptFactory.cookingPlanPrompt(
        "Fırında Yumurtalı Ekmek",
        listOf("bread", "tomato", "egg"),
        setOf("stove", "oven", "knife"),
        2,
        "electric",
        9,
        true,
        false,
        false,
        "none",
        emptySet(),
        "Türkçe"
    )

    @Test
    fun servingsMustEqualRequestExactly() {
        assertContains(prompt, "servings MUST equal the requested servings exactly: 2")
    }

    @Test
    fun stepTypeVocabularyIsTheFullValidatorEnum() {
        assertContains(prompt, "type MUST be exactly one of: prep, cook, rest, serve, combine, heat, cool")
        assertContains(prompt, "Never output synonyms such as bake, toast, assemble, mix, or fry as the type field")
        assertContains(prompt, "\"type\": \"prep|cook|rest|serve|combine|heat|cool\"")
    }

    @Test
    fun dependenciesMustBeAcyclicAndBackwardPointing() {
        assertContains(prompt, "a step must never depend on itself")
        assertContains(prompt, "the dependency graph MUST be acyclic")
        assertContains(prompt, "dependencies MUST point to logically earlier steps")
    }

    @Test
    fun exclusiveHeatResourcesMustBeDirectlyChained() {
        assertContains(prompt, "SAME exclusive heat resource (stove, oven, or airfryer)")
        assertContains(prompt, "MUST list the previous use of that same resource directly in its dependsOn")
        assertContains(prompt, "Never emit multiple independent steps that appear to use the same exclusive heat resource concurrently")
    }

    @Test
    fun durationsAreBounded() {
        assertContains(prompt, "durationSeconds MUST be between 30 and 3600 inclusive")
    }

    @Test
    fun resourceVocabularyIsComplete() {
        assertContains(prompt, "Resource MUST be one of: stove, oven, airfryer, counter, knife, bowl, fridge, sink, cutting_board, pan, pot, baking_tray, mixer, blender")
        assertContains(prompt, "Do not invent resource identifiers")
    }

    @Test
    fun canonicalUnitsAreListed() {
        assertContains(prompt, "g, kg, ml, l, tsp, tbsp, cup, piece, package, bunch, slice, clove, pinch, unit, to taste")
    }

    @Test
    fun everyStepIdMustBeUnique() {
        assertContains(prompt, "Every step id MUST be unique")
    }
}
