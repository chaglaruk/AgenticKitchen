from pathlib import Path


def replace_once(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'Anchor not found in {path}: {old[:140]!r}')
    if text.count(old) != 1:
        raise SystemExit(f'Anchor not unique in {path}: {text.count(old)} matches')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


def append_after(path: str, marker: str, addition: str):
    replace_once(path, marker, marker + addition)


# 1) Recipe measurement units used by real imported recipes should normalize deterministically.
replace_once(
    'shared/src/main/kotlin/com/agentickitchen/shared/inventory/PantryInventory.kt',
    '            "bunch", "demet" -> NormalizedAmount(quantity, "demet", UnitDimension.BUNCH)\n            else -> NormalizedAmount(quantity, unit.trim().ifBlank { "birim" }, UnitDimension.UNKNOWN)\n',
    '            "bunch", "bunches", "demet" -> NormalizedAmount(quantity, "demet", UnitDimension.BUNCH)\n'
    '            "cup", "cups" -> NormalizedAmount(quantity * 240.0, "ml", UnitDimension.VOLUME)\n'
    '            "tbsp", "tablespoon", "tablespoons" -> NormalizedAmount(quantity * 15.0, "ml", UnitDimension.VOLUME)\n'
    '            "tsp", "teaspoon", "teaspoons" -> NormalizedAmount(quantity * 5.0, "ml", UnitDimension.VOLUME)\n'
    '            else -> NormalizedAmount(quantity, unit.trim().ifBlank { "birim" }, UnitDimension.UNKNOWN)\n'
)

# 2) Validator accepts the inventory/count/package unit vocabulary that import guard already understands.
replace_once(
    'shared/src/main/kotlin/com/agentickitchen/shared/validator/CookingPlanValidator.kt',
    '            if (normalizedUnit !in setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "piece", "pieces", "slice", "slices", "clove", "pinch", "unit", "to taste", "")) {',
    '            if (normalizedUnit !in setOf("g", "kg", "ml", "l", "tsp", "tbsp", "cup", "piece", "pieces", "count", "adet", "package", "packages", "pack", "packs", "paket", "bunch", "bunches", "demet", "slice", "slices", "clove", "pinch", "unit", "to taste", "")) {'
)

# 3) Fail-closed import draft policy shared by UI and preparation pipeline.
append_after(
    'shared/src/main/kotlin/com/agentickitchen/shared/inventory/RecipeImportPantryPlanner.kt',
    '''data class RecipeImportPantrySummary(
    val matches: List<RecipeImportIngredientMatch>
) {
    val availableCount: Int get() = matches.count { it.availability == RecipeImportAvailability.AVAILABLE }
    val partialCount: Int get() = matches.count { it.availability == RecipeImportAvailability.PARTIAL }
    val missingCount: Int get() = matches.count { it.availability == RecipeImportAvailability.MISSING }
    val needsReviewCount: Int get() = matches.count { it.availability == RecipeImportAvailability.NEEDS_REVIEW }
    val readyForValidatedPlan: Boolean get() = needsReviewCount == 0
}
''',
    '''

object RecipeImportDraftPolicy {
    fun issues(recipe: ImportedRecipe): List<String> = buildList {
        if (recipe.name.isBlank()) add("recipe_name_missing")
        if (recipe.servings == null || recipe.servings <= 0) add("servings_missing")
        if (recipe.ingredients.isEmpty()) add("ingredients_missing")
        recipe.ingredients.forEachIndexed { index, ingredient ->
            if (ingredient.displayName.isBlank()) add("ingredient_name_$index")
            val quantity = ingredient.quantity
            val unit = ingredient.unit
            if (quantity == null || !quantity.isFinite() || quantity <= 0.0) add("ingredient_quantity_$index")
            if (unit.isNullOrBlank()) {
                add("ingredient_unit_$index")
            } else if (quantity != null && quantity.isFinite() && quantity > 0.0) {
                val normalized = runCatching { InventoryUnits.normalize(quantity, unit) }.getOrNull()
                if (normalized == null || normalized.dimension == UnitDimension.UNKNOWN) add("ingredient_unit_$index")
            }
        }
        if (recipe.instructions.isEmpty() || recipe.instructions.any(String::isBlank)) add("instructions_missing")
    }.distinct()

    fun canPrepare(recipe: ImportedRecipe): Boolean = issues(recipe).isEmpty()
}
'''
)

# 4) AppViewModel recipe import state, URL/text/photo/share flows and imported-plan orchestration.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    'import com.agentickitchen.android.ai.ProviderFailureCategory\n',
    'import com.agentickitchen.android.ai.ProviderFailureCategory\nimport com.agentickitchen.android.ai.RecipeImportUrlLoader\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    'import com.agentickitchen.shared.ai.AiFailureType\n',
    '''import com.agentickitchen.shared.ai.AiFailureType
import com.agentickitchen.shared.ai.DeterministicRecipeImportParser
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.RecipeImportNormalizer
import com.agentickitchen.shared.ai.RecipeImportResponse
import com.agentickitchen.shared.ai.RecipeImportSource
import com.agentickitchen.shared.ai.RecipePhotoImportRequest
import com.agentickitchen.shared.ai.RecipeTextImportRequest
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    'import com.agentickitchen.shared.inventory.RecipeMatchTier\n',
    '''import com.agentickitchen.shared.inventory.RecipeMatchTier
import com.agentickitchen.shared.inventory.RecipeImportDraftPolicy
import com.agentickitchen.shared.inventory.RecipeImportPantryPlanner
import com.agentickitchen.shared.inventory.RecipeImportPantrySummary
import com.agentickitchen.shared.inventory.RecipeImportPlanGuard
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''sealed interface KitchenScanState {
''',
    '''sealed interface RecipeImportState {
    data object Idle : RecipeImportState
    data class Loading(val source: String) : RecipeImportState
    data class Review(
        val response: RecipeImportResponse,
        val pantry: RecipeImportPantrySummary
    ) : RecipeImportState
    data class Error(val message: String) : RecipeImportState
}

sealed interface KitchenScanState {
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''    private val _shoppingImportState = MutableStateFlow<ShoppingImportState>(ShoppingImportState.Idle)
    val shoppingImportState: StateFlow<ShoppingImportState> = _shoppingImportState.asStateFlow()
''',
    '''    private val _shoppingImportState = MutableStateFlow<ShoppingImportState>(ShoppingImportState.Idle)
    val shoppingImportState: StateFlow<ShoppingImportState> = _shoppingImportState.asStateFlow()
    private val _recipeImportState = MutableStateFlow<RecipeImportState>(RecipeImportState.Idle)
    val recipeImportState: StateFlow<RecipeImportState> = _recipeImportState.asStateFlow()
    private var recipeImportJob: Job? = null
'''
)

recipe_methods = r'''

    fun importRecipeText(text: String) {
        importRecipeTextInternal(text, sourceOverride = null)
    }

    fun importSharedRecipe(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val singleUrl = clean.takeIf { it.matches(Regex("(?is)^https?://\\S+$")) }
        if (singleUrl != null) {
            importRecipeUrlInternal(singleUrl, RecipeImportSource.ANDROID_SHARE)
        } else {
            importRecipeTextInternal(clean, RecipeImportSource.ANDROID_SHARE)
        }
    }

    private fun importRecipeTextInternal(text: String, sourceOverride: RecipeImportSource?) {
        val clean = text.trim()
        if (clean.isBlank()) return
        if (clean.length > RecipeImportUrlLoader.MAX_AI_TEXT_CHARS) {
            _recipeImportState.value = RecipeImportState.Error(
                if (L.isTr) "Tarif metni çok uzun. Daha kısa bir tarif metni kullan." else "The recipe text is too long. Use a shorter recipe text."
            )
            return
        }
        recipeImportJob?.cancel()
        val deterministic = DeterministicRecipeImportParser.parsePlainText(clean, sourceLabel = if (L.isTr) "Yapıştırılan tarif" else "Pasted recipe")
        if (deterministic != null) {
            presentRecipeImport(deterministic, sourceOverride)
            return
        }
        recipeImportJob = viewModelScope.launch {
            _recipeImportState.value = RecipeImportState.Loading("text")
            try {
                val response = executeAiWithProvider { provider ->
                    provider.parseRecipeText(
                        RecipeTextImportRequest(
                            text = clean,
                            language = language.value,
                            sourceLabel = if (L.isTr) "Yapıştırılan tarif" else "Pasted recipe"
                        )
                    ).requireValue()
                }
                presentRecipeImport(response, sourceOverride)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _recipeImportState.value = RecipeImportState.Error(recipeImportError(error))
            }
        }
    }

    fun importRecipeUrl(url: String) {
        importRecipeUrlInternal(url, sourceOverride = null)
    }

    private fun importRecipeUrlInternal(url: String, sourceOverride: RecipeImportSource?) {
        val clean = url.trim()
        if (clean.isBlank()) return
        recipeImportJob?.cancel()
        recipeImportJob = viewModelScope.launch {
            _recipeImportState.value = RecipeImportState.Loading("url")
            try {
                val loaded = RecipeImportUrlLoader().use { loader ->
                    loader.load(clean).getOrElse { throw it }
                }
                val deterministic = DeterministicRecipeImportParser.parseJsonLd(
                    loaded.body,
                    sourceLabel = loaded.sourceLabel,
                    sourceUrl = loaded.finalUrl
                )
                val response = deterministic ?: executeAiWithProvider { provider ->
                    val visibleText = RecipeImportUrlLoader.visibleRecipeText(loaded.body)
                    if (visibleText.isBlank()) throw IllegalArgumentException("No visible recipe text")
                    provider.parseRecipeText(
                        RecipeTextImportRequest(
                            text = visibleText,
                            language = language.value,
                            sourceLabel = loaded.sourceLabel,
                            sourceUrl = loaded.finalUrl
                        )
                    ).requireValue()
                }
                presentRecipeImport(response, sourceOverride)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _recipeImportState.value = RecipeImportState.Error(recipeImportError(error))
            }
        }
    }

    fun importRecipePhoto(image: Bitmap) {
        recipeImportJob?.cancel()
        recipeImportJob = viewModelScope.launch {
            _recipeImportState.value = RecipeImportState.Loading("photo")
            try {
                val response = executeAiWithProvider { provider ->
                    provider.scanRecipePhoto(
                        RecipePhotoImportRequest(
                            image = encodeKitchenImage(image),
                            language = language.value,
                            sourceLabel = if (L.isTr) "Tarif fotoğrafı" else "Recipe photo"
                        )
                    ).requireValue()
                }
                presentRecipeImport(response, null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _recipeImportState.value = RecipeImportState.Error(recipeImportError(error))
            }
        }
    }

    private fun presentRecipeImport(response: RecipeImportResponse, sourceOverride: RecipeImportSource?): Boolean {
        val source = sourceOverride ?: response.source
        val normalized = RecipeImportNormalizer.normalize(
            response = response,
            source = source,
            sourceLabel = response.recipe.sourceLabel,
            sourceUrl = response.recipe.sourceUrl
        ) ?: run {
            _recipeImportState.value = RecipeImportState.Error(
                if (L.isTr) "Tarif güvenilir biçimde okunamadı." else "The recipe could not be read reliably."
            )
            return false
        }
        val pantry = RecipeImportPantryPlanner.compare(normalized.recipe, _inventory.value, reservedQuantities())
        _recipeImportState.value = RecipeImportState.Review(normalized, pantry)
        return true
    }

    fun clearRecipeImport() {
        recipeImportJob?.cancel()
        recipeImportJob = null
        _recipeImportState.value = RecipeImportState.Idle
    }

    private fun recipeImportError(error: Throwable): String {
        if (error is AiRequestException && error.failure.technicalMessage == "recipe_import_not_supported") {
            return if (L.isTr) {
                "Seçili çevrimdışı sağlayıcı tarif içe aktaramıyor. Firebase veya Gemini seç."
            } else {
                "The selected offline provider cannot import recipes. Choose Firebase or Gemini."
            }
        }
        val message = error.message.orEmpty()
        if (message.contains("Blocked recipe URL host", ignoreCase = true) ||
            message.contains("Only HTTP(S)", ignoreCase = true) ||
            message.contains("Recipe URL credentials", ignoreCase = true)) {
            return if (L.isTr) "Bu tarif bağlantısı güvenlik nedeniyle açılamadı." else "This recipe link was blocked for safety."
        }
        if (message.contains("too large", ignoreCase = true)) {
            return if (L.isTr) "Tarif sayfası çok büyük." else "The recipe page is too large."
        }
        return readerSafeCurrentProviderError(error)
    }

    fun prepareImportedRecipe(recipe: ImportedRecipe) {
        if (!canReplacePreparedRecipe(_cookingState.value.status)) {
            emitUiEvent(
                if (L.isTr) "Devam eden pişirmeyi bitirmeden başka bir tarif hazırlayamazsın."
                else "Finish the active cooking session before preparing another recipe."
            )
            return
        }
        val issues = RecipeImportDraftPolicy.issues(recipe)
        if (issues.isNotEmpty()) {
            emitUiEvent(
                if (L.isTr) "Tarif adı, porsiyon, miktarlar, birimler ve adımlar tamamlanmalı."
                else "Recipe name, servings, amounts, units, and steps must be complete."
            )
            return
        }
        val sourceReview = _recipeImportState.value as? RecipeImportState.Review ?: return
        val normalizedResponse = RecipeImportNormalizer.normalize(
            response = sourceReview.response.copy(recipe = recipe),
            source = sourceReview.response.source,
            sourceLabel = recipe.sourceLabel,
            sourceUrl = recipe.sourceUrl
        ) ?: return
        val imported = normalizedResponse.recipe
        val importedPantry = RecipeImportPantryPlanner.compare(imported, _inventory.value, reservedQuantities())
        if (!importedPantry.readyForValidatedPlan) {
            _recipeImportState.value = RecipeImportState.Review(normalizedResponse, importedPantry)
            emitUiEvent(if (L.isTr) "Önce belirsiz tarif miktarlarını düzelt." else "Resolve the uncertain recipe amounts first.")
            return
        }

        viewModelScope.launch {
            _recipeImportState.value = RecipeImportState.Loading("prepare")
            _planState.value = PlanState.Loading
            try {
                executeAiWithProvider { provider ->
                    val hw = _hw.value
                    val servings = imported.servings ?: throw IllegalArgumentException("Missing servings")
                    val plan = normalizeCookingPlan(
                        provider.generateCookingPlan(
                            CookingPlanRequest(
                                recipeName = imported.name,
                                ingredients = imported.ingredients.map { it.displayName },
                                equipment = _selectedEquipment.value,
                                servings = servings,
                                stoveType = selectedStoveType(),
                                stoveMaxLevel = hw.stovePowerMax,
                                ovenAvailable = hw.ovenAvailable,
                                ovenHasFan = hw.ovenHasFan,
                                airfryerAvailable = "airfryer" in _selectedEquipment.value,
                                dietType = dietSettings.value.dietType,
                                allergies = dietSettings.value.allergies,
                                language = language.value,
                                inventoryLines = _inventory.value.map { "${it.quantity} ${it.unit} ${it.originalName}" },
                                sourceRecipeIngredientLines = imported.ingredients.map { ingredient ->
                                    ingredient.rawText ?: "${ingredient.quantity} ${ingredient.unit} ${ingredient.displayName}"
                                },
                                sourceRecipeInstructions = imported.instructions
                            )
                        ).requireValue()
                    )
                    val sourceGuard = RecipeImportPlanGuard.validate(imported, plan)
                    if (!sourceGuard.valid) {
                        AppLogger.w("RecipeImportGuard", sourceGuard.reasons.joinToString("_"))
                        throw ProviderFailure("RECIPE_IMPORT", ProviderFailureCategory.CONSTRAINT_CONFLICT)
                    }
                    val validation = CookingPlanValidator(
                        _selectedEquipment.value,
                        hw.stovePowerMax,
                        selectedStoveType(),
                        hw.ovenAvailable,
                        _selectedEquipment.value.contains("airfryer"),
                        dietSettings.value.dietType,
                        dietSettings.value.allergies,
                        servings
                    ).validate(plan)
                    if (!validation.valid) throw PlanValidationException(validation.errors)

                    val usagePlan = InventoryWorkflow.planUsage(plan, _inventory.value, reservedQuantities())
                    val sessionId = UUID.randomUUID().toString()
                    val sequentialSeconds = plan.steps.sumOf { it.durationSeconds.toLong() }.coerceAtLeast(60L)
                    val readyTimeIso = ZonedDateTime.now().plusSeconds(sequentialSeconds)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    val option = RecipeOption(
                        id = "import-$sessionId",
                        type = "imported",
                        name = imported.name,
                        description = if (L.isTr) "İçe aktarılan tarif" else "Imported recipe",
                        sourceLabel = imported.sourceLabel ?: when (normalizedResponse.source) {
                            RecipeImportSource.URL_JSON_LD -> if (L.isTr) "Web tarifi" else "Web recipe"
                            RecipeImportSource.PLAIN_TEXT -> if (L.isTr) "Metin tarifi" else "Text recipe"
                            RecipeImportSource.AI_TEXT -> if (L.isTr) "AI ile çıkarılan tarif" else "AI-extracted recipe"
                            RecipeImportSource.AI_PHOTO -> if (L.isTr) "Fotoğraftan tarif" else "Recipe from photo"
                            RecipeImportSource.ANDROID_SHARE -> if (L.isTr) "Paylaşılan tarif" else "Shared recipe"
                        },
                        proposedIngredients = plan.ingredients,
                        shortages = usagePlan.shortages,
                        matchTier = when (usagePlan.shortages.size) {
                            0 -> RecipeMatchTier.READY_NOW
                            1 -> RecipeMatchTier.MISSING_ONE
                            else -> RecipeMatchTier.MISSING_TWO
                        },
                        pantryCoveragePercent = if (imported.ingredients.isEmpty()) 0 else
                            ((importedPantry.availableCount * 100.0) / imported.ingredients.size).toInt(),
                        servings = servings,
                        canPrepareFromPantry = usagePlan.shortages.isEmpty()
                    )
                    val session = RecipeSession(
                        sessionId,
                        readyTimeIso,
                        plan.ingredients.map { IngredientAmount(slugify(it.name), quantityToGrams(it.quantity, it.unit)) },
                        "kitchen",
                        plan.steps.map { RecipeStep(it.id, it.type, it.resource, it.targetTemperatureC, it.durationSeconds, it.instruction, it.dependsOn) }
                    )
                    val schedule = orchestrator.startSession(session)
                    historyRepo.insertRecipe(
                        sessionId,
                        imported.name,
                        plan.ingredients.joinToString { "${it.quantity} ${it.unit} ${it.name}" },
                        ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        "started"
                    )
                    loadHistory()
                    inventoryRecipeRequest = null
                    lastOptions = emptyList()
                    cookingTicker?.cancel()
                    recentCookingTurns.clear()
                    _cookingState.value = preparedCookingState(imported.name)
                    _planState.value = activeRecipeState(
                        sessionId,
                        option,
                        schedule.events,
                        servings,
                        readyTimeIso,
                        plan,
                        usagePlan.usages,
                        usagePlan.shortages
                    )
                    _recipeImportState.value = RecipeImportState.Review(normalizedResponse, importedPantry)
                    persistActiveSession()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = when (error) {
                    is PlanValidationException -> readerSafePlanValidationError(error.validationErrors)
                    else -> recipeImportError(error)
                }
                _recipeImportState.value = RecipeImportState.Review(normalizedResponse, importedPantry)
                _planState.value = PlanState.Error(message, canUseOffline = false)
                emitUiEvent(message)
            }
        }
    }
'''

replace_once(
    'app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt',
    '''    fun clearShoppingImport() {
        _shoppingImportState.value = ShoppingImportState.Idle
    }
''',
    '''    fun clearShoppingImport() {
        _shoppingImportState.value = ShoppingImportState.Idle
    }
''' + recipe_methods
)

# 5) MainActivity accepts Android text shares and routes recipe import state/callbacks to Home.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    'import android.os.Bundle\n',
    'import android.content.Intent\nimport android.os.Bundle\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
''',
    '''    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleRecipeShare(intent)
        setContent {
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''        }
    }
}

sealed class Screen''',
    '''        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRecipeShare(intent)
    }

    private fun handleRecipeShare(intent: Intent?) {
        recipeSharePayload(intent?.action, intent?.type, intent?.getStringExtra(Intent.EXTRA_TEXT))
            ?.let(viewModel::importSharedRecipe)
    }
}

internal fun recipeSharePayload(action: String?, mimeType: String?, text: String?): String? {
    if (action != Intent.ACTION_SEND || mimeType?.startsWith("text/") != true) return null
    return text?.trim()?.takeIf(String::isNotEmpty)
}

sealed class Screen'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''    val shoppingImportState by viewModel.shoppingImportState.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
''',
    '''    val shoppingImportState by viewModel.shoppingImportState.collectAsState()
    val recipeImportState by viewModel.recipeImportState.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''                            shoppingImportState = shoppingImportState,
                            shoppingList = shoppingList,
''',
    '''                            shoppingImportState = shoppingImportState,
                            recipeImportState = recipeImportState,
                            shoppingList = shoppingList,
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/MainActivity.kt',
    '''                            onClearShoppingImport = viewModel::clearShoppingImport,
                            onToggleShoppingItem = viewModel::setShoppingItemChecked,
''',
    '''                            onClearShoppingImport = viewModel::clearShoppingImport,
                            onImportRecipeText = viewModel::importRecipeText,
                            onImportRecipeUrl = viewModel::importRecipeUrl,
                            onImportRecipePhoto = viewModel::importRecipePhoto,
                            onPrepareImportedRecipe = viewModel::prepareImportedRecipe,
                            onClearRecipeImport = viewModel::clearRecipeImport,
                            onToggleShoppingItem = viewModel::setShoppingItemChecked,
'''
)

# 6) Manifest ACTION_SEND text entry, no browsable deep-link surface.
replace_once(
    'app-android/src/main/AndroidManifest.xml',
    '''            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
''',
    '''            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
'''
)

# 7) Home wiring: one compact import entry and dialog lifecycle.
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    'import com.agentickitchen.android.KitchenScanState\n',
    'import com.agentickitchen.android.KitchenScanState\nimport com.agentickitchen.android.RecipeImportState\nimport com.agentickitchen.shared.ai.ImportedRecipe\n'
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''    shoppingImportState: ShoppingImportState = ShoppingImportState.Idle,
    shoppingList: List<ShoppingListItem> = emptyList(),
''',
    '''    shoppingImportState: ShoppingImportState = ShoppingImportState.Idle,
    recipeImportState: RecipeImportState = RecipeImportState.Idle,
    shoppingList: List<ShoppingListItem> = emptyList(),
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''    onClearShoppingImport: () -> Unit = {},
    onToggleShoppingItem: (String, Boolean) -> Unit = { _, _ -> },
''',
    '''    onClearShoppingImport: () -> Unit = {},
    onImportRecipeText: (String) -> Unit = {},
    onImportRecipeUrl: (String) -> Unit = {},
    onImportRecipePhoto: (Bitmap) -> Unit = {},
    onPrepareImportedRecipe: (ImportedRecipe) -> Unit = {},
    onClearRecipeImport: () -> Unit = {},
    onToggleShoppingItem: (String, Boolean) -> Unit = { _, _ -> },
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''    var showShoppingImport by remember { mutableStateOf(false) }
    var showKitchenScan by remember { mutableStateOf(false) }
''',
    '''    var showShoppingImport by remember { mutableStateOf(false) }
    var showRecipeImport by remember { mutableStateOf(false) }
    var showKitchenScan by remember { mutableStateOf(false) }
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''    LaunchedEffect(filteredIngredients) {
        expandedAuto = filteredIngredients.isNotEmpty()
    }
''',
    '''    LaunchedEffect(filteredIngredients) {
        expandedAuto = filteredIngredients.isNotEmpty()
    }
    LaunchedEffect(recipeImportState) {
        if (recipeImportState !is RecipeImportState.Idle) showRecipeImport = true
    }
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''        if (inventory.isEmpty()) {
''',
    '''        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedButton(
                onClick = { showRecipeImport = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                border = BorderStroke(1.dp, colors.divider),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Tarif içe aktar · URL / metin / fotoğraf" else "Import recipe · URL / text / photo", color = colors.primary)
            }
        }
        if (inventory.isEmpty()) {
'''
)
replace_once(
    'app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt',
    '''    if (showInventoryRecipe) {
        InventoryRecipeDialog(
            onDismiss = { showInventoryRecipe = false },
            onStart = {
                showInventoryRecipe = false
                onStartInventorySession(it)
            }
        )
    }
}
''',
    '''    if (showInventoryRecipe) {
        InventoryRecipeDialog(
            onDismiss = { showInventoryRecipe = false },
            onStart = {
                showInventoryRecipe = false
                onStartInventorySession(it)
            }
        )
    }

    if (showRecipeImport) {
        RecipeImportDialog(
            state = recipeImportState,
            inventory = inventory,
            onDismiss = {
                showRecipeImport = false
                onClearRecipeImport()
            },
            onImportUrl = onImportRecipeUrl,
            onImportText = onImportRecipeText,
            onImportPhoto = onImportRecipePhoto,
            onPrepare = onPrepareImportedRecipe,
            onConfigureGemini = onConfigureGemini
        )
    }
}
'''
)

# 8) Dedicated recipe import review UI; preserve editorial visual language and novice readability.
Path('app-android/src/main/java/com/agentickitchen/android/ui/RecipeImportDialog.kt').write_text(r'''package com.agentickitchen.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.L
import com.agentickitchen.android.RecipeImportState
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.RecipeImportAvailability
import com.agentickitchen.shared.inventory.RecipeImportDraftPolicy
import com.agentickitchen.shared.inventory.RecipeImportPantryPlanner
import java.math.BigDecimal

@Composable
fun RecipeImportDialog(
    state: RecipeImportState,
    inventory: List<PantryStockItem>,
    onDismiss: () -> Unit,
    onImportUrl: (String) -> Unit,
    onImportText: (String) -> Unit,
    onImportPhoto: (Bitmap) -> Unit,
    onPrepare: (ImportedRecipe) -> Unit,
    onConfigureGemini: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let(onImportPhoto)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
    }
    val importing = state is RecipeImportState.Loading

    Dialog(
        onDismissRequest = { if (!importing) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !importing,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            backgroundColor = colors.surface,
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    if (L.isTr) "Tarif içe aktar" else "Import a recipe",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h5
                )
                Text(
                    if (L.isTr) "Bağlantı, metin, ekran görüntüsü veya Android Paylaş menüsünden tarif al." else "Bring in a recipe from a link, text, screenshot, or Android Share menu.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2
                )
                Spacer(Modifier.height(16.dp))

                when (state) {
                    RecipeImportState.Idle, is RecipeImportState.Error -> {
                        if (state is RecipeImportState.Error) {
                            Text(state.message, color = colors.error, style = MaterialTheme.typography.body2)
                            Spacer(Modifier.height(10.dp))
                        }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(if (L.isTr) "Tarif bağlantısı" else "Recipe URL") },
                            placeholder = { Text("https://…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onImportUrl(url) },
                            enabled = url.trim().startsWith("http://") || url.trim().startsWith("https://"),
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(if (L.isTr) "Bağlantıdan oku" else "Read from URL", color = colors.onPrimary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Divider(color = colors.divider)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text(if (L.isTr) "Tarif metni" else "Recipe text") },
                            placeholder = { Text(if (L.isTr) "Başlık, malzemeler ve yapılışı yapıştır…" else "Paste title, ingredients, and instructions…") },
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onImportText(pastedText) },
                            enabled = pastedText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, colors.primary),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "Metni çözümle" else "Parse text", color = colors.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    camera.launch(null)
                                } else {
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, colors.divider),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "Tarif fotoğrafını tara" else "Scan recipe photo", color = colors.primary)
                        }
                        if (state is RecipeImportState.Error) {
                            TextButton(onClick = onConfigureGemini, modifier = Modifier.align(Alignment.End)) {
                                Text(if (L.isTr) "AI ayarları" else "AI settings", color = colors.primary)
                            }
                        }
                    }

                    is RecipeImportState.Loading -> {
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = colors.primary)
                            Text(
                                when (state.source) {
                                    "prepare" -> if (L.isTr) "Tarifi güvenli pişirme planına dönüştürüyorum…" else "Turning the recipe into a validated cooking plan…"
                                    "url" -> if (L.isTr) "Tarif sayfasını okuyorum…" else "Reading the recipe page…"
                                    "photo" -> if (L.isTr) "Tarif fotoğrafını okuyorum…" else "Reading the recipe photo…"
                                    else -> if (L.isTr) "Tarifi okuyorum…" else "Reading the recipe…"
                                },
                                color = colors.onSurface
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    is RecipeImportState.Review -> RecipeImportReview(
                        state = state,
                        inventory = inventory,
                        onPrepare = onPrepare
                    )
                }

                if (!importing) {
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(if (L.isTr) "Kapat" else "Close", color = colors.onSurfaceSub)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeImportReview(
    state: RecipeImportState.Review,
    inventory: List<PantryStockItem>,
    onPrepare: (ImportedRecipe) -> Unit
) {
    val colors = LocalAppColors.current
    val source = state.response.recipe
    var name by remember(source) { mutableStateOf(source.name) }
    var servingsText by remember(source) { mutableStateOf(source.servings?.toString().orEmpty()) }
    var ingredients by remember(source) { mutableStateOf(source.ingredients) }
    var instructions by remember(source) { mutableStateOf(source.instructions) }

    val draft = source.copy(
        name = name.trim(),
        servings = servingsText.toIntOrNull()?.takeIf { it > 0 },
        ingredients = ingredients,
        instructions = instructions
    )
    val pantry = RecipeImportPantryPlanner.compare(draft, inventory)
    val issues = RecipeImportDraftPolicy.issues(draft)

    Text(
        if (L.isTr) "Önizleme ve kontrol" else "Preview and review",
        color = colors.primary,
        style = MaterialTheme.typography.overline
    )
    source.sourceLabel?.takeIf(String::isNotBlank)?.let {
        Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
    }
    state.response.uncertainty?.let {
        Text(it, color = colors.warning, style = MaterialTheme.typography.caption)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(if (L.isTr) "Tarif adı" else "Recipe name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = servingsText,
        onValueChange = { servingsText = it.filter(Char::isDigit).take(3) },
        label = { Text(if (L.isTr) "Porsiyon" else "Servings") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(14.dp))
    Divider(color = colors.divider)
    Spacer(Modifier.height(12.dp))
    Text(if (L.isTr) "Malzemeler" else "Ingredients", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    ingredients.forEachIndexed { index, ingredient ->
        val match = pantry.matches.getOrNull(index)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ingredient.displayName,
            onValueChange = { value ->
                ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(displayName = value) else item }
            },
            label = { Text(if (L.isTr) "Malzeme ${index + 1}" else "Ingredient ${index + 1}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ingredient.quantity?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
                onValueChange = { raw ->
                    val parsed = raw.replace(',', '.').toDoubleOrNull()
                    ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(quantity = parsed) else item }
                },
                label = { Text(if (L.isTr) "Miktar" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ingredient.unit.orEmpty(),
                onValueChange = { value ->
                    ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(unit = value.ifBlank { null }) else item }
                },
                label = { Text(if (L.isTr) "Birim" else "Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            importAvailabilityLabel(match?.availability ?: RecipeImportAvailability.NEEDS_REVIEW),
            color = when (match?.availability) {
                RecipeImportAvailability.AVAILABLE -> colors.success
                RecipeImportAvailability.PARTIAL -> colors.warning
                RecipeImportAvailability.MISSING -> colors.error
                else -> colors.onSurfaceSub
            },
            style = MaterialTheme.typography.caption
        )
    }
    Spacer(Modifier.height(14.dp))
    Divider(color = colors.divider)
    Spacer(Modifier.height(12.dp))
    Text(if (L.isTr) "Adımlar" else "Instructions", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    instructions.forEachIndexed { index, instruction ->
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = instruction,
            onValueChange = { value ->
                instructions = instructions.mapIndexed { i, item -> if (i == index) value else item }
            },
            label = { Text(if (L.isTr) "Adım ${index + 1}" else "Step ${index + 1}") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        if (L.isTr) {
            "Stok: ${pantry.availableCount} hazır · ${pantry.partialCount} kısmi · ${pantry.missingCount} eksik · ${pantry.needsReviewCount} kontrol"
        } else {
            "Pantry: ${pantry.availableCount} ready · ${pantry.partialCount} partial · ${pantry.missingCount} missing · ${pantry.needsReviewCount} review"
        },
        color = colors.onSurfaceSub,
        style = MaterialTheme.typography.body2
    )
    if (issues.isNotEmpty()) {
        Text(
            if (L.isTr) "Eksik veya anlaşılmayan alanları düzeltmeden pişirme planı oluşturulmaz." else "Complete unclear or missing fields before creating a cooking plan.",
            color = colors.warning,
            style = MaterialTheme.typography.caption
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onPrepare(draft) },
        enabled = issues.isEmpty(),
        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
    ) {
        Text(if (L.isTr) "Pişirme planını hazırla" else "Prepare cooking plan", color = colors.onPrimary)
    }
}

private fun importAvailabilityLabel(value: RecipeImportAvailability): String = when (value) {
    RecipeImportAvailability.AVAILABLE -> if (L.isTr) "Stokta yeterli" else "Enough in pantry"
    RecipeImportAvailability.PARTIAL -> if (L.isTr) "Stokta kısmen var" else "Partially available"
    RecipeImportAvailability.MISSING -> if (L.isTr) "Eksik · alışveriş/değişiklik gerekebilir" else "Missing · shopping/substitution may be needed"
    RecipeImportAvailability.NEEDS_REVIEW -> if (L.isTr) "Miktar veya birim kontrol edilmeli" else "Amount or unit needs review"
}
''', encoding='utf-8')

# 9) Focused unit tests for imported-measurement policy and share intake.
Path('shared/src/test/kotlin/com/agentickitchen/shared/inventory/RecipeImportDraftPolicyTest.kt').write_text(r'''package com.agentickitchen.shared.inventory

import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeImportDraftPolicyTest {
    @Test fun culinaryVolumeUnitsNormalizeDeterministically() {
        assertEquals(240.0, InventoryUnits.normalize(1.0, "cup").quantity)
        assertEquals(30.0, InventoryUnits.normalize(2.0, "tbsp").quantity)
        assertEquals(5.0, InventoryUnits.normalize(1.0, "tsp").quantity)
        assertEquals(UnitDimension.VOLUME, InventoryUnits.normalize(1.0, "cup").dimension)
    }

    @Test fun completeImportedRecipeCanPrepare() {
        val recipe = ImportedRecipe(
            name = "Soup",
            servings = 2,
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "cup")),
            instructions = listOf("Warm the milk.")
        )
        assertTrue(RecipeImportDraftPolicy.canPrepare(recipe))
        assertTrue(RecipeImportDraftPolicy.issues(recipe).isEmpty())
    }

    @Test fun missingServingsOrUnknownUnitFailsClosed() {
        val recipe = ImportedRecipe(
            name = "Soup",
            servings = null,
            ingredients = listOf(ImportedRecipeIngredient("Milk", 1.0, "ladle")),
            instructions = listOf("Warm the milk.")
        )
        val issues = RecipeImportDraftPolicy.issues(recipe)
        assertFalse(RecipeImportDraftPolicy.canPrepare(recipe))
        assertTrue("servings_missing" in issues)
        assertTrue(issues.any { it.startsWith("ingredient_unit_") })
    }
}
''', encoding='utf-8')

Path('app-android/src/test/java/com/agentickitchen/android/RecipeSharePayloadTest.kt').write_text(r'''package com.agentickitchen.android

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeSharePayloadTest {
    @Test fun acceptsOnlyNonBlankTextSendPayloads() {
        assertEquals(
            "https://example.com/recipe",
            recipeSharePayload(Intent.ACTION_SEND, "text/plain", "  https://example.com/recipe  ")
        )
        assertNull(recipeSharePayload(Intent.ACTION_VIEW, "text/plain", "https://example.com/recipe"))
        assertNull(recipeSharePayload(Intent.ACTION_SEND, "image/jpeg", "https://example.com/recipe"))
        assertNull(recipeSharePayload(Intent.ACTION_SEND, "text/plain", "   "))
    }
}
''', encoding='utf-8')
