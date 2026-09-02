package com.agentickitchen.shared.ai

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class RecipeImportParserTest {
    @Test
    fun `parses recipe JSON-LD from graph without AI`() {
        val html = """
            <html><head><script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebSite","name":"Example"},
              {"@type":"Recipe","name":"Tomato Rice","recipeYield":"2 servings",
               "recipeIngredient":["200 g rice","2 tomatoes","1 onion"],
               "recipeInstructions":[{"@type":"HowToStep","text":"Rinse the rice."},{"@type":"HowToStep","text":"Cook with tomato and onion."}]}
            ]}
            </script></head></html>
        """.trimIndent()

        val result = DeterministicRecipeImportParser.parseJsonLd(html, "example.com", "https://example.com/rice")
        assertNotNull(result)
        assertEquals(RecipeImportSource.URL_JSON_LD, result?.source)
        assertEquals("Tomato Rice", result?.recipe?.name)
        assertEquals(2, result?.recipe?.servings)
        assertEquals(3, result?.recipe?.ingredients?.size)
        assertEquals(200.0, result?.recipe?.ingredients?.first()?.quantity ?: 0.0, 0.0001)
        assertEquals("g", result?.recipe?.ingredients?.first()?.unit)
        assertEquals("rice", result?.recipe?.ingredients?.first()?.canonicalIngredientId)
        assertEquals(2, result?.recipe?.instructions?.size)
    }

    @Test
    fun `parses explicitly sectioned plain text deterministically`() {
        val text = """
            Onion Eggs
            Serves 2
            Ingredients:
            - 2 eggs
            - 1 onion
            Instructions:
            1. Slice the onion.
            2. Cook the onion and eggs.
        """.trimIndent()

        val result = DeterministicRecipeImportParser.parsePlainText(text, "shared text")
        assertNotNull(result)
        assertEquals(RecipeImportSource.PLAIN_TEXT, result?.source)
        assertEquals("Onion Eggs", result?.recipe?.name)
        assertEquals(2, result?.recipe?.servings)
        assertEquals("egg", result?.recipe?.ingredients?.first()?.canonicalIngredientId)
        assertEquals("adet", result?.recipe?.ingredients?.first()?.unit)
        assertTrue(result?.recipe?.instructions?.first()?.startsWith("Slice") == true)
    }

    @Test
    fun `deterministic parser exposes structured amount review count`() {
        val text = """
            Tomato Toast
            Serves 1
            Ingredients:
            - bread
            - tomato
            Instructions:
            1. Assemble and serve.
        """.trimIndent()

        val result = DeterministicRecipeImportParser.parsePlainText(text)

        assertNotNull(result)
        assertEquals(2, recipeImportAmountReviewCount(result?.uncertainty))
    }

    @Test
    fun `keeps implicit ingredient amount uncertain rather than inventing`() {
        val ingredient = DeterministicRecipeImportParser.parseIngredientLine("salt to taste")
        assertEquals("salt to taste", ingredient.displayName)
        assertNull(ingredient.quantity)
        assertNull(ingredient.unit)
        assertEquals("amount_not_explicit", ingredient.uncertaintyReason)
    }

    @Test
    fun `ambiguous prose without recipe sections does not pretend to parse`() {
        val result = DeterministicRecipeImportParser.parsePlainText("I made a nice tomato dish yesterday and it was great.")
        assertNull(result)
    }
}
