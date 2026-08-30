package com.agentickitchen.android

import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.ProviderFailure
import com.agentickitchen.android.ai.ProviderFailureCategory
import com.agentickitchen.android.app.AppViewModelFactory
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.inventory.InventoryAdjustmentRecord
import com.agentickitchen.shared.inventory.PantryInventoryRepository
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.PendingRecipeUsageRecord
import com.agentickitchen.shared.inventory.ActiveCookingSessionRecord
import com.agentickitchen.shared.inventory.AdjustmentMode
import com.agentickitchen.shared.inventory.AdjustmentReason
import com.agentickitchen.shared.inventory.UnitDimension
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.ai.dto.PlannedIngredientDto
import com.agentickitchen.shared.validator.ErrorType
import com.agentickitchen.shared.validator.ValidationError
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.ScheduleEvent
import com.agentickitchen.shared.models.ScheduleResult
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AppViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @org.junit.After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
            preferences,
            history,
            FakeInventoryRepository(),
            FakeOrchestrator,
            FakePantryIntelAgent,
            FakeProviderFactory,
            TargetTimeResolver()
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

        CookingProviderSelection.provider(factory, HardwareSettings(aiProvider = "DUCKDUCKGO"))

        assertEquals(CookingProviderSelection.Firebase, factory.receivedProviderId)
        assertFalse(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = "DUCKDUCKGO")))
        assertFalse(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = CookingProviderSelection.Firebase)))
        assertTrue(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = CookingProviderSelection.Gemini)))
        assertFalse(CookingProviderSelection.needsApiKey(HardwareSettings(aiProvider = "HUGGINGFACE")))
        assertEquals(CookingProviderSelection.Firebase, CookingProviderSelection.normalize("LEGACY"))
    }

    @Test
    fun savingVerifiedGeminiKeyAlsoSelectsGeminiProvider() {
        val preferences = FakePreferences()
        val viewModel = newViewModel(preferences, FakeHistoryRepository())

        viewModel.saveApiKey("test-key")

        assertEquals("test-key", preferences.hardware.geminiApiKey)
        assertEquals(CookingProviderSelection.Gemini, preferences.hardware.aiProvider)
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
    fun validationFailuresKeepReaderMessagesSpecificToTheirReason() {
        L.applyLanguage(L.English)
        assertEquals(
            "This recipe conflicts with your diet or allergy preferences. Choose another recipe.",
            readerSafePlanValidationError(listOf(ValidationError(ErrorType.ALLERGEN_CONFLICT, "ingredients", "unsafe detail")))
        )
        assertEquals(
            "This recipe cannot be prepared safely with your kitchen setup. Choose another recipe.",
            readerSafePlanValidationError(listOf(ValidationError(ErrorType.UNAVAILABLE_EQUIPMENT, "steps[0]", "unsafe detail")))
        )
        assertEquals(
            "Some recipe quantities could not be understood. Try again.",
            readerSafePlanValidationError(listOf(ValidationError(ErrorType.UNKNOWN_UNIT, "ingredients[0]", "unsafe detail")))
        )
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
        val preferences = FakePreferences().apply { languageValue = L.Turkish }
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
            sessionId = "session",
            option = RecipeOption("1", "Easy", "Soup", "Tomato soup"),
            events = events,
            servings = 4,
            readyTimeIso = "2026-07-30T19:00:00Z",
            plan = plan
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

    @Test
    fun draftUndoRestoresExactOrderAfterMultipleRemovals() {
        val preferences = FakePreferences().apply {
            ingredientDraftValue = listOf("Tomato", "Onion", "Rice", "Cheese")
        }
        val viewModel = newViewModel(preferences, FakeHistoryRepository())
        val onionRemoval = UiEvent.DraftIngredientRemoved(
            "Onion",
            1,
            listOf("Tomato", "Onion", "Rice", "Cheese"),
            "Onion removed"
        )
        val riceRemoval = UiEvent.DraftIngredientRemoved(
            "Rice",
            2,
            listOf("Tomato", "Onion", "Rice", "Cheese"),
            "Rice removed"
        )

        viewModel.removeChip("Onion")
        viewModel.removeChip("Rice")
        viewModel.restoreRemovedChip(onionRemoval)
        viewModel.restoreRemovedChip(riceRemoval)

        assertEquals(listOf("Tomato", "Onion", "Rice", "Cheese"), viewModel.chips.value)
        assertEquals(viewModel.chips.value, preferences.ingredientDraftValue)
    }

    @Test
    fun inventoryPersistsAcrossViewModelRecreation() {
        val inventory = FakeInventoryRepository()
        val preferences = FakePreferences().apply { ingredientDraftValue = listOf("Tomato") }
        val first = newViewModel(preferences, FakeHistoryRepository(), inventory = inventory)

        assertTrue(first.inventory.value.isEmpty())

        first.saveInventoryItem(null, "Milk", 1.5, "L", "bottle")
        val recreated = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inventory)

        assertEquals(1, recreated.inventory.value.size)
        assertEquals(1500.0, recreated.inventory.value.single().quantity, 0.0)
        assertEquals("ml", recreated.inventory.value.single().unit)
    }

    @Test
    fun editingInventoryKeepsTheItemUntilExplicitDelete() {
        val inventory = FakeInventoryRepository()
        val viewModel = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inventory)
        viewModel.saveInventoryItem(null, "Eggs", 6.0, "adet", null)
        val item = viewModel.inventory.value.single()

        viewModel.saveInventoryItem(item, "Eggs", 12.0, "adet", null)

        assertEquals(1, viewModel.inventory.value.size)
        assertEquals(12.0, viewModel.inventory.value.single().quantity, 0.0)
        assertEquals(2, viewModel.inventoryAdjustments.value.getValue(item.id).size)

        viewModel.deleteInventoryItem(viewModel.inventory.value.single())
        assertTrue(viewModel.inventory.value.isEmpty())
    }

    // ─── Consumption & Cancellation Tests ───

    @Test
    fun plannedConsumptionRemovesOnlyTargetSessionAndExposesNextPending() {
        val inv = FakeInventoryRepository()
        val vm = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inv)
        val now = "2026-01-01T00:00:00Z"
        val sidA = "A"; val sidB = "B"
        inv.upsert(
            PantryStockItem("i1", null, "Tomato", null, null, 1000.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a1", "i1", 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.upsert(
            PantryStockItem("i2", null, "Onion", null, null, 500.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a2", "i2", 500.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.reserve(listOf(PendingRecipeUsageRecord(sidA, "i1", 300.0, "g", status = "reserved", timestamp = now)))
        inv.reserve(listOf(PendingRecipeUsageRecord(sidB, "i2", 200.0, "g", status = "reserved", timestamp = now)))
        vm.refreshInventory()
        vm.refreshPendingConsumptions()
        assertEquals(2, vm.allPendingConsumptions.value.size)

        vm.consumePlannedInventory(sidA)

        assertEquals(700.0, vm.inventory.value.first { it.id == "i1" }.quantity, 0.0)
        assertEquals(500.0, vm.inventory.value.first { it.id == "i2" }.quantity, 0.0)
        assertEquals(1, vm.allPendingConsumptions.value.size)
        assertEquals(sidB, vm.allPendingConsumptions.value.first().sessionId)
        assertNotNull(vm.pendingConsumption.value)
        assertEquals(sidB, vm.pendingConsumption.value!!.sessionId)
    }

    @Test
    fun actualAmountsOverridePlannedAmounts() {
        val inv = FakeInventoryRepository()
        val vm = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inv)
        val now = "2026-01-01T00:00:00Z"
        val sid = "X"
        inv.upsert(
            PantryStockItem("i1", null, "Carrot", null, null, 1000.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a1", "i1", 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.reserve(listOf(PendingRecipeUsageRecord(sid, "i1", 500.0, "g", status = "reserved", timestamp = now)))
        vm.refreshInventory(); vm.refreshPendingConsumptions()

        vm.consumeActualInventory(mapOf("i1" to 300.0), sid)

        assertEquals(700.0, vm.inventory.value.first { it.id == "i1" }.quantity, 0.0)
        assertTrue(vm.allPendingConsumptions.value.isEmpty())
    }

    @Test
    fun invalidActualAmountsDoNotMutateState() {
        val inv = FakeInventoryRepository()
        val vm = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inv)
        val now = "2026-01-01T00:00:00Z"
        val sid = "s1"
        inv.upsert(
            PantryStockItem("i1", null, "Rice", null, null, 1000.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a1", "i1", 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.reserve(listOf(PendingRecipeUsageRecord(sid, "i1", 300.0, "g", status = "reserved", timestamp = now)))
        vm.refreshInventory(); vm.refreshPendingConsumptions()

        for (bad in listOf(0.0, -100.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            vm.consumeActualInventory(mapOf("i1" to bad), sid)
            assertEquals(1000.0, vm.inventory.value.first { it.id == "i1" }.quantity, 0.0)
            assertEquals(1, vm.allPendingConsumptions.value.size)
        }
    }

    @Test
    fun repositoryConsumeFailurePreservesPendingGroup() {
        val failing = FailingConsumeInventory()
        val vm = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = failing)
        val now = "2026-01-01T00:00:00Z"
        val sid = "sX"
        failing.upsert(
            PantryStockItem("iX", null, "Flour", null, null, 1000.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("aX", "iX", 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        failing.reserve(listOf(PendingRecipeUsageRecord(sid, "iX", 300.0, "g", status = "reserved", timestamp = now)))
        vm.refreshInventory(); vm.refreshPendingConsumptions()
        assertEquals(1, vm.allPendingConsumptions.value.size)

        vm.consumeActualInventory(mapOf("iX" to 300.0), sid)

        assertEquals(1000.0, vm.inventory.value.first { it.id == "iX" }.quantity, 0.0)
        assertEquals(1, vm.allPendingConsumptions.value.size)
        assertEquals(sid, vm.allPendingConsumptions.value.first().sessionId)
    }

    @Test
    fun cancellingOneSessionReleasesReservationAndExposesNextPending() {
        val inv = FakeInventoryRepository()
        val vm = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inv)
        val now = "2026-01-01T00:00:00Z"
        val sidA = "A"; val sidB = "B"
        inv.upsert(
            PantryStockItem("i1", null, "Basil", null, null, 1000.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a1", "i1", 1000.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.upsert(
            PantryStockItem("i2", null, "Oregano", null, null, 500.0, "g", UnitDimension.WEIGHT, null, false, null, "manual", now, now),
            InventoryAdjustmentRecord("a2", "i2", 500.0, AdjustmentMode.DELTA, AdjustmentReason.MANUAL_ADD, "test", now)
        )
        inv.reserve(listOf(PendingRecipeUsageRecord(sidA, "i1", 300.0, "g", status = "reserved", timestamp = now)))
        inv.reserve(listOf(PendingRecipeUsageRecord(sidB, "i2", 200.0, "g", status = "reserved", timestamp = now)))
        vm.refreshInventory(); vm.refreshPendingConsumptions()
        assertEquals(2, vm.allPendingConsumptions.value.size)

        vm.cancelInventoryConsumption(sidA)

        assertEquals(1000.0, vm.inventory.value.first { it.id == "i1" }.quantity, 0.0)
        assertEquals(1, vm.allPendingConsumptions.value.size)
        assertEquals(sidB, vm.allPendingConsumptions.value.first().sessionId)
        assertNotNull(vm.pendingConsumption.value)
        assertEquals(sidB, vm.pendingConsumption.value!!.sessionId)
    }

    @Test
    fun staleRunningSessionIsPersistedAsCompletedWhenRestoreFindsElapsedPlan() {
        val inventory = FakeInventoryRepository()
        val now = System.currentTimeMillis()
        inventory.saveActiveSession(
            ActiveCookingSessionRecord(
                sessionId = "stale-session",
                recipeOptionId = "option",
                recipeName = "Timed recipe",
                recipeType = "test",
                description = "description",
                servings = 2,
                resolvedReadyTimeIso = "2026-08-30T12:00:00Z",
                cookingPlanJson = """{"recipeName":"Timed recipe","servings":2,"ingredients":[],"steps":[],"safetyNotes":[]}""",
                eventsJson = """[{"id":"step","startIso":"2026-08-30T12:00:00Z","endIso":"2026-08-30T12:00:01Z","instruction":"Wait","resource":"counter"}]""",
                plannedUsageJson = "[]",
                status = "RUNNING",
                startedAtMillis = now - 120_000,
                accumulatedElapsedSeconds = 0,
                lastRunningStartMillis = now - 120_000,
                pausedAtMillis = null,
                completedStepIdsJson = "[]",
                skippedStepIdsJson = "[]",
                recentChatTurnsJson = "[]",
                updatedAtIso = "2026-08-30T12:00:00Z"
            )
        )

        newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inventory)

        assertEquals("COMPLETED", inventory.getActiveSession("stale-session")?.status)
    }

    @Test
    fun completedRestoredSessionWithoutPendingUsageIsDeletedWhenReturningToRecipes() {
        val inventory = FakeInventoryRepository()
        val now = System.currentTimeMillis()
        inventory.saveActiveSession(
            ActiveCookingSessionRecord(
                sessionId = "completed-session",
                recipeOptionId = "option",
                recipeName = "Completed recipe",
                recipeType = "test",
                description = "description",
                servings = 2,
                resolvedReadyTimeIso = "2026-08-30T12:00:00Z",
                cookingPlanJson = """{"recipeName":"Completed recipe","servings":2,"ingredients":[],"steps":[],"safetyNotes":[]}""",
                eventsJson = "[]",
                plannedUsageJson = "[]",
                status = "COMPLETED",
                startedAtMillis = now - 120_000,
                accumulatedElapsedSeconds = 120,
                completedStepIdsJson = "[]",
                skippedStepIdsJson = "[]",
                recentChatTurnsJson = "[]",
                updatedAtIso = "2026-08-30T12:00:00Z"
            )
        )
        val viewModel = newViewModel(FakePreferences(), FakeHistoryRepository(), inventory = inventory)

        viewModel.backToOptions()

        assertEquals(null, inventory.getActiveSession("completed-session"))
    }

    private fun newViewModel(
        preferences: FakePreferences,
        history: FakeHistoryRepository,
        pantryIntelAgent: PantryIntelAgent = FakePantryIntelAgent,
        inventory: PantryInventoryRepository = FakeInventoryRepository()
    ) = AppViewModel(
        preferences,
        history,
        inventory,
        FakeOrchestrator,
        pantryIntelAgent,
        FakeProviderFactory,
        TargetTimeResolver()
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

    private class FakeInventoryRepository : PantryInventoryRepository {
        private val items = linkedMapOf<String, PantryStockItem>()
        private val adjustments = mutableListOf<InventoryAdjustmentRecord>()
        private val pending = mutableListOf<PendingRecipeUsageRecord>()
        private val activeSessions = mutableMapOf<String, ActiveCookingSessionRecord>()

        override fun getAll() = items.values.toList()
        override fun upsert(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
            items[item.id] = item
            adjustments += adjustment
        }
        override fun delete(item: PantryStockItem, adjustment: InventoryAdjustmentRecord) {
            adjustments += adjustment
            items.remove(item.id)
        }
        override fun adjustments(itemId: String) = adjustments.filter { it.itemId == itemId }
        override fun pendingUsage(sessionId: String) = pending.filter { it.sessionId == sessionId }
        override fun allPendingUsage() = pending.toList()
        override fun upsertPendingUsage(usage: PendingRecipeUsageRecord) {
            pending.removeAll { it.sessionId == usage.sessionId && it.itemId == usage.itemId }
            pending += usage
        }
        override fun deletePendingUsage(sessionId: String) {
            pending.removeAll { it.sessionId == sessionId }
        }
        override fun applyMutations(mutations: List<com.agentickitchen.shared.inventory.InventoryMutation>) {
            mutations.forEach {
                items[it.item.id] = it.item
                adjustments += it.adjustment
            }
        }
        override fun reserve(usages: List<PendingRecipeUsageRecord>): Boolean {
            usages.forEach(::upsertPendingUsage)
            return true
        }
        override fun releaseReservation(sessionId: String): Boolean {
            pending.filter { it.sessionId == sessionId }.forEach { usage ->
                adjustments += InventoryAdjustmentRecord(
                    id = "$sessionId:${usage.itemId}:release",
                    itemId = usage.itemId,
                    amount = usage.plannedQuantity,
                    mode = AdjustmentMode.DELTA,
                    reason = AdjustmentReason.RECIPE_RESERVATION_RELEASE,
                    source = "recipe",
                    timestamp = "now"
                )
            }
            deletePendingUsage(sessionId)
            deleteActiveSession(sessionId)
            return true
        }
        override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean {
            pendingUsage(sessionId).forEach { usage ->
                val item = items[usage.itemId] ?: return false
                val amount = actualQuantities[usage.itemId] ?: usage.plannedQuantity
                if (amount > item.quantity) return false
                items[item.id] = item.copy(quantity = item.quantity - amount)
            }
            deletePendingUsage(sessionId)
            deleteActiveSession(sessionId)
            return true
        }
        override fun saveActiveSession(session: ActiveCookingSessionRecord) {
            activeSessions[session.sessionId] = session
        }
        override fun getActiveSession(sessionId: String) = activeSessions[sessionId]
        override fun getAllActiveSessions() = activeSessions.values.toList()
        override fun deleteActiveSession(sessionId: String) { activeSessions.remove(sessionId) }
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
        override fun provider(settings: HardwareSettings): KitchenAiProvider? = null
        override fun close() = Unit
    }

    private class RecordingProviderFactory : AiProviderFactory {
        var receivedProviderId: String? = null

        override fun provider(settings: HardwareSettings): KitchenAiProvider? {
            receivedProviderId = settings.aiProvider
            return null
        }

        override fun close() = Unit
    }

    private class FailingConsumeInventory : PantryInventoryRepository by FakeInventoryRepository() {
        override fun consume(sessionId: String, actualQuantities: Map<String, Double>): Boolean = false
    }
}
