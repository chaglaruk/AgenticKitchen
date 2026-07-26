package com.agentickitchen.android

import com.agentickitchen.android.ai.LlmProvider
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentickitchen.shared.agents.Orchestrator
import com.agentickitchen.shared.agents.PantryIntelAgent
import com.agentickitchen.shared.models.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.agentickitchen.shared.db.RecipeHistory
import com.agentickitchen.shared.db.RecipeHistoryRepository
import com.agentickitchen.shared.scheduler.TargetTimeResolver
import com.agentickitchen.android.data.preferences.AppPreferences
import com.agentickitchen.android.ai.AiProviderFactory
import com.agentickitchen.shared.ai.AiResult
import com.agentickitchen.shared.ai.StructuredRecipeParser
import com.agentickitchen.shared.ai.prompt.PromptFactory
import com.agentickitchen.shared.scheduler.TargetTimeChoice
import com.agentickitchen.shared.validator.CookingPlanValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// ── Dil ───────────────────────────────────────────────────────────────────
object L {
    val isTr: Boolean get() = Locale.getDefault().language == "tr"
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

data class IngredientCategory(val icon: String, val labelTr: String, val labelEn: String, val items: List<Pair<String, String>>) {
    val label get() = if (L.isTr) labelTr else labelEn
    fun displayItems() = items
}

val INGREDIENT_CATEGORIES = listOf(
    IngredientCategory("🥩", "Et & Tavuk", "Meat & Poultry", listOf("Tavuk göğsü" to "Chicken breast", "Kıyma" to "Ground beef", "Dana eti" to "Beef", "Somon" to "Salmon")),
    IngredientCategory("🥦", "Sebze", "Vegetables", listOf("Patates" to "Potato", "Soğan" to "Onion", "Domates" to "Tomato", "Mantar" to "Mushroom", "Sarımsak" to "Garlic")),
    IngredientCategory("🧀", "Süt & Yumurta", "Dairy & Eggs", listOf("Yumurta" to "Egg", "Kaşar peyniri" to "Kashar cheese", "Tereyağı" to "Butter", "Krema" to "Cream")),
    IngredientCategory("🌾", "Tahıl", "Grains", listOf("Pirinç" to "Rice", "Makarna" to "Pasta", "Nohut" to "Chickpeas")),
    IngredientCategory("🧄", "Baharat", "Spices", listOf("Zeytinyağı" to "Olive oil", "Tuz" to "Salt", "Karabiber" to "Black pepper", "Salça" to "Tomato paste"))
)

// ── UI States & Models ─────────────────────────────────────────────────
data class RecipeOption(val id: String, val type: String, val name: String, val description: String)

sealed class PlanState {
    object Idle : PlanState()
    object Loading : PlanState()
    data class OptionsReady(val options: List<RecipeOption>) : PlanState()
    data class RecipeActive(
        val recipe: RecipeOption, 
        val events: List<ScheduleEvent>, 
        val agentChatResponse: String? = null,
        val visionScanResponse: String? = null
    ) : PlanState()
    data class Error(val message: String) : PlanState()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}

data class HardwareSettings(
    val stoveType: String = "electric", val stovePowerMax: Int = 9,
    val ovenAvailable: Boolean = true, val ovenHasFan: Boolean = true, val ovenHasGrill: Boolean = false,
    val servingSize: Int = 2, val powerLevel: Int = 7,
    val geminiApiKey: String = "",
    val hfApiKey: String = "",
    val aiProvider: String = "FREE" // "GEMINI", "HUGGINGFACE", "DUCKDUCKGO", "FREE"
)
data class DietSettings(val dietType: String = "none", val allergies: Set<String> = emptySet())

// ── ViewModel ─────────────────────────────────────────────────────────────
class AppViewModel(
    private val prefs: AppPreferences,
    private val historyRepo: RecipeHistoryRepository,
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

    private val _mealTime = MutableStateFlow(prefs.mealTime())
    val mealTime: StateFlow<String> = _mealTime.asStateFlow()

    private val _chips = MutableStateFlow<List<String>>(emptyList())
    val chips: StateFlow<List<String>> = _chips.asStateFlow()

    private val _planState = MutableStateFlow<PlanState>(PlanState.Idle)
    val planState: StateFlow<PlanState> = _planState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private fun emitUiEvent(message: String) {
        viewModelScope.launch { _uiEvent.emit(UiEvent.ShowSnackbar(message)) }
    }

    private val _scannedIngredients = MutableStateFlow<List<String>?>(null)
    val scannedIngredients: StateFlow<List<String>?> = _scannedIngredients.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()
    fun clearAiError() { _aiError.value = null }

    private val _hw = MutableStateFlow(loadHardwareSettings())
    val hardwareSettings: StateFlow<HardwareSettings> = _hw.asStateFlow()
    val dietSettings = MutableStateFlow(prefs.dietSettings())
    val theme = MutableStateFlow(prefs.theme())
    val language = MutableStateFlow(prefs.language())
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

    init { loadHistory() }
    
    private fun loadHistory() {
        _history.value = historyRepo.getAllHistory()
    }

    fun completeSetup(equipment: Set<String>, servings: Int, mealTime: String, hw: HardwareSettings) {
        AppLogger.i("Setup", "Kurulum tamamlandı — ekipman: $equipment, porsiyon: $servings, saat: $mealTime, apiKey uzunluk: ${hw.geminiApiKey.length}")
        _selectedEquipment.value = equipment
        _mealTime.value = mealTime
        _hw.value = hw
        _setupDone.value = true
        _isEditingSetup.value = false
        prefs.saveSetup(true, equipment, servings, mealTime)
        saveHardwareSettings(hw)
        refreshPantryIntel()
    }

    fun startEditingSetup() { _isEditingSetup.value = true }
    fun cancelEditingSetup() { _isEditingSetup.value = false }
    fun addChip(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && !_chips.value.any { it.equals(trimmed, ignoreCase = true) }) {
            _chips.value = _chips.value + trimmed
            refreshPantryIntel()
        }
    }
    fun addMultipleChips(names: List<String>) {
        val existing = _chips.value.map { it.lowercase() }.toSet()
        val newChips = names
            .map { it.trim() }
            .filter { it.isNotEmpty() && !existing.contains(it.lowercase()) }
        _chips.value = _chips.value + newChips
        refreshPantryIntel()
    }
    fun removeChip(name: String) {
        _chips.value = _chips.value.filter { it != name }
        refreshPantryIntel()
    }
    fun clearAll() {
        _chips.value = emptyList()
        _planState.value = PlanState.Idle
        lastOptions = emptyList()
        refreshPantryIntel()
    }

    fun startSession(isRefresh: Boolean = false) {
        if (_chips.value.isEmpty()) { _planState.value = PlanState.Error(L.noIngredientError); return }
        AppLogger.i("Session", "startSession çağrıldı — malzemeler: ${_chips.value}")
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val prompt = PromptFactory.recipeOptionsPrompt(_chips.value, _selectedEquipment.value, dietSettings.value.dietType, dietSettings.value.allergies, language.value)
                    val parsed = StructuredRecipeParser.recipeOptions(provider.generateContent(prompt).orEmpty())
                    val response = (parsed as? AiResult.Success)?.value ?: throw IllegalArgumentException(parsed.failureOrNull()?.userMessage)
                    if (response.options.size != 3 || response.options.map { it.id }.toSet().size != 3 || response.options.any { it.id.isBlank() || it.name.isBlank() || it.estimatedMinutes <= 0 }) throw IllegalArgumentException("Invalid recipe options")
                    lastOptions = response.options.map { RecipeOption(it.id, it.difficulty, it.name, it.summary) }
                    _planState.value = PlanState.OptionsReady(lastOptions)
                }
            } catch (e: Exception) {
                AppLogger.aiError("Options", e)
                val msg = e.message ?: ""
                if (msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true) || msg.contains("429")) {
                    _aiError.value = "QUOTA_EXCEEDED"
                    val errorMsg = "Ajan hata verdi: API Kotası Doldu (High Demand)"
                    emitUiEvent(errorMsg)
                    _planState.value = PlanState.Error(errorMsg)
                } else {
                    val errorMsg = "Ajan hata verdi: ${e.message}"
                    emitUiEvent(errorMsg)
                    _planState.value = PlanState.Error(errorMsg)
                }
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

    fun refreshSession() {
        startSession(isRefresh = true)
    }

    fun selectRecipeOption(option: RecipeOption, choice: TargetTimeChoice) {
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val hw = _hw.value
                    val prompt = PromptFactory.cookingPlanPrompt(option.name, _chips.value, _selectedEquipment.value, hw.servingSize, hw.stovePowerMax, hw.ovenAvailable, hw.ovenHasFan, _selectedEquipment.value.contains("airfryer"), dietSettings.value.dietType, dietSettings.value.allergies, language.value)
                    val parsed = StructuredRecipeParser.cookingPlan(provider.generateContent(prompt).orEmpty())
                    val plan = (parsed as? AiResult.Success)?.value ?: throw IllegalArgumentException(parsed.failureOrNull()?.userMessage)
                    val validation = CookingPlanValidator(_selectedEquipment.value, hw.stovePowerMax, hw.ovenAvailable, _selectedEquipment.value.contains("airfryer"), dietSettings.value.dietType, dietSettings.value.allergies, hw.servingSize).validate(plan)
                    if (!validation.valid) throw IllegalArgumentException(validation.errors.joinToString { it.message })
                    val target = targetTimeResolver.resolve(choice).getOrElse { throw it }
                    val session = RecipeSession(UUID.randomUUID().toString(), target.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), plan.ingredients.map { IngredientAmount(slugify(it.name), quantityToGrams(it.quantity, it.unit)) }, "kitchen", plan.steps.map { RecipeStep(it.id, it.type, it.resource, it.targetTemperatureC, it.durationSeconds, it.instruction, it.dependsOn) })
                    val result = orchestrator.startSession(session)
                    historyRepo.insertRecipe(session.sessionId, option.name, plan.ingredients.joinToString { "${it.quantity} ${it.unit} ${it.name}" }, ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), "started")
                    loadHistory()
                    _planState.value = PlanState.RecipeActive(option, result.events)
                }
            } catch (e: Exception) {
                val message = e.message ?: "Unable to create a valid cooking plan. Please retry."
                emitUiEvent(message)
                _planState.value = PlanState.Error(message)
            }
        }
    }

    @Deprecated("Replaced by structured JSON cooking plans")
    private fun legacySelectRecipeOption(option: RecipeOption, targetTime: String) {
        viewModelScope.launch {
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val hwInfo = _hw.value
                    val equipment = _selectedEquipment.value.joinToString(", ")
                    val hardwareContext = "Donanım: Ocak tipi=${hwInfo.stoveType}, Max Güç=${hwInfo.stovePowerMax}. " +
                        (if (hwInfo.ovenAvailable) "Fırın: Mevcut (Fan=${hwInfo.ovenHasFan}, Izgara=${hwInfo.ovenHasGrill}). " else "") +
                        (if (_selectedEquipment.value.contains("airfryer")) "ÖNEMLİ: Airfryer mevcut, kızartma adımlarında Airfryer'ı tercih et." else "")
                    
                    val prompt = "Seçilen yemek: ${option.name}. Malzemeler: ${_chips.value.joinToString(", ")}. $hardwareContext. Bu yemeği yapmak için askeri düzeyde kesin komutlarla bir tarif oluştur. Süreleri dakika cinsinden tam sayı olarak yaz. Komutlar 'ceviz büyüklüğünde' gibi insan dostu ama 'ocağı 9. seviyeye al' gibi katı askeri dilde olsun. Format:\n" +
                            "prep|Soğanları serçe parmağı kalınlığında doğra.|3\n" +
                            "cook|Ocağı ${hwInfo.stovePowerMax}. seviyeye al ve ısıt.|2\n" +
                            "rest|Ocağı kapat ve bekle.|5\n" +
                            "Açıklama vb. yazma, sadece her adım için yeni bir satır."
                    
                    val responseText = provider.generateContent(prompt) ?: ""
                    val steps = responseText.split("\n").filter { it.contains("|") }.mapIndexed { idx, line ->
                        val p = line.split("|")
                        RecipeStep("step_$idx", p[0].trim(), "stovetop", durationSec = (p.getOrNull(2)?.trim()?.toIntOrNull() ?: 5) * 60, instruction = p[1].trim())
                    }
                    if (steps.isNotEmpty()) {
                        val session = RecipeSession(
                            sessionId = UUID.randomUUID().toString(), targetTimeIso = buildTargetIso(targetTime),
                            ingredients = _chips.value.map { IngredientAmount(id = slugify(it), massG = 200) },
                            hardwareProfileId = "stovetop", steps = steps
                        )
                        val result = orchestrator.startSession(session)
                        historyRepo.insertRecipe(
                            id = session.sessionId,
                            name = option.name,
                            ingredients = _chips.value.joinToString(", "),
                            timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            status = "started"
                        )
                        loadHistory()
                        _planState.value = PlanState.RecipeActive(option, result.events)
                    } else {
                        throw Exception("Adım oluşturulamadı")
                    }
                }
            } catch(e:Exception) {
                if (e.message?.contains("API_KEY_MISSING") == true) {
                    val hwInfo = _hw.value
                    val steps = mutableListOf<RecipeStep>()
                    steps += RecipeStep("prep1", "prep", "stovetop", durationSec = 300, instruction = "Tüm malzemeleri tezgaha diz. Soğanları serçe parmağının ucu büyüklüğünde (brunoise) doğra.")
                    steps += RecipeStep("heat1", "preheat", "stovetop", durationSec = 60, dependsOn = listOf("prep1"), instruction = "Tavayı ocağa koy. Güç seviyesini ${hwInfo.stovePowerMax} (Maks) konumuna al. 60 saniye ısınmasını bekle.")
                    steps += RecipeStep("cook1", "cook", "stovetop", durationSec = 300, dependsOn = listOf("heat1"), instruction = "Etleri tavaya diz, birbirine değmesin. Güç seviyesini ${(hwInfo.stovePowerMax * 0.75).toInt()}'a düşür. 5 dakika mühürle.")
                    val session = RecipeSession(
                        sessionId = UUID.randomUUID().toString(), targetTimeIso = buildTargetIso(targetTime),
                        ingredients = _chips.value.map { IngredientAmount(id = slugify(it), massG = 200) },
                        hardwareProfileId = "stovetop", steps = steps
                    )
                    val result = orchestrator.startSession(session)
                    historyRepo.insertRecipe(
                        id = session.sessionId,
                        name = option.name,
                        ingredients = _chips.value.joinToString(", "),
                        timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        status = "started"
                    )
                    loadHistory()
                    _planState.value = PlanState.RecipeActive(option, result.events)
                } else if (e.message?.contains("quota", ignoreCase = true) == true || e.message?.contains("rate", ignoreCase = true) == true || e.message?.contains("429") == true) {
                    _aiError.value = "QUOTA_EXCEEDED"
                    val errorMsg = "Tarif oluşturulamadı: API Kotası Doldu (High Demand)"
                    emitUiEvent(errorMsg)
                    _planState.value = PlanState.Error(errorMsg)
                } else {
                    val errorMsg = "Tarif oluşturulamadı: ${e.message}"
                    emitUiEvent(errorMsg)
                    _planState.value = PlanState.Error(errorMsg)
                }
            }
        }
    }

    private fun getGeminiModel(modelName: String = "gemini-1.5-flash"): GenerativeModel? {
        return providerFactory.gemini(_hw.value, modelName)
    }

    private fun getActiveProvider(): LlmProvider? {
        return providerFactory.provider(_hw.value.copy(aiProvider = "GEMINI"))
    }

    private suspend fun <T> executeAiWithProvider(action: suspend (LlmProvider) -> T): T {
        val provider = getActiveProvider() ?: throw Exception("API_KEY_MISSING")
        return try {
            action(provider)
        } catch (e: Exception) {
            AppLogger.e("AiProvider", "Provider hatası: ${e.message}")
            throw e
        }
    }

    private suspend fun <T> executeAiWithFallback(action: suspend (GenerativeModel) -> T): T {
        // This is legacy for Gemini-specific stuff like Vision. 
        // For text-only, we should use executeAiWithProvider.
        val primaryModel = getGeminiModel("gemini-1.5-flash") ?: throw Exception("API_KEY_MISSING")
        return try {
            action(primaryModel)
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true) || msg.contains("429") || msg.contains("unexpected", ignoreCase = true)) {
                AppLogger.w("GeminiFallback", "1.5-flash hata verdi ($msg), 2.0-flash deneniyor...")
                val fallbackModel = getGeminiModel("gemini-2.0-flash") ?: throw e
                action(fallbackModel)
            } else {
                throw e
            }
        }
    }

    fun askIngredientAgent(question: String) {
        val currentState = _planState.value
        if (currentState is PlanState.RecipeActive) {
            _planState.value = currentState.copy(agentChatResponse = "Ajan düşünüyor...")
            viewModelScope.launch {
                try {
                    executeAiWithProvider { provider ->
                        val prompt = "Sen askeri disiplinle çalışan katı bir şef asistanısın. Şu anki tarif: ${currentState.recipe.name}. Kullanıcı sorusu: '$question'. Değişiklik lezzeti bozuyorsa 'HAYIR!' diyerek kısa ve net reddet ve alternatif sun. İyiyse onay ver. Askeri ve çok kısa bir dille cevapla."
                        val responseText = provider.generateContent(prompt)
                        _planState.value = currentState.copy(agentChatResponse = responseText ?: "Anlaşılamadı.")
                    }
                } catch (e: Exception) {
                    if (e.message?.contains("API_KEY_MISSING") == true) {
                        _planState.value = currentState.copy(agentChatResponse = "Gemini API Key eksik. Gerçek ajan yetenekleri için Settings'ten ekleyin.")
                    } else {
                        _planState.value = currentState.copy(agentChatResponse = "Hata: ${e.message}")
                    }
                }
            }
        }
    }

    fun clearScannedIngredients() { _scannedIngredients.value = null }

    fun scanIngredients(image: Bitmap) {
        AppLogger.i("Vision", "scanIngredients çağrıldı — bitmap: ${image.width}x${image.height}")
        viewModelScope.launch {
            val hw = _hw.value
            val geminiKey = hw.geminiApiKey
            val aiProvider = hw.aiProvider

            // Gemini sadece provider GEMINI ise ve key varsa dene
            if (aiProvider == "GEMINI" && geminiKey.isNotBlank()) {
                try {
                    executeAiWithFallback { model ->
                        val prompt = "Resimdeki yiyecek malzemelerini (sebze, et, baharat vb.) tespit et ve sadece aralarına virgül koyarak Türkçe kelimeler olarak listele. Başka hiçbir açıklama yazma."
                        AppLogger.aiRequest("ScanIngr-Gemini", prompt)
                        val response = model.generateContent(content {
                            image(image)
                            text(prompt)
                        })
                        val text = response.text ?: ""
                        AppLogger.aiResponse("ScanIngr-Gemini", text)
                        val items = text.split(",").map { it.trim().replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }.filter { it.isNotBlank() && !it.startsWith("Hata") }
                        _scannedIngredients.value = items
                    }
                    return@launch
                } catch (e: Exception) {
                    AppLogger.w("Vision", "Gemini vision failed, falling back to HF: ${e.message}")
                }
            }

            // Real Free Vision Fallback (Hugging Face + Text AI)
            var caption: String? = null
            try {
                AppLogger.i("ScanIngr", "Hugging Face ile gerçek analiz yapılıyor...")
                val visionService = providerFactory.vision(hw)
                caption = visionService.analyzeImage(image)
            } catch (e: Exception) {
                AppLogger.w("ScanIngr", "HF Vision başarısız: ${e.message}")
            }

            // Text-based fallback: Eğer HF başarısız olsa bile metin sorgusu yap
            try {
                val provider = getActiveProvider() ?: throw Exception("API_KEY_MISSING")
                val textPrompt = if (caption != null) {
                    "Şu görsel açıklamasındaki yiyecek malzemelerini (sebze, et, baharat vb.) tespit et ve sadece aralarına virgül koyarak Türkçe kelimeler olarak listele: '$caption'. Başka hiçbir metin yazma."
                } else {
                    "Tipik yemeklerde kullanılan temel malzemeleri (sebze, et, balık, baharat, krema, un vb.) virgülle ayrılmış 5-10 kelime olarak Türkçe listele. Başka açıklama yazma."
                }
                AppLogger.aiRequest("ScanIngr-Text", textPrompt)
                val responseText = provider.generateContent(textPrompt) ?: ""
                AppLogger.aiResponse("ScanIngr-Text", responseText)
                val items = responseText.split(",").map { it.trim().replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() } }.filter { it.isNotBlank() && !it.startsWith("Hata") }
                if (items.isNotEmpty()) {
                    _scannedIngredients.value = items
                    val source = if (caption != null) "Hugging Face + ${hw.aiProvider}" else "AI (metin tabanlı)"
                    emitUiEvent("Görsel analiz $source ile yapıldı.")
                } else {
                    throw Exception("Boş malzeme listesi alındı")
                }
            } catch (e: Exception) {
                AppLogger.aiError("ScanIngr", e)
                val msg = e.message ?: ""
                if (msg.contains("API_KEY_MISSING")) {
                    _aiError.value = "API_KEY_MISSING"
                } else if (msg.contains("quota", ignoreCase = true) || msg.contains("rate", ignoreCase = true) || msg.contains("429")) {
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
        val hw = _hw.value
        val geminiKey = hw.geminiApiKey
        val aiProvider = hw.aiProvider

        viewModelScope.launch {
            // Gemini sadece provider GEMINI ise ve key varsa dene
            if (aiProvider == "GEMINI" && geminiKey.isNotBlank()) {
                try {
                    executeAiWithFallback { model ->
                        val prompt = "Şu anki tarif: ${currentState.recipe.name}. Askeri bir şef gibi bu fotoğrafı incele. Yemek ne durumda? Ocağı kapatmalı mıyız, devam mı etmeliyiz? Kısaca, net bir emir ver."
                        val response = model.generateContent(content {
                            image(image)
                            text(prompt)
                        })
                        _planState.value = currentState.copy(visionScanResponse = response.text ?: "Görüntü anlaşılamadı.")
                    }
                    return@launch
                } catch (e: Exception) {
                    AppLogger.w("Vision", "Gemini check vision failed: ${e.message}")
                }
            }

            // Free Vision Check Fallback
            try {
                val visionService = providerFactory.vision(hw)
                val caption = visionService.analyzeImage(image)
                if (caption != null) {
                    val provider = getActiveProvider() ?: throw Exception("API_KEY_MISSING")
                    val prompt = "Şu anki tarif: ${currentState.recipe.name}. Görseldeki durum şu şekilde betimlendi: '$caption'. Askeri bir şef gibi durumu değerlendir ve net bir emir ver."
                    val responseText = provider.generateContent(prompt) ?: "Analiz başarısız."
                    _planState.value = currentState.copy(visionScanResponse = responseText)
                } else {
                    _planState.value = currentState.copy(visionScanResponse = "Görsel analizi yapılamadı (HF).")
                }
            } catch (e: Exception) {
                _planState.value = currentState.copy(visionScanResponse = "Görsel analizi hatası: ${e.message}")
            }
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
        _hw.value = hw
        prefs.saveHardwareSettings(hw)
        refreshPantryIntel()
    }

    fun saveApiKey(key: String) {
        val current = _hw.value
        val updated = when (current.aiProvider) {
            "HUGGINGFACE" -> current.copy(hfApiKey = key)
            else -> current.copy(geminiApiKey = key)
        }
        saveHardwareSettings(updated)
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
        language.value = lang
        prefs.saveLanguage(lang)
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
        return prefs.hardwareSettings()
    }

    private fun quantityToGrams(quantity: Double, unit: String): Int = when (unit.lowercase()) {
        "kg" -> (quantity * 1000).toInt()
        "g" -> quantity.toInt()
        "ml" -> quantity.toInt()
        "l" -> (quantity * 1000).toInt()
        else -> quantity.toInt().coerceAtLeast(1)
    }


    private fun slugify(name: String) = name.lowercase().replace("ğ", "g").replace("ü", "u").replace("ş", "s").replace("ı", "i").replace("ö", "o").replace("ç", "c").replace(Regex("[^a-z0-9]"), "_")
    private fun buildTargetIso(timeText: String): String {
        val parts = timeText.split(":"); val now = ZonedDateTime.now()
        var target = now.withHour(parts.getOrNull(0)?.toIntOrNull() ?: 19).withMinute(parts.getOrNull(1)?.toIntOrNull() ?: 0).withSecond(0).withNano(0)
        if (target.isBefore(now)) target = target.plusDays(1)
        return target.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}
