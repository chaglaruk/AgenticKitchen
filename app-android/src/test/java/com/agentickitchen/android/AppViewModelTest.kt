package com.agentickitchen.android

import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.HuggingFaceVisionService
import com.agentickitchen.android.ai.LlmProvider
import com.agentickitchen.android.ai.ProviderFailure
import com.agentickitchen.android.ai.ProviderFailureCategory
import com.agentickitchen.android.app.AppViewModelFactory
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.ScheduleEvent
import com.agentickitchen.shared.models.ScheduleResult
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppViewModelTest {
    @Test
    fun initialStateAndSettingsActionsUseInjectedDependencies() {
        val preferences = FakePreferences()
        val history = FakeHistoryRepository()
        val viewModel = newViewModel(preferences, history)

        assertEquals(true, viewModel.setupDone.value)
        assertEquals(setOf("oven", "pan"), viewModel.selectedEquipment.value)
        assertEquals("dark", viewModel.theme.value)
        assertEquals("English", viewModel.language.value)
        assertEquals("vegetarian", viewModel.dietSettings.value.dietType)
        assertEquals(1, viewModel.history.value.size)

        viewModel.completeSetup(setOf("gas"), HardwareSettings(stoveType = "gas"))
        viewModel.saveDietSettings(DietSettings("vegan", setOf("nuts")))
        viewModel.setTheme("heritage")
        viewModel.setLanguage("Türkçe")

        assertEquals(setOf("gas"), preferences.savedEquipment)
        assertEquals("gas", preferences.hardware.stoveType)
        assertEquals("vegan", preferences.diet.dietType)
        assertEquals("heritage", preferences.themeValue)
        assertEquals("Türkçe", preferences.languageValue)
    }

    @Test
    fun factoryUsesSuppliedFakesAndRejectsUnsupportedModels() {
        val preferences = FakePreferences()
        val history = FakeHistoryRepository()
        val factory = AppViewModelFactory(
            preferences, history, FakeOrchestrator, FakePantryIntelAgent, FakeProviderFactory, TargetTimeResolver()
        )

        val viewModel = factory.create(AppViewModel::class.java)

        assertEquals(preferences.setupDone(), viewModel.setupDone.value)
        try {
            factory.create(androidx.lifecycle.ViewModel::class.java)
            fail("Unsupported ViewModel was accepted")
        } catch (_: IllegalArgumentException) {
            // Expected: the factory only creates the production ViewModel type.
        }
    }

    @Test
    fun languageSelectionUpdatesTheSharedVisibleLanguageState() {
        val preferences = FakePreferences()
        val viewModel = newViewModel(preferences, FakeHistoryRepository())

        viewModel.setLanguage(L.English)

        assertEquals(L.English, preferences.languageValue)
        assertFalse(L.isTr)

        viewModel.setLanguage(L.Turkish)

        assertEquals(L.Turkish, preferences.languageValue)
        assertTrue(L.isTr)
    }

    @Test
    fun providerSelectionUsesTheSelectedSupportedProviderAndKeyRules() {
        val factory = RecordingProviderFactory()

        CookingProviderSelection.provider(factory, HardwareSettings(aiProvider = CookingProviderSelection.DuckDuckGo))

        assertEquals(CookingProviderSelection.Free, factory.receivedProviderId)
        assertFalse(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = CookingProviderSelection.DuckDuckGo)))
        assertTrue(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = CookingProviderSelection.Gemini)))
        assertTrue(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = CookingProviderSelection.HuggingFace)))
        assertEquals(CookingProviderSelection.Free, CookingProviderSelection.normalize("LEGACY"))
    }

    @Test
    fun missingVisualCaptionDoesNotCreateAGenericIngredientPrompt() {
        assertNull(imageDerivedIngredientPrompt(null))
        assertTrue(imageDerivedIngredientPrompt("a bowl of tomatoes")!!.contains("a bowl of tomatoes"))
    }

    @Test
    fun aiFailuresUseReaderSafeMessages() {
        L.applyLanguage(L.English)
        assertEquals("The selected provider is missing its credential. Add it in Settings.", readerSafeAiError(Exception("API_KEY_MISSING")))
        assertEquals("The provider is busy or has reached its usage limit. Try again shortly.", readerSafeAiError(Exception("429 rate limit")))
        assertEquals("Could not connect. Check your internet connection and try again.", readerSafeAiError(Exception("network timeout")))
        assertEquals(
            "Could not connect. Check your internet connection and try again.",
            readerSafeAiError(ProviderFailure("FREE_LOCAL", ProviderFailureCategory.NETWORK))
        )
        assertEquals(
            "The selected ingredients conflict with the diet, allergy, or safe cooking setup.",
            readerSafeAiError(ProviderFailure("FREE_LOCAL", ProviderFailureCategory.CONSTRAINT_CONFLICT))
        )
        assertNotEquals("provider exploded", readerSafeAiError(Exception("provider exploded")))
        L.applyLanguage(L.Turkish)
    }

    @Test
    fun ingredientDraftRestoresInOrderWithoutDuplicatesAndRefreshesPantryIntel() {
        val preferences = FakePreferences().apply {
            ingredientDraftValue = listOf("Domates", "Pirinç", "domates", "Kaşar peyniri")
        }
        val pantry = RecordingPantryIntelAgent()

        val viewModel = newViewModel(preferences, FakeHistoryRepository(), pantry)

        assertEquals(listOf("Domates", "Pirinç", "Kaşar peyniri"), viewModel.chips.value)
        assertEquals(viewModel.chips.value, pantry.lastIngredients)
        assertTrue(newViewModel(FakePreferences(), FakeHistoryRepository()).chips.value.isEmpty())
    }

    @Test
    fun ingredientDraftMutationsPersistTheirOrderedResult() {
        val preferences = FakePreferences()
        val viewModel = newViewModel(preferences, FakeHistoryRepository())

        viewModel.addChip("Domates")
        assertEquals(listOf("Domates"), preferences.ingredientDraftValue)

        viewModel.addMultipleChips(listOf("Pirinç", "domates", "Kaşar peyniri", "pirinç"))
        assertEquals(listOf("Domates", "Pirinç", "Kaşar peyniri"), preferences.ingredientDraftValue)

        viewModel.removeChip("Pirinç")
        assertEquals(listOf("Domates", "Kaşar peyniri"), preferences.ingredientDraftValue)

        viewModel.clearAll()
        assertTrue(preferences.ingredientDraftValue.isEmpty())
    }

    @Test
    fun activeRecipeStateRetainsTheValidatedPlanAndResolvedReadyTime() {
        val plan = CookingPlanResponse(
            recipeName = "Soup",
            servings = 4,
            ingredients = listOf(PlannedIngredientDto("Tomato", 500.0, "g")),
            steps = listOf(CookingStepDto("prep", "prep", "Chop tomato", "counter", 120)),
            safetyNotes = listOf("Wash produce")
        )
        val events = listOf(ScheduleEvent("prep", "2026-07-30T18:00:00Z", "2026-07-30T18:02:00Z", "Chop tomato", "counter"))

        val state = activeRecipeState(
            RecipeOption("1", "Easy", "Soup", "Tomato soup"),
            events,
            4,
            "2026-07-30T19:00:00Z",
            plan
        )

        assertSame(plan, state.cookingPlan)
        assertEquals(events, state.events)
        assertEquals("2026-07-30T19:00:00Z", state.resolvedReadyTimeIso)
        assertEquals(4, state.servings)
    }

    @Test
    fun canonicalAllergiesSurviveViewModelRecreationWithoutDuplicates() {
        val preferences = FakePreferences()
        val first = newViewModel(preferences, FakeHistoryRepository())
        val normalized = AllergyCatalog.normalize(setOf("dairy", "milk", "Susam", "  my custom  "))

        first.saveDietSettings(DietSettings("none", normalized))
        val recreated = newViewModel(preferences, FakeHistoryRepository())

        assertEquals(setOf("milk", "sesame", "custom:my_custom"), recreated.dietSettings.value.allergies)
    }

    private fun newViewModel(
        preferences: FakePreferences,
        history: FakeHistoryRepository,
        pantryIntelAgent: PantryIntelAgent = FakePantryIntelAgent
    ) = AppViewModel(
        preferences, history, FakeOrchestrator, pantryIntelAgent, FakeProviderFactory, TargetTimeResolver()
    )

    private class FakePreferences : AppPreferences {
        var setup = true
        var equipmentValue = setOf("oven", "pan")
        var hardware = HardwareSettings(stoveType = "electric")
        var diet = DietSettings("vegetarian", setOf("dairy"))
        var themeValue = "dark"
        var languageValue = "English"
        var savedEquipment = emptySet<String>()
        var ingredientDraftValue = emptyList<String>()

        override fun setupDone() = setup
        override fun saveSetup(done: Boolean, equipment: Set<String>) {
            setup = done
            equipmentValue = equipment
            savedEquipment = equipment
        }
        override fun equipment() = equipmentValue
        override fun hardwareSettings() = hardware
        override fun saveHardwareSettings(settings: HardwareSettings) { hardware = settings }
        override fun dietSettings() = diet
        override fun saveDietSettings(settings: DietSettings) { diet = settings }
        override fun theme() = themeValue
        override fun saveTheme(theme: String) { themeValue = theme }
        override fun language() = languageValue
        override fun saveLanguage(language: String) { languageValue = language }
        override fun ingredientDraft() = ingredientDraftValue
        override fun saveIngredientDraft(ingredients: List<String>) { ingredientDraftValue = ingredients }
    }

    private class FakeHistoryRepository : RecipeHistoryRepository {
        private val entries = mutableListOf(RecipeHistory("1", "Soup", "tomato", "2026-01-01", "started"))
        override fun getAllHistory() = entries.toList()
        override fun insertRecipe(id: String, name: String, ingredients: String, timestamp: String, status: String) {
            entries += RecipeHistory(id, name, ingredients, timestamp, status)
        }
        override fun deleteRecipe(id: String) { entries.removeAll { it.id == id } }
    }

    private object FakeOrchestrator : Orchestrator {
        override suspend fun startSession(session: com.agentickitchen.shared.models.RecipeSession) = ScheduleResult()
    }

    private object FakePantryIntelAgent : PantryIntelAgent {
        override fun analyze(ingredients: List<String>, equipment: Set<String>, dietType: String) = PantryIntelReport(
            readinessScore = 0, focusCategoryId = "none", focusCategoryLabel = "None",
            categoryBreakdown = emptyList(), warnings = emptyList(), tactics = emptyList(), equipmentLane = "none"
        )
    }

    private class RecordingPantryIntelAgent : PantryIntelAgent {
        var lastIngredients = emptyList<String>()

        override fun analyze(ingredients: List<String>, equipment: Set<String>, dietType: String): PantryIntelReport {
            lastIngredients = ingredients
            return PantryIntelReport(
                readinessScore = 0, focusCategoryId = "none", focusCategoryLabel = "None",
                categoryBreakdown = emptyList(), warnings = emptyList(), tactics = emptyList(), equipmentLane = "none"
            )
        }
    }

    private object FakeProviderFactory : AiProviderFactory {
        override fun provider(settings: HardwareSettings): LlmProvider? = null
        override fun gemini(settings: HardwareSettings, model: String) = null
        override fun vision(settings: HardwareSettings): HuggingFaceVisionService = error("Vision is not used in this JVM test")
        override fun close() = Unit
    }

    private class RecordingProviderFactory : AiProviderFactory {
        var receivedProviderId: String? = null

        override fun provider(settings: HardwareSettings): LlmProvider? {
            receivedProviderId = settings.aiProvider
            return null
        }

        override fun gemini(settings: HardwareSettings, model: String) = null
        override fun vision(settings: HardwareSettings): HuggingFaceVisionService = error("Vision is not used in this JVM test")
        override fun close() = Unit
    }
}
