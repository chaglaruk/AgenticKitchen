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
import com.agentickitchen.shared.inventory.InventoryUnits
import com.agentickitchen.shared.inventory.PantryInventoryRepository
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.android.ai.ProviderFailure
import com.agentickitchen.android.ai.ProviderFailureCategory
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.CookingChatRequest
import com.agentickitchen.shared.ai.CookingChatResponse
import com.agentickitchen.shared.ai.CookingChatTurn
import com.agentickitchen.shared.ai.CookingPhotoRequest
import com.agentickitchen.shared.ai.KitchenAiProvider
import com.agentickitchen.shared.ai.KitchenImage
import com.agentickitchen.shared.ai.RecipeOptionsRequest
import com.agentickitchen.shared.ai.ShoppingPhotoRequest
import com.agentickitchen.shared.ai.CookingPlanRequest
import com.agentickitchen.shared.ai.dto.CookingPlanResponse
import com.agentickitchen.shared.scheduler.TargetTimeChoice
import com.agentickitchen.shared.validator.CookingPlanValidator
import com.agentickitchen.shared.cooking.CookingSessionController
import com.agentickitchen.shared.cooking.CookingSessionState
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
    val appTagline get() = if (isTr) "Mutfağının Yapay Zekası" else "The AI of Your Kitchen"
    val addIngredient get() = if (isTr) "Malzeme ekle..." else "Type an ingredient..."
    val clearAll get() = if (isTr) "Temizle" else "Clear"
    val generatePlan get() = if (isTr) "Tarif Alternatifleri Üret" else "Generate Options"
    val thinking get() = if (isTr) "Yapay Zeka Planlıyor..." else "AI is calculating..."
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
    val sourceLabel: String? = null
)
data class RecipeRequestSelection(val servings: Int, val targetTime: TargetTimeChoice)

sealed class PlanState {
    object Idle : PlanState()
    object Loading : PlanState()
    data class OptionsReady(val options: List<RecipeOption>) : PlanState()
    data class RecipeActive(
        val recipe: RecipeOption,
        val events: List<ScheduleEvent>,
        val servings: Int = 2,
        val resolvedReadyTimeIso: String = "",
        val cookingPlan: CookingPlanResponse? = null,
        val agentChatResponse: String? = null,
        val visionScanResponse: String? = null
    ) : PlanState()
    data class Error(val message: String, val canUseOffline: Boolean = false) : PlanState()
}

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
    option: RecipeOption,
    events: List<ScheduleEvent>,
    servings: Int,
    readyTimeIso: String,
    plan: CookingPlanResponse
) = PlanState.RecipeActive(
    recipe = option,
    events = events,
    servings = servings,
    resolvedReadyTimeIso = readyTimeIso,
    cookingPlan = plan
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
    }
    
    private fun loadHistory() {
        _history.value = historyRepo.getAllHistory()
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
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !_chips.value.any { it.equals(trimmed, ignoreCase = true) }) {
            draftOrder = draftOrder.filterNot { it.equals(trimmed, ignoreCase = true) } + trimmed
            saveIngredientDraft(_chips.value + trimmed)
        }
    }
    fun addMultipleChips(names: List<String>) {
        val updated = _chips.value.toMutableList()
        names.map(String::trim).filter(String::isNotEmpty).forEach { ingredient ->
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

    private fun refreshInventory() {
        _inventory.value = inventoryRepository.getAll()
        _inventoryAdjustments.value = _inventory.value.associate { item ->
            item.id to inventoryRepository.adjustments(item.id)
        }
    }

    fun startSession(isRefresh: Boolean = false) {
        if (_chips.value.isEmpty()) { _planState.value = PlanState.Error(L.noIngredientError); return }
        AppLogger.i("Session", "Recipe option request started")
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val result = provider.generateRecipeOptions(
                        RecipeOptionsRequest(
                            ingredients = _chips.value,
                            equipment = _selectedEquipment.value,
                            dietType = dietSettings.value.dietType,
                            allergies = dietSettings.value.allergies,
                            language = language.value
                        )
                    )
                    val response = result.requireValue()
                    val sourceLabel = (result as? AiResult.Success)?.provider
                        ?.takeIf { it == com.agentickitchen.shared.ai.AiProviderId.FREE }
                        ?.label
                    lastOptions = response.options.map {
                        RecipeOption(it.id, it.difficulty, it.name, it.summary, sourceLabel)
                    }
                    _planState.value = PlanState.OptionsReady(lastOptions)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                logAiFailure("Options", e)
                val errorMsg = readerSafeAiError(e)
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

    fun startCooking() { val active = _planState.value as? PlanState.RecipeActive ?: run { _cookingState.value = CookingSessionState(error = "Choose a recipe first"); return }; _cookingState.value = cookingController.start(active.recipe.name, active.events); startCookingTicker() }
    fun pauseCooking() { _cookingState.value = cookingController.pause(); cookingTicker?.cancel() }
    fun resumeCooking() { _cookingState.value = cookingController.resume(); startCookingTicker() }
    fun completeCookingStep(id: String) { _cookingState.value = cookingController.complete(id); stopTickerIfFinished() }
    fun skipCookingStep(id: String) { _cookingState.value = cookingController.skip(id); stopTickerIfFinished() }
    fun endCooking() { _cookingState.value = cookingController.end(); cookingTicker?.cancel() }
    private fun startCookingTicker() { cookingTicker?.cancel(); if (_cookingState.value.status != com.agentickitchen.shared.cooking.CookingSessionStatus.RUNNING) return; cookingTicker = viewModelScope.launch { while (true) { _cookingState.value = cookingController.current(); if (_cookingState.value.status != com.agentickitchen.shared.cooking.CookingSessionStatus.RUNNING) break; delay(500) } } }
    private fun stopTickerIfFinished() { if (_cookingState.value.status != com.agentickitchen.shared.cooking.CookingSessionStatus.RUNNING) cookingTicker?.cancel() }

    fun refreshSession() {
        startSession(isRefresh = true)
    }

    fun selectRecipeOption(option: RecipeOption, selection: RecipeRequestSelection) {
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val hw = _hw.value
                    val stoveType = selectedStoveType()
                    val plan = provider.generateCookingPlan(
                        CookingPlanRequest(
                            recipeName = option.name,
                            ingredients = _chips.value,
                            equipment = _selectedEquipment.value,
                            servings = selection.servings,
                            stoveType = stoveType,
                            stoveMaxLevel = hw.stovePowerMax,
                            ovenAvailable = hw.ovenAvailable,
                            ovenHasFan = hw.ovenHasFan,
                            airfryerAvailable = "airfryer" in _selectedEquipment.value,
                            dietType = dietSettings.value.dietType,
                            allergies = dietSettings.value.allergies,
                            language = language.value
                        )
                    ).requireValue()
                    val validation = CookingPlanValidator(_selectedEquipment.value, hw.stovePowerMax, stoveType, hw.ovenAvailable, _selectedEquipment.value.contains("airfryer"), dietSettings.value.dietType, dietSettings.value.allergies, selection.servings).validate(plan)
                    if (!validation.valid) {
                        throw ProviderFailure("VALIDATOR", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                    }
                    val target = targetTimeResolver.resolve(selection.targetTime).getOrElse { throw it }
                    val readyTimeIso = target.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    val session = RecipeSession(UUID.randomUUID().toString(), readyTimeIso, plan.ingredients.map { IngredientAmount(slugify(it.name), quantityToGrams(it.quantity, it.unit)) }, "kitchen", plan.steps.map { RecipeStep(it.id, it.type, it.resource, it.targetTemperatureC, it.durationSeconds, it.instruction, it.dependsOn) })
                    val result = orchestrator.startSession(session)
                    historyRepo.insertRecipe(session.sessionId, option.name, plan.ingredients.joinToString { "${it.quantity} ${it.unit} ${it.name}" }, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "started")
                    loadHistory()
                    _planState.value = activeRecipeState(option, result.events, selection.servings, readyTimeIso, plan)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                val message = readerSafeAiError(e)
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
                    _planState.value = currentState.copy(agentChatResponse = readerSafeAiError(e))
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
                _planState.value = currentState.copy(visionScanResponse = readerSafeAiError(e))
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
