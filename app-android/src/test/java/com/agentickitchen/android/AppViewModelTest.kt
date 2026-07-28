package com.agentickitchen.android

import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.HuggingFaceVisionService
import com.agentickitchen.android.ai.LlmProvider
import com.agentickitchen.android.app.AppViewModelFactory
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.ScheduleResult
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
        assertEquals("18:30", viewModel.mealTime.value)
        assertEquals("dark", viewModel.theme.value)
        assertEquals("English", viewModel.language.value)
        assertEquals("vegetarian", viewModel.dietSettings.value.dietType)
        assertEquals(1, viewModel.history.value.size)

        viewModel.completeSetup(setOf("gas"), 4, "20:00", HardwareSettings(stoveType = "gas"))
        viewModel.saveDietSettings(DietSettings("vegan", setOf("nuts")))
        viewModel.setTheme("heritage")
        viewModel.setLanguage("Türkçe")

        assertEquals(setOf("gas"), preferences.savedEquipment)
        assertEquals(4, preferences.savedServings)
        assertEquals("20:00", preferences.savedMealTime)
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

        assertEquals(CookingProviderSelection.DuckDuckGo, factory.receivedProviderId)
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
        assertNotEquals("provider exploded", readerSafeAiError(Exception("provider exploded")))
        L.applyLanguage(L.Turkish)
    }

    @Test
    fun cookingPlanPromptIsCalmAndKeepsItsStructuredContract() {
        val prompt = calmCookingPlanPrompt("You are a military-precision chef AI. Use military precision: \"Set burner to level X for Y minutes\". type|instruction|durationMinutes")

        assertFalse(prompt.contains("military", ignoreCase = true))
        assertTrue(prompt.contains("type|instruction|durationMinutes"))
    }

    private fun newViewModel(preferences: FakePreferences, history: FakeHistoryRepository) = AppViewModel(
        preferences, history, FakeOrchestrator, FakePantryIntelAgent, FakeProviderFactory, TargetTimeResolver()
    )

    private class FakePreferences : AppPreferences {
        var setup = true
        var equipmentValue = setOf("oven", "pan")
        var mealTimeValue = "18:30"
        var hardware = HardwareSettings(stoveType = "electric")
        var diet = DietSettings("vegetarian", setOf("dairy"))
        var themeValue = "dark"
        var languageValue = "English"
        var savedEquipment = emptySet<String>()
        var savedServings = 0
        var savedMealTime = ""

        override fun setupDone() = setup
        override fun saveSetup(done: Boolean, equipment: Set<String>, servings: Int, mealTime: String) {
            setup = done
            equipmentValue = equipment
            savedEquipment = equipment
            savedServings = servings
            savedMealTime = mealTime
            mealTimeValue = mealTime
        }
        override fun equipment() = equipmentValue
        override fun mealTime() = mealTimeValue
        override fun hardwareSettings() = hardware
        override fun saveHardwareSettings(settings: HardwareSettings) { hardware = settings }
        override fun dietSettings() = diet
        override fun saveDietSettings(settings: DietSettings) { diet = settings }
        override fun theme() = themeValue
        override fun saveTheme(theme: String) { themeValue = theme }
        override fun language() = languageValue
        override fun saveLanguage(language: String) { languageValue = language }
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
