package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.shared.ai.StructuredRecipeParser
import com.agentickitchen.shared.ai.prompt.PromptFactory
import com.agentickitchen.shared.validator.CookingPlanValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LocalRecipeProviderTest {

    @Test
    fun createsExactlyThreeStructuredRecipeOptions() = runBlocking {
        val diagnostics = mutableListOf<ProviderDiagnostic>()
        val provider = LocalRecipeProvider(diagnostics::add)
        val prompt = PromptFactory.recipeOptionsPrompt(
            ingredients = listOf("tomato", "egg", "rice"),
            equipment = setOf("elec", "pan"),
            dietType = "none",
            allergies = emptySet(),
            language = "English"
        )

        val result = StructuredRecipeParser.recipeOptions(provider.generateContent(prompt))

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.options?.size)
        assertEquals(3, result.getOrNull()?.options?.map { it.id }?.toSet()?.size)
        assertEquals("SUCCESS", diagnostics.single().category)
    }

    @Test
    fun defaultFactoryUsesTheLocalKeylessProvider() {
        val factory = DefaultAiProviderFactory()

        assertTrue(factory.provider(HardwareSettings(aiProvider = "FREE")) is LocalRecipeProvider)

        factory.close()
    }

    @Test
    fun createsAValidatedStructuredCookingPlan() = runBlocking {
        val provider = LocalRecipeProvider { }
        val prompt = PromptFactory.cookingPlanPrompt(
            recipeName = "Tomato rice",
            ingredients = listOf("tomato", "rice"),
            equipment = setOf("elec", "pan"),
            servings = 2,
            stoveType = "electric",
            stoveMaxLevel = 9,
            ovenAvailable = false,
            ovenHasFan = false,
            airfryerAvailable = false,
            dietType = "none",
            allergies = emptySet(),
            language = "English"
        )

        val result = StructuredRecipeParser.cookingPlan(provider.generateContent(prompt))
        val plan = requireNotNull(result.getOrNull()) { "Expected a structured cooking plan" }
        val validation = CookingPlanValidator(
            availableEquipment = setOf("elec", "pan"),
            stoveMaxLevel = 9,
            stoveType = "electric",
            ovenAvailable = false,
            airfryerAvailable = false,
            dietType = "none",
            allergens = emptySet(),
            servings = 2
        ).validate(plan)

        assertTrue(validation.errors.joinToString { it.message }, validation.valid)
    }

    @Test
    fun invalidStructuredRequestIsClassifiedWithoutReturningNull() {
        val diagnostics = mutableListOf<ProviderDiagnostic>()
        val provider = LocalRecipeProvider(diagnostics::add)
        val prompt = PromptFactory.recipeOptionsPrompt(
            ingredients = emptyList(),
            equipment = setOf("pan"),
            dietType = "none",
            allergies = emptySet(),
            language = "English"
        )

        val failure = expectProviderFailure { provider.generateContent(prompt) }

        assertEquals(ProviderFailureCategory.INVALID_REQUEST, failure.category)
        assertEquals("INVALID_REQUEST", diagnostics.single().category)
        assertFalse(failure.message.orEmpty().contains(prompt))
    }

    @Test
    fun malformedJsonRemainsAParserFailure() {
        assertTrue(StructuredRecipeParser.recipeOptions("not json").isFailure)
    }

    @Test
    fun diagnosticsNeverContainThePromptOrResponseText() = runBlocking {
        val diagnostics = mutableListOf<ProviderDiagnostic>()
        val provider = LocalRecipeProvider(diagnostics::add)

        val response = provider.generateContent("prompt-secret-value")
        val log = diagnostics.single().asLogMessage()

        assertTrue(response.isNotBlank())
        assertTrue(log.contains("provider=FREE_LOCAL"))
        assertTrue(log.contains("responseLength="))
        assertFalse(log.contains("prompt-secret-value"))
        assertFalse(log.contains(response))
    }

    @Test
    fun visualIngredientRequestFailsClosed() {
        val provider = LocalRecipeProvider { }

        val failure = expectProviderFailure {
            provider.generateContent("Şu görsel açıklamasındaki yiyecek malzemelerini listele")
        }

        assertEquals(ProviderFailureCategory.INVALID_REQUEST, failure.category)
    }

    private fun expectProviderFailure(block: suspend () -> Unit): ProviderFailure {
        return try {
            runBlocking { block() }
            fail("Expected ProviderFailure")
            error("unreachable")
        } catch (failure: ProviderFailure) {
            failure
        }
    }
}
