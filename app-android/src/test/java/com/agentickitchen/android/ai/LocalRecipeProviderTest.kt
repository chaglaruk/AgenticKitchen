package com.agentickitchen.android.ai

import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.shared.ai.StructuredRecipeParser
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.RecipeOptionsResponse
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

        assertTrue(factory.provider(HardwareSettings(aiProvider = "FREE")) is InventoryAwareOfflineProvider)

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

    @Test
    fun turkishChickenRiceAndGarlicProducePracticalElectricRecipes() = runBlocking {
        val ingredients = listOf("Tavuk göğsü", "Pirinç", "Sarımsak", "Zeytinyağı")
        val equipment = setOf("elec", "pan")
        val options = options(ingredients, equipment, language = "Türkçe")

        assertEquals(3, options.options.map { it.name }.toSet().size)
        assertEquals(3, options.options.map { it.summary }.toSet().size)
        assertTrue(options.options.all { equipment.containsAll(it.requiredEquipment) })

        val plan = plan(options.options.first().name, ingredients, equipment, stoveType = "electric", stoveMax = 9, language = "Türkçe")
        assertTrue(plan.steps.any { "Tavuk göğsü" in it.instruction && it.resource == "stove" })
        assertTrue(plan.steps.filter { it.resource == "stove" }.all { it.powerLevel in 1..9 })
        assertTrue(plan.safetyNotes.any { "74°C" in it })
        assertValidated(plan, equipment, "electric", 9)
    }

    @Test
    fun englishFishAndVegetablesUseOnlyTheAvailableOven() = runBlocking {
        val ingredients = listOf("Salmon", "Broccoli", "Carrot", "Olive oil")
        val equipment = setOf("oven")
        val options = options(ingredients, equipment)

        assertEquals(3, options.options.map { it.name }.toSet().size)
        assertTrue(options.options.all { it.requiredEquipment == listOf("oven") })

        val plan = plan(options.options.first().name, ingredients, equipment, stoveType = "none", oven = true)
        assertTrue(plan.steps.any { it.resource == "oven" && it.targetTemperatureC == 200 })
        assertTrue(plan.steps.none { it.resource in setOf("stove", "airfryer") })
        assertTrue(plan.safetyNotes.any { "63°C" in it })
        assertValidated(plan, equipment, "none", 9, oven = true)
    }

    @Test
    fun turkishLentilsAndVegetablesSupportAVeganPlan() = runBlocking {
        val ingredients = listOf("Kırmızı mercimek", "Domates", "Soğan", "Havuç")
        val equipment = setOf("elec", "pan")
        val options = options(ingredients, equipment, diet = "vegan", language = "Türkçe")
        val plan = plan(
            options.options.first().name,
            ingredients,
            equipment,
            stoveType = "electric",
            diet = "vegan",
            language = "Türkçe"
        )

        assertTrue(plan.steps.any { "Kırmızı mercimek" in it.instruction })
        assertValidated(plan, equipment, "electric", 9, diet = "vegan")
    }

    @Test
    fun conflictingDietAndAllergyRequestsAreClassified() {
        assertConstraintConflict(listOf("Tavuk göğsü", "Pirinç"), diet = "vegetarian")
        listOf("Yumurta", "Süt", "Kaşar peyniri", "Bal", "Somon", "Dana eti").forEach {
            assertConstraintConflict(listOf(it, "Domates"), diet = "vegan")
        }
        listOf("Yoğurt", "Beyaz peynir").forEach {
            assertConstraintConflict(listOf(it, "Domates"), allergies = setOf("milk"))
        }
        assertConstraintConflict(listOf("Karides", "Domates"), allergies = setOf("shellfish"))
    }

    @Test
    fun noStovePantryProducesThreeColdPlansWithoutHeatingResources() = runBlocking {
        val ingredients = listOf("Ekmek", "Salatalık", "Beyaz peynir", "Zeytinyağı")
        val equipment = emptySet<String>()
        val options = options(ingredients, equipment, language = "Türkçe")

        assertEquals(3, options.options.size)
        assertTrue(options.options.all { it.requiredEquipment.isEmpty() })

        val plan = plan(options.options.first().name, ingredients, equipment, stoveType = "none", language = "Türkçe")
        assertTrue(plan.steps.none { it.resource in setOf("stove", "oven", "airfryer") })
        assertValidated(plan, equipment, "none", 9)
    }

    @Test
    fun gasAndElectricPlansUseTheConfiguredHeatGuidance() = runBlocking {
        val ingredients = listOf("Chicken breast", "Rice", "Garlic")
        val gasEquipment = setOf("gas", "pan")
        val gasName = options(ingredients, gasEquipment).options.first().name
        val gasPlan = plan(gasName, ingredients, gasEquipment, stoveType = "gas")
        assertTrue(gasPlan.steps.filter { it.resource == "stove" }.all { it.powerLevel == null })
        assertTrue(gasPlan.steps.any { "gas flame" in it.instruction })

        listOf(1, 15).forEach { maximum ->
            val equipment = setOf("elec", "pan")
            val name = options(ingredients, equipment).options.first().name
            val electricPlan = plan(name, ingredients, equipment, stoveType = "electric", stoveMax = maximum)
            val powers = electricPlan.steps.mapNotNull { it.powerLevel }
            assertTrue(powers.isNotEmpty())
            assertTrue(powers.all { it in 1..maximum })
            assertTrue(maximum in powers)
        }
    }

    @Test
    fun quantitiesScaleByIngredientRoleInsteadOfOnePlaceholderAmount() = runBlocking {
        val ingredients = listOf("Chicken breast", "Rice", "Garlic", "Olive oil", "Salt")
        val equipment = setOf("elec", "pan")
        val name = options(ingredients, equipment).options.first().name
        val two = plan(name, ingredients, equipment, stoveType = "electric", servings = 2)
        val quantities = two.ingredients.associateBy { it.name }

        assertEquals(300.0, quantities.getValue("Chicken breast").quantity, 0.0)
        assertEquals(150.0, quantities.getValue("Rice").quantity, 0.0)
        assertEquals("clove", quantities.getValue("Garlic").unit)
        assertEquals("tbsp", quantities.getValue("Olive oil").unit)
        assertEquals("tsp", quantities.getValue("Salt").unit)

        val twelve = plan(name, ingredients, equipment, stoveType = "electric", servings = 12)
        assertTrue(twelve.ingredients.map { it.quantity }.toSet().size > 2)
        assertFalse(twelve.ingredients.all { it.quantity == 1_200.0 })
    }

    @Test
    fun offlineGuidanceAnswersDifferentEnglishKitchenProblems() = runBlocking {
        val responses = listOf(
            "The sauce is too thick",
            "The sauce is watery",
            "The food is burning",
            "The pan is too hot",
            "The chicken is undercooked",
            "The dish is too salty",
            "What can I substitute instead of cream?",
            "Should I reduce heat?"
        ).map { guidance(it, "English") }

        assertEquals(responses.size, responses.toSet().size)
        assertTrue(responses[0].contains("hot water"))
        assertTrue(responses[2].contains("off the heat"))
        assertTrue(responses[4].contains("safe internal temperature"))
        assertTrue(responses[7].contains("electric hob"))
    }

    @Test
    fun offlineGuidanceAnswersDifferentTurkishKitchenProblems() = runBlocking {
        val responses = listOf(
            "Sos çok koyu",
            "Sos çok sulu",
            "Yemek yanıyor",
            "Tava çok sıcak",
            "Tavuk az pişmiş",
            "Yemek çok tuzlu",
            "Krema yerine ne kullanabilirim?",
            "Ateşi artırmalı mıyım?"
        ).map { guidance(it, "Türkçe") }

        assertEquals(responses.size, responses.toSet().size)
        assertTrue(responses[0].contains("sıcak su"))
        assertTrue(responses[2].contains("su dökme"))
        assertTrue(responses[4].contains("güvenli iç sıcaklığı"))
        assertTrue(responses[7].contains("elektrikli ocak"))
    }

    @Test
    fun unsupportedOfflineGuidanceAdmitsItsLimitAndUsesTheCurrentStep() = runBlocking {
        val english = guidance("Is this ready for a dinner party?", "English")
        val turkish = guidance("Bu misafirler için uygun mu?", "Türkçe")

        assertTrue(english.contains("cannot determine that precisely"))
        assertTrue(english.contains("Simmer the rice"))
        assertTrue(turkish.contains("tam olarak belirleyemiyor"))
        assertTrue(turkish.contains("Simmer the rice"))
    }

    private suspend fun options(
        ingredients: List<String>,
        equipment: Set<String>,
        diet: String = "none",
        allergies: Set<String> = emptySet(),
        language: String = "English"
    ): RecipeOptionsResponse {
        val response = LocalRecipeProvider { }.generateContent(
            PromptFactory.recipeOptionsPrompt(ingredients, equipment, diet, allergies, language)
        )
        return requireNotNull(StructuredRecipeParser.recipeOptions(response).getOrNull())
    }

    private suspend fun plan(
        recipeName: String,
        ingredients: List<String>,
        equipment: Set<String>,
        servings: Int = 2,
        stoveType: String,
        stoveMax: Int = 9,
        oven: Boolean = false,
        airfryer: Boolean = false,
        diet: String = "none",
        allergies: Set<String> = emptySet(),
        language: String = "English"
    ): CookingPlanResponse {
        val response = LocalRecipeProvider { }.generateContent(
            PromptFactory.cookingPlanPrompt(
                recipeName,
                ingredients,
                equipment,
                servings,
                stoveType,
                stoveMax,
                oven,
                false,
                airfryer,
                diet,
                allergies,
                language
            )
        )
        return requireNotNull(StructuredRecipeParser.cookingPlan(response).getOrNull())
    }

    private suspend fun guidance(question: String, language: String): String =
        LocalRecipeProvider { }.generateContent(
            """
                Kitchen guidance request
                Recipe: Tomato rice
                Current step: Simmer the rice
                Stove type: electric
                Language: $language
                Question: $question
            """.trimIndent()
        )

    private fun assertValidated(
        plan: CookingPlanResponse,
        equipment: Set<String>,
        stoveType: String,
        stoveMax: Int,
        oven: Boolean = false,
        airfryer: Boolean = false,
        diet: String = "none",
        allergies: Set<String> = emptySet()
    ) {
        val result = CookingPlanValidator(
            equipment,
            stoveMax,
            stoveType,
            oven,
            airfryer,
            diet,
            allergies,
            plan.servings
        ).validate(plan)
        assertTrue(result.errors.joinToString { it.message }, result.valid)
    }

    private fun assertConstraintConflict(
        ingredients: List<String>,
        diet: String = "none",
        allergies: Set<String> = emptySet()
    ) {
        val failure = expectProviderFailure {
            LocalRecipeProvider { }.generateContent(
                PromptFactory.recipeOptionsPrompt(
                    ingredients,
                    setOf("elec", "pan"),
                    diet,
                    allergies,
                    "Türkçe"
                )
            )
        }
        assertEquals(ProviderFailureCategory.CONSTRAINT_CONFLICT, failure.category)
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
