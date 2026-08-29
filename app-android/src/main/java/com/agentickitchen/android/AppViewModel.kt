package com.agentickitchen.android

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.models.*
import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.inventory.AdjustmentMode
import com.agentickitchen.shared.inventory.AdjustmentReason
import com.agentickitchen.shared.inventory.InventoryAdjustmentRecord
import com.agentickitchen.shared.inventory.InventoryWorkflow
import com.agentickitchen.shared.inventory.PlannedPantryUsage
import com.agentickitchen.shared.inventory.PendingRecipeUsageRecord
import com.agentickitchen.shared.inventory.ShoppingImportMode
import com.agentickitchen.shared.inventory.InventoryUnits
import com.agentickitchen.shared.inventory.PantryInventoryRepository
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.ActiveCookingSessionRecord
import com.agentickitchen.shared.inventory.LocalIngredientResolver
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.ProviderFailure
import com.agentickitchen.android.ai.ProviderFailureCategory
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.AiFailureType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingChatTurn
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.ShoppingTextRequest
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.scheduler.TargetTimeChoice
import com.agentickitchen.shared.validator.CookingPlanValidator
import com.agentickitchen.shared.validator.ErrorType
import com.agentickitchen.shared.validator.ValidationError
import com.agentickitchen.shared.validator.normalizeCookingPlan
import com.agentickitchen.shared.cooking.CookingSessionController
import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// ── Dil ───────────────────────────────────────────────────────────────────
object L {
    const val Turkish = "Türkçe"
    const val English = "English"

    private val selectedLanguage = mutableStateOf(deviceLanguage())

    val isTr: Boolean get() = selectedLanguage.value == Turkish

    fun normalize(language: String): String = when (language) {
        Turkish, English -> language
        else -> deviceLanguage()
    }

    fun applyLanguage(language: String) {
        selectedLanguage.value = normalize(language)
    }

    private fun deviceLanguage() = if (Locale.getDefault().language == "tr") Turkish else English
    val appTagline get() = if (isTr) "Mutfağında daha rahat pişir" else "Cook with more confidence"
    val addIngredient get() = if (isTr) "Malzeme ekle..." else "Type an ingredient..."
    val clearAll get() = if (isTr) "Temizle" else "Clear"
    val generatePlan get() = if (isTr) "Tarif Alternatifleri Üret" else "Generate Options"
    val thinking get() = if (isTr) "Tariflere bakıyorum…" else "Finding a few good options…"
    val noIngredientError get() = if (isTr) "Lütfen en az bir malzeme ekleyin." else "Please add at least one ingredient."
    val noEquipmentError get() = if (isTr) "Pişirme aracı seçilmedi." else "No cooking equipment selected."
    val ingredients get() = if (isTr) "Malzemeler" else "Ingredients"
    val settings get() = if (isTr) "Ayarlar" else "Settings"
    val home get() = if (isTr) "Ana Sayfa" else "Home"
    val setupTitle get() = if (isTr) "Mutfağını Tanıtalım 🍳" else "Set Up Your Kitchen 🍳"
    val setupSubtitle get() = if (isTr) "Hangi pişirme araçlarına sahipsin?" else "What cooking equipment do you have?"
    val setupServings get() = if (isTr) "Kaç kişilik yemek yapacaksın?" else "How many servings?"
    val setupTime get() = if (isTr) "Yemek genellikle kaçta hazır olsun?" else "When should meals be ready?"
    val setupStart get() = if (isTr) "Başla!" else "Let's Cook!"
    val hardwareProfile get() = if (isTr) "Donanım Profili" else "Hardware Profile"
    val notifications get() = if (isTr) "Bildirimler" else "Notifications"
    val language get() = if (isTr) "Dil" else "Language"
    val version get() = if (isTr) "Versiyon" else "Version"
    val notifSubtitle get() = if (isTr) "Adım başladığında bildir" else "Notify on each step"
    val app get() = if (isTr) "Uygulama" else "Application"
    val save get() = if (isTr) "Kaydet" else "Save"
    val cancel get() = if (isTr) "İptal" else "Cancel"
    val selectLanguage get() = if (isTr) "Dil Seçin" else "Select Language"
    val persons get() = if (isTr) "kişi" else "persons"
    val stoveType get() = if (isTr) "Ocak Tipi" else "Stove Type"
    val electric get() = if (isTr) "⚡ Elektrik" else "⚡ Electric"
    val gas get() = if (isTr) "🔥 Gaz" else "🔥 Gas"
    val dietary = if (isTr) "Diyet Tercihi" else "Dietary Preference"
    val theme = if (isTr) "Uygulama Teması" else "App Theme"
    val editSetup = if (isTr) "Düzenle" else "Edit Setup"
    val categoryBtn = if (isTr) "Kategoriler" else "Categories"
    val scanIngredients = if (isTr) "Kamerayla Tara" else "Scan Ingredients"
    val hasOven = if (isTr) "Fırın Kullanılabilir" else "Oven Available"
    val serving = if (isTr) "Porsiyon" else "Serving"
}

// ── Ekipman & Malzeme ──────────────────────────────────────────────────
data class CookingEquipment(val id: String, val icon: String, val labelTr: String, val labelEn: String, val resource: String) {
    val label get() = if (L.isTr) labelTr else labelEn
}

val ALL_EQUIPMENT = listOf(
    CookingEquipment("oven",      "🫕", "Fırın",        "Oven",          "oven"),
    CookingEquipment("elec",      "⚡", "Elektrik Ocak","Electric Stove","stovetop"),
    CookingEquipment("gas",       "🔥", "Gaz Ocak",     "Gas Stove",     "stovetop"),
    CookingEquipment("grill",     "🪵", "Mangal",        "BBQ Grill",     "grill"),
    CookingEquipment("camping",   "🏕️", "Piknik Tüpü",  "Camping Stove", "camping"),
    CookingEquipment("airfryer",  "💨", "Hava Fritözü",  "Air Fryer",    "airfryer"),
    CookingEquipment("microwave", "🌀", "Mikrodalga",    "Microwave",     "microwave"),
    CookingEquipment("pan",       "🍳", "Tava/Tencere",  "Pan/Pot",       "stovetop")
)

// ── UI States & Models ─────────────────────────────────────────────────
data class RecipeOption(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val sourceLabel: String? = null,
    val proposedIngredients: List<com.agentickitchen.shared.ai.dto.PlannedIngredientDto> = emptyList(),
    val shortages: List<String> = emptyList()
)
data class RecipeRequestSelection(val servings: Int, val targetTime: TargetTimeChoice)

sealed class PlanState {
    object Idle : PlanState()
    object Loading : PlanState()
    data class OptionsReady(val options: List<RecipeOption>) : PlanState()
    data class RecipeActive(
        val sessionId: String = "",
        val recipe: RecipeOption,
        val events: List<ScheduleEvent>,
        val servings: Int = 2,
        val resolvedReadyTimeIso: String = "",
        val cookingPlan: CookingPlanResponse? = null,
        val plannedUsage: List<PlannedPantryUsage> = emptyList(),
        val agentChatResponse: String? = null,
        val visionScanResponse: String? = null
    ) : PlanState()
    data class Error(val message: String, val canUseOffline: Boolean = false) : PlanState()
}

sealed interface ShoppingImportState {
    data object Idle : ShoppingImportState
    data object Loading : ShoppingImportState
    data class Review(
        val candidates: List<ShoppingCandidate>,
        val mode: ShoppingImportMode,
        val source: String,
        val conflicts: List<String> = emptyList()
    ) : ShoppingImportState
    data class Error(val message: String) : ShoppingImportState
}

data class InventoryRecipeRequest(
    val servings: Int,
    val strictStock: Boolean,
    val maxMissingStaples: Int,
    val prioritizedIngredients: List<String>
)

data class PendingConsumption(
    val sessionId: String,
    val usages: List<PendingRecipeUsageRecord>
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class DraftIngredientRemoved(
        val ingredient: String,
        val previousIndex: Int,
        val orderBeforeRemoval: List<String>,
        val message: String
    ) : UiEvent()
}

data class HardwareSettings(
    val stoveType: String = "electric", val stovePowerMax: Int = 9,
    val ovenAvailable: Boolean = true, val ovenHasFan: Boolean = true, val ovenHasGrill: Boolean = false,
    val powerLevel: Int = 7,
    val geminiApiKey: String = "",
    val hfApiKey: String = "",
    val aiProvider: String = "FREE"
)

object CookingProviderSelection {
    const val Gemini = "GEMINI"
    const val Free = "FREE"

    private val supportedIds = setOf(Gemini, Free)

    fun normalize(providerId: String): String = providerId.takeIf { it in supportedIds } ?: Free

    fun needsApiKey(settings: HardwareSettings): Boolean = when (normalize(settings.aiProvider)) {
        Gemini -> settings.geminiApiKey.isBlank()
        else -> false
    }

    fun provider(factory: AiProviderFactory, settings: HardwareSettings): KitchenAiProvider? =
        factory.provider(settings.copy(aiProvider = normalize(settings.aiProvider)))
}

enum class AiConnectionStatus {
    NOT_CONFIGURED,
    TESTING,
    CONNECTED,
    INVALID_KEY,
    QUOTA_UNAVAILABLE,
    NETWORK_FAILURE
}

internal fun aiConnectionStatusFor(result: AiResult<*>): AiConnectionStatus = when (result) {
    is AiResult.Success -> AiConnectionStatus.CONNECTED
    is AiResult.Failure -> when (result.type) {
        AiFailureType.MissingCredential -> AiConnectionStatus.NOT_CONFIGURED
        AiFailureType.Unauthorized -> AiConnectionStatus.INVALID_KEY
        AiFailureType.QuotaExceeded, AiFailureType.RateLimited -> AiConnectionStatus.QUOTA_UNAVAILABLE
        else -> AiConnectionStatus.NETWORK_FAILURE
    }
}
data class DietSettings(val dietType: String = "none", val allergies: Set<String> = emptySet())

internal fun activeRecipeState(
    sessionId: String,
    option: RecipeOption,
    events: List<ScheduleEvent>,
    servings: Int,
    readyTimeIso: String,
    plan: CookingPlanResponse,
    plannedUsage: List<PlannedPantryUsage> = emptyList()
) = PlanState.RecipeActive(
    sessionId = sessionId,
    recipe = option,
    events = events,
    servings = servings,
    resolvedReadyTimeIso = readyTimeIso,
    cookingPlan = plan,
    plannedUsage = plannedUsage
)

internal fun readerSafeAiError(error: Throwable?): String {
    if (error is AiRequestException) {
        if (error.failure.technicalMessage == "request_too_large") {
            return if (L.isTr) "Fotoğraf gönderilemeyecek kadar büyük. Daha küçük bir fotoğraf seç." else "The photo is too large to send. Choose a smaller photo."
        }
        return when (error.failure.type) {
            AiFailureType.MissingCredential ->
                if (L.isTr) "Gemini anahtarı eksik. Ayarlar bölümünden ekleyebilirsin." else "The Gemini key is missing. Add it in Settings."
            AiFailureType.Unauthorized ->
                if (L.isTr) "Gemini anahtarı geçerli değil. Ayarlar bölümünden kontrol et." else "The Gemini key is not valid. Check it in Settings."
            AiFailureType.QuotaExceeded, AiFailureType.RateLimited ->
                if (L.isTr) "Gemini kullanım sınırına ulaştı. Daha sonra tekrar dene veya çevrimdışı modu seç." else "Gemini has reached its usage limit. Try later or choose Offline mode."
            AiFailureType.NetworkUnavailable, AiFailureType.Timeout ->
                if (L.isTr) "Gemini'ye bağlanılamadı. Bağlantını kontrol et veya çevrimdışı modu seç." else "Could not reach Gemini. Check your connection or choose Offline mode."
            AiFailureType.SafetyBlocked ->
                if (L.isTr) "Gemini bu isteğe yanıt veremedi." else "Gemini could not answer this request."
            else ->
                if (L.isTr) "Gemini yanıtı kullanılamadı. Tekrar dene veya çevrimdışı modu seç." else "The Gemini response could not be used. Retry or choose Offline mode."
        }
    }
    if (error is ProviderFailure) {
        return when {
            error.statusCode == 429 || error.statusCode == 402 ->
                if (L.isTr) "Sağlayıcı şu anda yoğun veya kullanım sınırına ulaşıldı. Biraz sonra tekrar dene." else "The provider is busy or has reached its usage limit. Try again shortly."
            error.category == ProviderFailureCategory.TIMEOUT || error.category == ProviderFailureCategory.NETWORK ->
                if (L.isTr) "İnternet bağlantısı kurulamadı. Bağlantını kontrol edip tekrar dene." else "Could not connect. Check your internet connection and try again."
            error.category == ProviderFailureCategory.CONSTRAINT_CONFLICT ->
                if (L.isTr) "Seçili malzemeler diyet, alerji veya güvenli pişirme koşullarıyla uyuşmuyor." else "The selected ingredients conflict with the diet, allergy, or safe cooking setup."
            else ->
                if (L.isTr) "Şu anda yanıt alınamadı. Tekrar deneyebilirsin." else "No response was available just now. You can try again."
        }
    }
    val message = error?.message.orEmpty().lowercase()
    return when {
        "api_key_missing" in message || "credential" in message || "api key" in message -> if (L.isTr) "Seçili sağlayıcının anahtarı eksik. Ayarlar bölümünden ekleyebilirsin." else "The selected provider is missing its credential. Add it in Settings."
        "quota" in message || "rate" in message || "429" in message -> if (L.isTr) "Sağlayıcı şu anda yoğun veya kullanım sınırına ulaşıldı. Biraz sonra tekrar dene." else "The provider is busy or has reached its usage limit. Try again shortly."
        "timeout" in message || "network" in message || "connect" in message || "internet" in message -> if (L.isTr) "İnternet bağlantısı kurulamadı. Bağlantını kontrol edip tekrar dene." else "Could not connect. Check your internet connection and try again."
        else -> if (L.isTr) "Şu anda yanıt alınamadı. Tekrar deneyebilirsin." else "No response was available just now. You can try again."
    }
}

private class AiRequestException(val failure: AiResult.Failure) : Exception(failure.type.name)

private class PlanValidationException(val validationErrors: List<ValidationError>) :
    Exception(validationErrors.joinToString(",") { it.type.name })

internal fun readerSafePlanValidationError(errors: List<ValidationError>): String {
    val types = errors.map { it.type }.toSet()
    return when {
        ErrorType.DIET_CONFLICT in types || ErrorType.ALLERGEN_CONFLICT in types ->
            if (L.isTr) "Bu tarif diyet veya alerji tercihlerinle uyuşmuyor. Başka bir tarif seç." else "This recipe conflicts with your diet or allergy preferences. Choose another recipe."
        ErrorType.UNAVAILABLE_EQUIPMENT in types || ErrorType.UNKNOWN_RESOURCE in types || ErrorType.POWER_EXCEEDS_MAXIMUM in types ->
            if (L.isTr) "Bu tarif mutfak kurulumundaki araçlarla güvenle hazırlanamaz. Başka bir tarif seç." else "This recipe cannot be prepared safely with your kitchen setup. Choose another recipe."
        ErrorType.UNKNOWN_UNIT in types || ErrorType.MISSING_QUANTITY in types || ErrorType.SERVING_MISMATCH in types ->
            if (L.isTr) "Tarifteki miktarlar anlaşılamadı. Yeniden deneyebilirsin." else "Some recipe quantities could not be understood. Try again."
        else -> if (L.isTr) "Tarif adımları güvenli bir plana dönüştürülemedi. Yeniden deneyebilirsin." else "The recipe steps could not be turned into a safe plan. Try again."
    }
}

// ── ViewModel ─────────────────────────────────────────────────────────────
class AppViewModel(
    private val prefs: AppPreferences,
    private val historyRepo: RecipeHistoryRepository,
    private val inventoryRepository: PantryInventoryRepository,
    private val orchestrator: Orchestrator,
    private val pantryIntelAgent: PantryIntelAgent,
    private val providerFactory: AiProviderFactory,
    private val targetTimeResolver: TargetTimeResolver
) : ViewModel() {

    private val _setupDone = MutableStateFlow(prefs.setupDone())
    val setupDone: StateFlow<Boolean> = _setupDone.asStateFlow()

    private val _isEditingSetup = MutableStateFlow(false)
    val isEditingSetup: StateFlow<Boolean> = _isEditingSetup.asStateFlow()

    private val _selectedEquipment = MutableStateFlow<Set<String>>(loadEquipment())
    val selectedEquipment: StateFlow<Set<String>> = _selectedEquipment.asStateFlow()

    private val _chips = MutableStateFlow(loadIngredientDraft())
    val chips: StateFlow<List<String>> = _chips.asStateFlow()
    private var draftOrder = _chips.value

    private val _inventory = MutableStateFlow(inventoryRepository.getAll())
    val inventory: StateFlow<List<PantryStockItem>> = _inventory.asStateFlow()
    private val _inventoryAdjustments = MutableStateFlow(
        _inventory.value.associate { it.id to inventoryRepository.adjustments(it.id) }
    )
    val inventoryAdjustments = _inventoryAdjustments.asStateFlow()
    private val _shoppingImportState = MutableStateFlow<ShoppingImportState>(ShoppingImportState.Idle)
    val shoppingImportState: StateFlow<ShoppingImportState> = _shoppingImportState.asStateFlow()
    private val _allPendingConsumptions = MutableStateFlow(
        inventoryRepository.allPendingUsage()
            .groupBy(PendingRecipeUsageRecord::sessionId)
            .map { (sessionId, usages) -> PendingConsumption(sessionId, usages) }
    )
    val allPendingConsumptions: StateFlow<List<PendingConsumption>> = _allPendingConsumptions.asStateFlow()
    private val _pendingConsumption = MutableStateFlow(_allPendingConsumptions.value.firstOrNull())
    val pendingConsumption: StateFlow<PendingConsumption?> = _pendingConsumption.asStateFlow()
    private var inventoryRecipeRequest: InventoryRecipeRequest? = null

    private val _planState = MutableStateFlow<PlanState>(PlanState.Idle)
    val planState: StateFlow<PlanState> = _planState.asStateFlow()
    private val cookingController = CookingSessionController()
    private val _cookingState = MutableStateFlow(CookingSessionState())
    val cookingState: StateFlow<CookingSessionState> = _cookingState.asStateFlow()
    private var cookingTicker: Job? = null

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private fun emitUiEvent(message: String) {
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar(message)) }
    }

    private val _scannedIngredients = MutableStateFlow<List<String>?>(null)
    val scannedIngredients: StateFlow<List<String>?> = _scannedIngredients.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()
    fun clearAiError() { _aiError.value = null }
    private val _aiConnectionStatus = MutableStateFlow(AiConnectionStatus.NOT_CONFIGURED)
    val aiConnectionStatus: StateFlow<AiConnectionStatus> = _aiConnectionStatus.asStateFlow()
    private val recentCookingTurns = ArrayDeque<CookingChatTurn>()

    private val _hw = MutableStateFlow(loadHardwareSettings())
    val hardwareSettings: StateFlow<HardwareSettings> = _hw.asStateFlow()
    val dietSettings = MutableStateFlow(prefs.dietSettings())
    val theme = MutableStateFlow(prefs.theme())
    val language = MutableStateFlow(L.normalize(prefs.language()))
    private var lastOptions: List<RecipeOption> = emptyList()
    private val _pantryIntel = MutableStateFlow(
        pantryIntelAgent.analyze(
            ingredients = _chips.value,
            equipment = _selectedEquipment.value,
            dietType = dietSettings.value.dietType
        )
    )
    val pantryIntel: StateFlow<PantryIntelReport> = _pantryIntel.asStateFlow()

    private val _history = MutableStateFlow<List<RecipeHistory>>(emptyList())
    val history: StateFlow<List<RecipeHistory>> = _history.asStateFlow()

    init {
        L.applyLanguage(language.value)
        loadHistory()
        restoreActiveSession()
    }
    
    private fun loadHistory() {
        _history.value = historyRepo.getAllHistory()
    }

    private fun restoreActiveSession() {
        refreshPendingConsumptions()
        val activeSessions = inventoryRepository.getAllActiveSessions()
        val sessionRecord = activeSessions.firstOrNull() ?: return
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val plan = json.decodeFromString<CookingPlanResponse>(sessionRecord.cookingPlanJson)
            val events = json.decodeFromString<List<ScheduleEvent>>(sessionRecord.eventsJson)
            val plannedUsage = json.decodeFromString<List<PlannedPantryUsage>>(sessionRecord.plannedUsageJson)
            val completedStepIds = json.decodeFromString<Set<String>>(sessionRecord.completedStepIdsJson)
            val skippedStepIds = json.decodeFromString<Set<String>>(sessionRecord.skippedStepIdsJson)
            val chatTurns = json.decodeFromString<List<CookingChatTurn>>(sessionRecord.recentChatTurnsJson)

            recentCookingTurns.clear()
            chatTurns.forEach { recentCookingTurns.addLast(it) }

            val option = RecipeOption(
                id = sessionRecord.recipeOptionId,
                type = sessionRecord.recipeType,
                name = sessionRecord.recipeName,
                description = sessionRecord.description,
                sourceLabel = sessionRecord.sourceLabel,
                proposedIngredients = plan.ingredients
            )

            val restoredState = activeRecipeState(
                sessionId = sessionRecord.sessionId,
                option = option,
                events = events,
                servings = sessionRecord.servings,
                readyTimeIso = sessionRecord.resolvedReadyTimeIso,
                plan = plan,
                plannedUsage = plannedUsage
            )
            _planState.value = restoredState

            val restoredStatus = try {
                CookingSessionStatus.valueOf(sessionRecord.status)
            } catch (_: Exception) {
                CookingSessionStatus.READY
            }

            _cookingState.value = cookingController.restore(
                recipe = sessionRecord.recipeName,
                schedule = events,
                status = restoredStatus,
                startedAtMillis = sessionRecord.startedAtMillis,
                accumulatedElapsedSeconds = sessionRecord.accumulatedElapsedSeconds,
                lastRunningStartMillis = sessionRecord.lastRunningStartMillis,
                pausedAtMillis = sessionRecord.pausedAtMillis,
                completed = completedStepIds,
                skipped = skippedStepIds
            )

            if (_cookingState.value.status == CookingSessionStatus.RUNNING) {
                startCookingTicker()
            }
        } catch (e: Exception) {
            AppLogger.w("Recovery", "Failed to restore active session: ${e.message}")
        }
    }

    private fun persistActiveSession() {
        val activeState = _planState.value as? PlanState.RecipeActive ?: return
        val currentCooking = _cookingState.value
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val nowIso = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val record = ActiveCookingSessionRecord(
            sessionId = activeState.sessionId,
            recipeOptionId = activeState.recipe.id,
            recipeName = activeState.recipe.name,
            recipeType = activeState.recipe.type,
            description = activeState.recipe.description,
            sourceLabel = activeState.recipe.sourceLabel,
            servings = activeState.servings,
            resolvedReadyTimeIso = activeState.resolvedReadyTimeIso,
            cookingPlanJson = activeState.cookingPlan?.let { json.encodeToString(it) }.orEmpty(),
            eventsJson = json.encodeToString(activeState.events),
            plannedUsageJson = json.encodeToString(activeState.plannedUsage),
            status = currentCooking.status.name,
            startedAtMillis = System.currentTimeMillis() - (currentCooking.elapsedSeconds * 1000L),
            accumulatedElapsedSeconds = currentCooking.elapsedSeconds,
            lastRunningStartMillis = if (currentCooking.status == CookingSessionStatus.RUNNING) System.currentTimeMillis() else null,
            pausedAtMillis = if (currentCooking.status == CookingSessionStatus.PAUSED) System.currentTimeMillis() else null,
            completedStepIdsJson = json.encodeToString(currentCooking.completed),
            skippedStepIdsJson = json.encodeToString(currentCooking.skipped),
            recentChatTurnsJson = json.encodeToString(recentCookingTurns.toList()),
            updatedAtIso = nowIso
        )
        inventoryRepository.saveActiveSession(record)
    }

    internal fun refreshPendingConsumptions() {
        val groups = inventoryRepository.allPendingUsage()
            .groupBy(PendingRecipeUsageRecord::sessionId)
            .map { (sessionId, usages) -> PendingConsumption(sessionId, usages) }
        _allPendingConsumptions.value = groups
        _pendingConsumption.value = groups.firstOrNull()
    }

    fun completeSetup(equipment: Set<String>, hw: HardwareSettings) {
        AppLogger.i("Setup", "Kurulum tamamlandı")
        _selectedEquipment.value = equipment
        _hw.value = hw
        _setupDone.value = true
        _isEditingSetup.value = false
        prefs.saveSetup(true, equipment)
        saveHardwareSettings(hw)
        refreshPantryIntel()
    }

    fun startEditingSetup() { _isEditingSetup.value = true }
    fun cancelEditingSetup() { _isEditingSetup.value = false }
    fun addChip(name: String) {
        val canonical = canonicalIngredientName(name, L.isTr)
        if (canonical.isNotEmpty() && !_chips.value.any { it.equals(canonical, ignoreCase = true) }) {
            draftOrder = draftOrder.filterNot { it.equals(canonical, ignoreCase = true) } + canonical
            saveIngredientDraft(_chips.value + canonical)
        }
    }
    fun addMultipleChips(names: List<String>) {
        val updated = _chips.value.toMutableList()
        names.map { canonicalIngredientName(it, L.isTr) }.filter(String::isNotEmpty).forEach { ingredient ->
            if (updated.none { it.equals(ingredient, ignoreCase = true) }) updated += ingredient
        }
        if (updated != _chips.value) {
            updated.filterNot { candidate ->
                _chips.value.any { it.equals(candidate, ignoreCase = true) }
            }.forEach { addition ->
                draftOrder = draftOrder.filterNot { it.equals(addition, ignoreCase = true) } + addition
            }
            saveIngredientDraft(updated)
        }
    }
    fun removeChip(name: String) {
        val before = _chips.value
        val index = before.indexOf(name)
        if (index < 0) return
        saveIngredientDraft(before.toMutableList().apply { removeAt(index) })
        _uiEvent.tryEmit(
            UiEvent.DraftIngredientRemoved(
                ingredient = name,
                previousIndex = index,
                orderBeforeRemoval = draftOrder,
                message = if (L.isTr) "$name kaldırıldı" else "$name removed"
            )
        )
    }

    fun restoreRemovedChip(event: UiEvent.DraftIngredientRemoved) {
        if (_chips.value.any { it.equals(event.ingredient, ignoreCase = true) }) return
        val restored = _chips.value.toMutableList()
        val previous = event.orderBeforeRemoval.take(event.previousIndex).asReversed()
            .firstOrNull { candidate -> restored.any { it.equals(candidate, ignoreCase = true) } }
        val next = event.orderBeforeRemoval.drop(event.previousIndex + 1)
            .firstOrNull { candidate -> restored.any { it.equals(candidate, ignoreCase = true) } }
        val insertionIndex = when {
            previous != null -> restored.indexOfFirst { it.equals(previous, ignoreCase = true) } + 1
            next != null -> restored.indexOfFirst { it.equals(next, ignoreCase = true) }
            else -> event.previousIndex.coerceIn(0, restored.size)
        }
        restored.add(insertionIndex, event.ingredient)
        saveIngredientDraft(restored)
    }
    fun clearAll() {
        draftOrder = emptyList()
        saveIngredientDraft(emptyList())
        _planState.value = PlanState.Idle
        lastOptions = emptyList()
    }

    private fun loadIngredientDraft(): List<String> = buildList {
        prefs.ingredientDraft().map(String::trim).filter(String::isNotEmpty).forEach { ingredient ->
            if (none { it.equals(ingredient, ignoreCase = true) }) add(ingredient)
        }
    }

    private fun saveIngredientDraft(ingredients: List<String>) {
        _chips.value = ingredients
        prefs.saveIngredientDraft(ingredients)
        refreshPantryIntel()
    }

    fun saveInventoryItem(
        existing: PantryStockItem?,
        name: String,
        quantity: Double,
        unit: String,
        packageLabel: String?
    ) {
        val cleanName = name.trim()
        val normalized = runCatching { InventoryUnits.normalize(quantity, unit) }.getOrElse {
            emitUiEvent(if (L.isTr) "Geçerli bir stok miktarı gir." else "Enter a valid pantry amount.")
            return
        }
        if (cleanName.isEmpty()) {
            emitUiEvent(if (L.isTr) "Malzeme adı gerekli." else "Ingredient name is required.")
            return
        }
        val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val item = PantryStockItem(
            id = existing?.id ?: UUID.randomUUID().toString(),
            canonicalIngredientId = existing?.canonicalIngredientId,
            originalName = cleanName,
            displayNameTr = existing?.displayNameTr,
            displayNameEn = existing?.displayNameEn,
            quantity = normalized.quantity,
            unit = normalized.unit,
            unitDimension = normalized.dimension,
            packageLabel = packageLabel?.trim()?.takeIf(String::isNotEmpty),
            isEstimated = existing?.isEstimated ?: false,
            confidence = existing?.confidence,
            source = existing?.source ?: "manual",
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        inventoryRepository.upsert(
            item,
            InventoryAdjustmentRecord(
                id = UUID.randomUUID().toString(),
                itemId = item.id,
                amount = item.quantity,
                mode = if (existing == null) AdjustmentMode.DELTA else AdjustmentMode.REPLACE,
                reason = if (existing == null) AdjustmentReason.MANUAL_ADD else AdjustmentReason.RECOUNT,
                source = "manual",
                timestamp = now
            )
        )
        refreshInventory()
    }

    fun deleteInventoryItem(item: PantryStockItem) {
        val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        inventoryRepository.delete(
            item,
            InventoryAdjustmentRecord(
                id = UUID.randomUUID().toString(),
                itemId = item.id,
                amount = item.quantity,
                mode = AdjustmentMode.REPLACE,
                reason = AdjustmentReason.DELETION,
                source = "manual",
                timestamp = now
            )
        )
        refreshInventory()
    }

    internal fun refreshInventory() {
        _inventory.value = inventoryRepository.getAll()
        _inventoryAdjustments.value = _inventory.value.associate { item ->
            item.id to inventoryRepository.adjustments(item.id)
        }
    }

    private fun reservedQuantities(): Map<String, Double> =
        inventoryRepository.allPendingUsage()
            .filter { it.status == "reserved" }
            .groupBy(PendingRecipeUsageRecord::itemId)
            .mapValues { (_, usages) ->
                usages.sumOf { InventoryUnits.normalize(it.plannedQuantity, it.unit).quantity }
            }

    fun importShoppingText(text: String, mode: ShoppingImportMode) {
        if (text.isBlank()) return
        parseShoppingTextLocally(text, L.isTr)?.let { candidates ->
            _shoppingImportState.value = ShoppingImportState.Review(candidates, mode, "local_text")
            return
        }
        viewModelScope.launch {
            _shoppingImportState.value = ShoppingImportState.Loading
            try {
                val response = executeAiWithProvider {
                    it.parseShoppingText(ShoppingTextRequest(text.trim(), language.value)).requireValue()
                }
                _shoppingImportState.value = ShoppingImportState.Review(response.items, mode, "text")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _shoppingImportState.value = ShoppingImportState.Error(readerSafeCurrentProviderError(error))
            }
        }
    }

    fun importShoppingPhoto(image: Bitmap, mode: ShoppingImportMode) {
        viewModelScope.launch {
            _shoppingImportState.value = ShoppingImportState.Loading
            try {
                val response = executeAiWithProvider {
                    it.scanShoppingPhoto(
                        ShoppingPhotoRequest(encodeKitchenImage(image), language.value)
                    ).requireValue()
                }
                _shoppingImportState.value = ShoppingImportState.Review(response.items, mode, "photo")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _shoppingImportState.value = ShoppingImportState.Error(readerSafeCurrentProviderError(error))
            }
        }
    }

    fun confirmShoppingImport(candidates: List<ShoppingCandidate>, mode: ShoppingImportMode): Boolean {
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val plan = InventoryWorkflow.planImport(
            existing = _inventory.value,
            candidates = candidates,
            mode = mode,
            timestamp = timestamp,
            idFactory = { UUID.randomUUID().toString() }
        )
        if (plan.conflicts.isNotEmpty()) {
            _shoppingImportState.value = ShoppingImportState.Review(
                candidates = candidates,
                mode = mode,
                source = "review",
                conflicts = plan.conflicts
            )
            return false
        }
        inventoryRepository.applyMutations(plan.mutations)
        refreshInventory()
        _shoppingImportState.value = ShoppingImportState.Idle
        emitUiEvent(if (L.isTr) "Mutfak stoğu güncellendi." else "Kitchen inventory updated.")
        return true
    }

    fun clearShoppingImport() {
        _shoppingImportState.value = ShoppingImportState.Idle
    }

    fun reuseHistoryIngredients(names: List<String>) {
        val localized = names.map { canonicalIngredientName(it, L.isTr) }
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
        if (localized.isNotEmpty()) {
            draftOrder = localized
            saveIngredientDraft(localized)
        }
    }

    fun startSession() {
        inventoryRecipeRequest = null
        requestRecipeOptions(_chips.value, null)
    }

    fun startInventorySession(request: InventoryRecipeRequest) {
        if (_inventory.value.isEmpty()) {
            _planState.value = PlanState.Error(if (L.isTr) "Önce mutfak stoğuna ürün ekle." else "Add items to your kitchen inventory first.")
            return
        }
        inventoryRecipeRequest = request
        requestRecipeOptions(_inventory.value.map(PantryStockItem::originalName), request)
    }

    private fun requestRecipeOptions(
        ingredients: List<String>,
        inventoryRequest: InventoryRecipeRequest?
    ) {
        if (ingredients.isEmpty()) { _planState.value = PlanState.Error(L.noIngredientError); return }
        AppLogger.i("Session", "Recipe option request started")
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val result = provider.generateRecipeOptions(
                        RecipeOptionsRequest(
                            ingredients = ingredients,
                            equipment = _selectedEquipment.value,
                            dietType = dietSettings.value.dietType,
                            allergies = dietSettings.value.allergies,
                            language = language.value,
                            inventoryLines = if (inventoryRequest == null) emptyList() else _inventory.value.map {
                                "${it.quantity} ${it.unit} ${it.originalName}"
                            },
                            strictStock = inventoryRequest?.strictStock == true,
                            maxMissingStaples = inventoryRequest?.maxMissingStaples ?: 0,
                            prioritizedIngredients = inventoryRequest?.prioritizedIngredients.orEmpty(),
                            servings = inventoryRequest?.servings ?: 2
                        )
                    )
                    val response = result.requireValue()
                    val shortagesByOption = mutableMapOf<String, List<String>>()
                    if (inventoryRequest != null) {
                        val reserved = reservedQuantities()
                        val invalid = response.options.any { option ->
                            val usage = InventoryWorkflow.planUsage(
                                CookingPlanResponse(
                                    recipeName = option.name,
                                    servings = inventoryRequest.servings,
                                    ingredients = option.proposedIngredients,
                                    steps = emptyList(),
                                    safetyNotes = emptyList()
                                ),
                                _inventory.value,
                                reserved
                            )
                            shortagesByOption[option.id] = usage.shortages
                            option.proposedIngredients.isEmpty() ||
                                (inventoryRequest.strictStock && usage.shortages.isNotEmpty()) ||
                                (!inventoryRequest.strictStock && usage.shortages.size > inventoryRequest.maxMissingStaples)
                        }
                        if (invalid) throw AiRequestException(
                            AiResult.Failure(
                                AiFailureType.InvalidPlan,
                                true,
                                AiFailureType.InvalidPlan.userMessageRes
                            )
                        )
                    }
                    val sourceLabel = (result as? AiResult.Success)?.provider
                        ?.takeIf { it == com.agentickitchen.shared.ai.AiProviderId.FREE }
                        ?.label
                    lastOptions = response.options.map {
                        RecipeOption(
                            it.id,
                            it.difficulty,
                            it.name,
                            it.summary,
                            sourceLabel,
                            it.proposedIngredients,
                            shortagesByOption[it.id].orEmpty()
                        )
                    }
                    _planState.value = PlanState.OptionsReady(lastOptions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                logAiFailure("Options", e)
                val errorMsg = readerSafeCurrentProviderError(e)
                emitUiEvent(errorMsg)
                _planState.value = PlanState.Error(
                    errorMsg,
                    canUseOffline = CookingProviderSelection.normalize(_hw.value.aiProvider) == CookingProviderSelection.Gemini
                )
            }
        }
    }

    fun backToOptions() {
        val currentState = _planState.value
        if (currentState is PlanState.RecipeActive) {
            if (lastOptions.isNotEmpty()) {
                _planState.value = PlanState.OptionsReady(lastOptions)
            } else {
                startSession()
            }
        }
    }

    fun startCooking() {
        val active = _planState.value as? PlanState.RecipeActive ?: run {
            _cookingState.value = CookingSessionState(
                status = CookingSessionStatus.ERROR,
                error = if (L.isTr) "Önce bir tarif seç." else "Choose a recipe first."
            )
            return
        }
        if (_cookingState.value.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            _cookingState.value = _cookingState.value.copy(
                error = if (L.isTr) "Pişirme zaten devam ediyor." else "Cooking is already running."
            )
            return
        }
        val existingReservingSessions = inventoryRepository.allPendingUsage()
            .filter { it.status == "reserved" && it.sessionId != active.sessionId }
        if (existingReservingSessions.isNotEmpty()) {
            _cookingState.value = CookingSessionState(
                recipeName = active.recipe.name,
                status = CookingSessionStatus.ERROR,
                error = if (L.isTr) "Başka bir aktif pişirme seansı tamamlanmayı bekliyor." else "Another active cooking session is pending completion."
            )
            return
        }
        if (active.plannedUsage.isNotEmpty()) {
            val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val reservations = active.plannedUsage.map {
                PendingRecipeUsageRecord(
                    sessionId = active.sessionId,
                    itemId = it.itemId,
                    plannedQuantity = it.plannedQuantity,
                    unit = it.unit,
                    status = "reserved",
                    timestamp = timestamp
                )
            }
            if (!inventoryRepository.reserve(reservations)) {
                _cookingState.value = CookingSessionState(
                    recipeName = active.recipe.name,
                    status = CookingSessionStatus.ERROR,
                    error = if (L.isTr) {
                        "Bu tarif için yeterli kullanılabilir stok kalmadı."
                    } else {
                        "There is no longer enough available stock for this recipe."
                    }
                )
                return
            }
        }
        _cookingState.value = cookingController.start(active.recipe.name, active.events)
        persistActiveSession()
        startCookingTicker()
    }

    fun pauseCooking() {
        _cookingState.value = cookingController.pause()
        cookingTicker?.cancel()
        persistActiveSession()
    }

    fun resumeCooking() {
        _cookingState.value = cookingController.resume()
        persistActiveSession()
        startCookingTicker()
    }

    fun completeCookingStep(id: String) {
        _cookingState.value = cookingController.complete(id)
        persistActiveSession()
        stopTickerIfFinished()
    }

    fun skipCookingStep(id: String) {
        _cookingState.value = cookingController.skip(id)
        persistActiveSession()
        stopTickerIfFinished()
    }

    fun endCooking() {
        _cookingState.value = cookingController.end()
        cookingTicker?.cancel()
        persistActiveSession()
        exposePendingConsumption()
    }

    fun consumePlannedInventory(sessionId: String? = null) {
        val pending = (if (sessionId != null) _allPendingConsumptions.value.firstOrNull { it.sessionId == sessionId } else _pendingConsumption.value) ?: return
        consumeInventory(pending.sessionId, pending.usages.associate { it.itemId to it.plannedQuantity })
    }

    fun consumeActualInventory(actualQuantities: Map<String, Double>, sessionId: String? = null) {
        val pending = (if (sessionId != null) _allPendingConsumptions.value.firstOrNull { it.sessionId == sessionId } else _pendingConsumption.value) ?: return
        consumeInventory(pending.sessionId, actualQuantities)
    }

    fun cancelInventoryConsumption(sessionId: String? = null) {
        val pendingSessionId = sessionId ?: _pendingConsumption.value?.sessionId ?: return
        inventoryRepository.releaseReservation(pendingSessionId)
        inventoryRepository.deleteActiveSession(pendingSessionId)
        val active = _planState.value as? PlanState.RecipeActive
        if (active?.sessionId == pendingSessionId) {
            _planState.value = PlanState.Idle
            _cookingState.value = CookingSessionState()
        }
        refreshInventory()
        refreshPendingConsumptions()
    }

    private fun consumeInventory(sessionId: String, actualQuantities: Map<String, Double>) {
        if (actualQuantities.values.any { !it.isFinite() || it <= 0.0 }) {
            emitUiEvent(if (L.isTr) "Kullanılan miktarlar sıfırdan büyük olmalı." else "Used amounts must be greater than zero.")
            return
        }
        if (!inventoryRepository.consume(sessionId, actualQuantities)) {
            emitUiEvent(if (L.isTr) "Stok miktarları uygulanamadı." else "The pantry amounts could not be applied.")
            return
        }
        // On successful consumption: refresh inventory, reload canonical pending state from repository,
        // remove the consumed session, and expose the next pending session if one exists.
        refreshInventory()
        refreshPendingConsumptions()

        // Clear active recipe/cooking state only if the consumed session was the currently active session
        val active = _planState.value as? PlanState.RecipeActive
        if (active?.sessionId == sessionId) {
            _planState.value = PlanState.Idle
            _cookingState.value = CookingSessionState()
        }
    }

    private fun startCookingTicker() {
        cookingTicker?.cancel()
        if (_cookingState.value.status != CookingSessionStatus.RUNNING) return
        cookingTicker = viewModelScope.launch {
            while (true) {
                _cookingState.value = cookingController.current()
                if (_cookingState.value.status != CookingSessionStatus.RUNNING) {
                    exposePendingConsumption()
                    break
                }
                delay(500)
            }
        }
    }

    private fun stopTickerIfFinished() {
        if (_cookingState.value.status != CookingSessionStatus.RUNNING) {
            cookingTicker?.cancel()
            exposePendingConsumption()
        }
    }

    private fun exposePendingConsumption() {
        refreshPendingConsumptions()
    }

    fun refreshSession() {
        inventoryRecipeRequest?.let(::startInventorySession) ?: startSession()
    }

    fun selectRecipeOption(option: RecipeOption, selection: RecipeRequestSelection) {
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val hw = _hw.value
                    val stoveType = selectedStoveType()
                    val selectedIngredients = if (inventoryRecipeRequest == null) {
                        _chips.value
                    } else {
                        _inventory.value.map(PantryStockItem::originalName)
                    }
                    val plan = normalizeCookingPlan(provider.generateCookingPlan(
                        CookingPlanRequest(
                            recipeName = option.name,
                            ingredients = selectedIngredients,
                            equipment = _selectedEquipment.value,
                            servings = selection.servings,
                            stoveType = stoveType,
                            stoveMaxLevel = hw.stovePowerMax,
                            ovenAvailable = hw.ovenAvailable,
                            ovenHasFan = hw.ovenHasFan,
                            airfryerAvailable = "airfryer" in _selectedEquipment.value,
                            dietType = dietSettings.value.dietType,
                            allergies = dietSettings.value.allergies,
                            language = language.value,
                            inventoryLines = if (inventoryRecipeRequest == null) emptyList() else _inventory.value.map {
                                "${it.quantity} ${it.unit} ${it.originalName}"
                            }
                        )
                    ).requireValue())
                    val validation = CookingPlanValidator(_selectedEquipment.value, hw.stovePowerMax, stoveType, hw.ovenAvailable, _selectedEquipment.value.contains("airfryer"), dietSettings.value.dietType, dietSettings.value.allergies, selection.servings).validate(plan)
                    if (!validation.valid) {
                        AppLogger.w("PlanValidation", validation.errors.joinToString("_") { it.type.name })
                        throw PlanValidationException(validation.errors)
                    }
                    val target = targetTimeResolver.resolve(selection.targetTime).getOrElse { throw it }
                    val readyTimeIso = target.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    val sessionId = UUID.randomUUID().toString()
                    val usagePlan = if (inventoryRecipeRequest == null) {
                        com.agentickitchen.shared.inventory.InventoryUsagePlan(emptyList(), emptyList())
                    } else {
                        InventoryWorkflow.planUsage(plan, _inventory.value, reservedQuantities())
                    }
                    val allowedMissing = inventoryRecipeRequest?.maxMissingStaples ?: 0
                    if (
                        inventoryRecipeRequest?.strictStock == true && usagePlan.shortages.isNotEmpty() ||
                        inventoryRecipeRequest?.strictStock == false && usagePlan.shortages.size > allowedMissing
                    ) {
                        throw ProviderFailure("INVENTORY", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                    }
                    val session = RecipeSession(sessionId, readyTimeIso, plan.ingredients.map { IngredientAmount(slugify(it.name), quantityToGrams(it.quantity, it.unit)) }, "kitchen", plan.steps.map { RecipeStep(it.id, it.type, it.resource, it.targetTemperatureC, it.durationSeconds, it.instruction, it.dependsOn) })
                    val result = orchestrator.startSession(session)
                    historyRepo.insertRecipe(session.sessionId, option.name, plan.ingredients.joinToString { "${it.quantity} ${it.unit} ${it.name}" }, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "started")
                    loadHistory()
                    _planState.value = activeRecipeState(
                        sessionId,
                        option,
                        result.events,
                        selection.servings,
                        readyTimeIso,
                        plan,
                        usagePlan.usages
                    )
                    persistActiveSession()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                val message = if (e is PlanValidationException) readerSafePlanValidationError(e.validationErrors) else readerSafeCurrentProviderError(e)
                emitUiEvent(message)
                _planState.value = PlanState.Error(
                    message,
                    canUseOffline = CookingProviderSelection.normalize(_hw.value.aiProvider) == CookingProviderSelection.Gemini
                )
            }
        }
    }

    private fun getActiveProvider(): KitchenAiProvider? {
        return CookingProviderSelection.provider(providerFactory, _hw.value)
    }

    private fun selectedStoveType(): String = when {
        "gas" in _selectedEquipment.value -> "gas"
        "elec" in _selectedEquipment.value -> "electric"
        "camping" in _selectedEquipment.value -> "gas"
        else -> "none"
    }

    private suspend fun <T> executeAiWithProvider(action: suspend (KitchenAiProvider) -> T): T {
        val provider = getActiveProvider() ?: throw AiRequestException(
            AiResult.Failure(
                AiFailureType.MissingCredential,
                false,
                AiFailureType.MissingCredential.userMessageRes
            )
        )
        return action(provider)
    }

    private fun readerSafeCurrentProviderError(error: Throwable): String =
        if (CookingProviderSelection.normalize(_hw.value.aiProvider) == CookingProviderSelection.Free) {
            if (L.isTr) {
                "Çevrimdışı mod bu isteği tamamlayamadı. Gemini kullanmak için Ayarlar'dan Gemini'yi seç."
            } else {
                "Offline mode could not complete this request. Select Gemini in Settings to use Gemini."
            }
        } else {
            readerSafeAiError(error)
        }

    private fun <T> AiResult<T>.requireValue(): T = when (this) {
        is AiResult.Success -> value
        is AiResult.Failure -> throw AiRequestException(this)
    }

    private fun logAiFailure(feature: String, error: Throwable) {
        val message = if (error is AiRequestException) {
            "provider=${CookingProviderSelection.normalize(_hw.value.aiProvider)} category=${error.failure.type}"
        } else if (error is ProviderFailure) {
            "provider=${error.providerId} status=${error.statusCode ?: "none"} category=${error.category} responseLength=${error.responseLength}"
        } else {
            "category=${error::class.simpleName ?: "UNKNOWN"}"
        }
        AppLogger.w("AI-$feature", message)
    }

    fun useOfflineMode() {
        saveHardwareSettings(_hw.value.copy(aiProvider = CookingProviderSelection.Free))
        emitUiEvent(if (L.isTr) "Çevrimdışı mod seçildi." else "Offline mode selected.")
        if (_chips.value.isNotEmpty()) startSession()
    }

    fun askIngredientAgent(question: String) {
        val currentState = _planState.value
        if (currentState is PlanState.RecipeActive) {
            _planState.value = currentState.copy(agentChatResponse = if (L.isTr) "Mutfak asistanı düşünüyor..." else "The kitchen assistant is thinking...")
            viewModelScope.launch {
                try {
                    executeAiWithProvider { provider ->
                        val activeOperation = _cookingState.value.active.firstOrNull()
                        val currentStep = activeOperation?.event?.instruction
                            ?: currentState.events.firstOrNull()?.instruction
                            ?: if (L.isTr) "Henüz etkin adım yok." else "No active step yet."
                        val plan = currentState.cookingPlan ?: throw IllegalStateException("Validated plan missing")
                        val result = provider.askCookingAssistant(
                            CookingChatRequest(
                                recipeName = currentState.recipe.name,
                                plan = plan,
                                currentStep = currentStep,
                                elapsedSeconds = _cookingState.value.elapsedSeconds,
                                resource = activeOperation?.event?.resource,
                                recentTurns = recentCookingTurns.toList(),
                                question = question,
                                language = language.value
                            )
                        )
                        val response = result.requireValue()
                        val visibleAnswer = if (
                            (result as? AiResult.Success)?.provider == com.agentickitchen.shared.ai.AiProviderId.FREE
                        ) {
                            "${if (L.isTr) "Çevrimdışı" else "Offline"} · ${response.answer}"
                        } else {
                            response.answer
                        }
                        rememberCookingTurn("user", question)
                        rememberCookingTurn("assistant", response.answer)
                        _planState.value = currentState.copy(agentChatResponse = visibleAnswer)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (e: Exception) {
                    _planState.value = currentState.copy(agentChatResponse = readerSafeCurrentProviderError(e))
                }
            }
        }
    }

    private fun rememberCookingTurn(role: String, text: String) {
        recentCookingTurns.addLast(CookingChatTurn(role, text))
        while (recentCookingTurns.size > 6) recentCookingTurns.removeFirst()
    }

    fun clearScannedIngredients() { _scannedIngredients.value = null }

    fun scanIngredients(image: Bitmap) {
        viewModelScope.launch {
            _scannedIngredients.value = null
            try {
                val provider = getActiveProvider() ?: throw AiRequestException(
                    AiResult.Failure(
                        AiFailureType.MissingCredential,
                        false,
                        AiFailureType.MissingCredential.userMessageRes
                    )
                )
                val result = provider.scanShoppingPhoto(
                    ShoppingPhotoRequest(
                        image = encodeKitchenImage(image),
                        language = language.value
                    )
                ).requireValue()
                val items = result.items.map { it.displayName.trim() }.filter(String::isNotBlank)
                if (items.isEmpty()) throw AiRequestException(
                    AiResult.Failure(
                        AiFailureType.InvalidResponse,
                        true,
                        AiFailureType.InvalidResponse.userMessageRes
                    )
                )
                _scannedIngredients.value = items
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                logAiFailure("ScanIngr", e)
                if (e is AiRequestException && e.failure.type == AiFailureType.MissingCredential) {
                    _aiError.value = "API_KEY_MISSING"
                } else if (e is AiRequestException && e.failure.type in setOf(AiFailureType.QuotaExceeded, AiFailureType.RateLimited)) {
                    _aiError.value = "QUOTA_EXCEEDED"
                } else {
                    _aiError.value = "SCAN_FAILED"
                }
                _scannedIngredients.value = listOf("__ERROR__")
            }
        }
    }

    fun checkVisionAgent(image: Bitmap) {
        val currentState = _planState.value as? PlanState.RecipeActive ?: return

        viewModelScope.launch {
            try {
                val activeOperation = _cookingState.value.active.firstOrNull()
                val response = executeAiWithProvider { provider ->
                    provider.inspectCookingPhoto(
                        CookingPhotoRequest(
                            recipeName = currentState.recipe.name,
                            plan = currentState.cookingPlan ?: throw IllegalStateException("Validated plan missing"),
                            currentStep = activeOperation?.event?.instruction
                                ?: currentState.events.firstOrNull()?.instruction.orEmpty(),
                            elapsedSeconds = _cookingState.value.elapsedSeconds,
                            resource = activeOperation?.event?.resource,
                            recentTurns = recentCookingTurns.toList(),
                            question = if (L.isTr) "Yemeğin durumunu ve güvenli sonraki adımı değerlendir." else "Assess the food and the safe next action.",
                            image = encodeKitchenImage(image),
                            language = language.value
                        )
                    ).requireValue()
                }
                val text = listOfNotNull(
                    response.visibleObservation,
                    response.immediateAction,
                    response.heatAdjustment,
                    response.safetyWarning,
                    response.uncertainty
                ).filter(String::isNotBlank).joinToString("\n\n")
                _planState.value = currentState.copy(visionScanResponse = text)
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                _planState.value = currentState.copy(visionScanResponse = readerSafeCurrentProviderError(e))
            }
        }
    }

    private fun encodeKitchenImage(bitmap: Bitmap): KitchenImage {
        val maxDimension = 1_800
        val scale = (maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        return try {
            val output = java.io.ByteArrayOutputStream()
            check(scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)) { "Image encoding failed" }
            KitchenImage(output.toByteArray(), "image/jpeg")
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    fun clearAgentChat() {
        val currentState = _planState.value as? PlanState.RecipeActive ?: return
        _planState.value = currentState.copy(agentChatResponse = null)
    }

    fun clearVisionResponse() {
        val currentState = _planState.value as? PlanState.RecipeActive ?: return
        _planState.value = currentState.copy(visionScanResponse = null)
    }

    fun saveHardwareSettings(hw: HardwareSettings) {
        val normalized = hw.copy(aiProvider = CookingProviderSelection.normalize(hw.aiProvider))
        _hw.value = normalized
        prefs.saveHardwareSettings(normalized)
        _aiConnectionStatus.value = if (
            normalized.aiProvider == CookingProviderSelection.Gemini &&
            normalized.geminiApiKey.isBlank()
        ) {
            AiConnectionStatus.NOT_CONFIGURED
        } else {
            _aiConnectionStatus.value
        }
        refreshPantryIntel()
    }

    fun saveApiKey(key: String) {
        saveHardwareSettings(_hw.value.copy(geminiApiKey = key))
    }

    fun testAiConnection(settings: HardwareSettings = _hw.value) {
        val normalized = settings.copy(aiProvider = CookingProviderSelection.normalize(settings.aiProvider))
        if (normalized.aiProvider == CookingProviderSelection.Gemini && normalized.geminiApiKey.isBlank()) {
            _aiConnectionStatus.value = AiConnectionStatus.NOT_CONFIGURED
            return
        }
        _aiConnectionStatus.value = AiConnectionStatus.TESTING
        viewModelScope.launch {
            val result = providerFactory.provider(normalized)?.testConnection()
                ?: AiResult.Failure(
                    AiFailureType.MissingCredential,
                    false,
                    AiFailureType.MissingCredential.userMessageRes
                )
            _aiConnectionStatus.value = aiConnectionStatusFor(result)
        }
    }

    fun saveDietSettings(diet: DietSettings) {
        dietSettings.value = diet
        prefs.saveDietSettings(diet)
        refreshPantryIntel()
    }

    fun setTheme(t: String) { 
        theme.value = t
        prefs.saveTheme(t)
    }
    fun setLanguage(lang: String) {
        val normalizedLanguage = L.normalize(lang)
        language.value = normalizedLanguage
        L.applyLanguage(normalizedLanguage)
        prefs.saveLanguage(normalizedLanguage)
    }

    private fun loadEquipment() = prefs.equipment()
    private fun refreshPantryIntel() {
        _pantryIntel.value = pantryIntelAgent.analyze(
            ingredients = _chips.value,
            equipment = _selectedEquipment.value,
            dietType = dietSettings.value.dietType
        )
    }

    private fun loadHardwareSettings(): HardwareSettings {
        val stored = prefs.hardwareSettings()
        return stored.copy(aiProvider = CookingProviderSelection.normalize(stored.aiProvider))
    }

    private fun quantityToGrams(quantity: Double, unit: String): Int = when (unit.lowercase()) {
        "kg" -> (quantity * 1000).toInt()
        "g" -> quantity.toInt()
        "ml" -> quantity.toInt()
        "l" -> (quantity * 1000).toInt()
        else -> quantity.toInt().coerceAtLeast(1)
    }


    private fun slugify(name: String) = name.lowercase().replace("ğ", "g").replace("ü", "u").replace("ş", "s").replace("ı", "i").replace("ö", "o").replace("ç", "c").replace(Regex("[^a-z0-9]"), "_")
}
