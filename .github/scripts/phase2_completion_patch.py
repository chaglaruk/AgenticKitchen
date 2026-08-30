from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


# AppViewModel -----------------------------------------------------------------
vm_path = Path("app-android/src/main/java/com/agentickitchen/android/AppViewModel.kt")
vm = vm_path.read_text(encoding="utf-8")
vm = replace_once(
    vm,
    "import com.agentickitchen.shared.inventory.RecipeMatchCandidate\n",
    "import com.agentickitchen.shared.inventory.RecipeMatchCandidate\n"
    "import com.agentickitchen.shared.inventory.RecipeMatchConstraintPolicy\n",
    "AppViewModel constraint import",
)
vm = replace_once(
    vm,
    """data class RecipeOption(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val sourceLabel: String? = null,
    val proposedIngredients: List<com.agentickitchen.shared.ai.dto.PlannedIngredientDto> = emptyList(),
    val shortages: List<String> = emptyList(),
    val matchTier: RecipeMatchTier = RecipeMatchTier.AI_IDEA,
    val pantryCoveragePercent: Int? = null,
    val expiresTodayMatches: Int = 0,
    val useSoonMatches: Int = 0,
    val estimatedMinutes: Int? = null,
    val equipmentFit: Boolean = true,
    val previouslySuccessful: Boolean = false
)""",
    """data class RecipeOption(
    val id: String,
    val type: String,
    val name: String,
    val description: String,
    val sourceLabel: String? = null,
    val proposedIngredients: List<com.agentickitchen.shared.ai.dto.PlannedIngredientDto> = emptyList(),
    val shortages: List<String> = emptyList(),
    val matchTier: RecipeMatchTier = RecipeMatchTier.AI_IDEA,
    val pantryCoveragePercent: Int? = null,
    val expiresTodayMatches: Int = 0,
    val useSoonMatches: Int = 0,
    val estimatedMinutes: Int? = null,
    val equipmentFit: Boolean = true,
    val previouslySuccessful: Boolean = false,
    val servings: Int = 2,
    val requestedTargetTime: TargetTimeChoice? = null,
    val canPrepareFromPantry: Boolean = true
)""",
    "RecipeOption completion fields",
)
vm = replace_once(
    vm,
    """data class InventoryRecipeRequest(
    val servings: Int,
    val strictStock: Boolean,
    val maxMissingStaples: Int,
    val prioritizedIngredients: List<String>
)""",
    """data class InventoryRecipeRequest(
    val servings: Int,
    val strictStock: Boolean,
    val maxMissingStaples: Int,
    val prioritizedIngredients: List<String>,
    val targetTime: TargetTimeChoice = TargetTimeChoice.Flexible
)""",
    "InventoryRecipeRequest target time",
)
vm = replace_once(
    vm,
    """    private fun requestRecipeOptions(
        ingredients: List<String>,
        inventoryRequest: InventoryRecipeRequest?
    ) {
""",
    """    private fun requestedReadyMinutes(choice: TargetTimeChoice): Int? {
        if (choice == TargetTimeChoice.Flexible) return null
        val now = ZonedDateTime.now()
        val target = targetTimeResolver.resolve(choice, now.toInstant()).getOrNull() ?: return null
        return java.time.Duration.between(now, target)
            .toMinutes()
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun requestRecipeOptions(
        ingredients: List<String>,
        inventoryRequest: InventoryRecipeRequest?
    ) {
""",
    "ready-time helper",
)
vm = replace_once(
    vm,
    """                                    safetyAllowed = true,
                                    dietAllowed = true,
""",
    """                                    safetyAllowed = RecipeMatchConstraintPolicy.safetyAllowed(
                                        dto.proposedIngredients,
                                        dietSettings.value.allergies
                                    ),
                                    dietAllowed = RecipeMatchConstraintPolicy.dietAllowed(
                                        dto.proposedIngredients,
                                        dietSettings.value.dietType
                                    ),
""",
    "local option constraints",
)
vm = replace_once(
    vm,
    """                            availableEquipment = _selectedEquipment.value,
                            prioritizedIngredients = inventoryRequest.prioritizedIngredients
                        )
""",
    """                            availableEquipment = _selectedEquipment.value,
                            prioritizedIngredients = inventoryRequest.prioritizedIngredients,
                            requestedReadyMinutes = requestedReadyMinutes(inventoryRequest.targetTime)
                        )
""",
    "ready-time matcher input",
)
vm = replace_once(
    vm,
    """                        val allowed = ranked.filter { match ->
                            if (inventoryRequest.strictStock) {
                                match.tier == RecipeMatchTier.READY_NOW
                            } else {
                                when (match.tier) {
                                    RecipeMatchTier.READY_NOW -> true
                                    RecipeMatchTier.MISSING_ONE -> inventoryRequest.maxMissingStaples >= 1
                                    RecipeMatchTier.MISSING_TWO -> inventoryRequest.maxMissingStaples >= 2
                                    RecipeMatchTier.AI_IDEA -> false
                                }
                            }
                        }
""",
    """                        val allowed = ranked.filter { match ->
                            RecipeMatcher.shouldSurface(
                                result = match,
                                strictStock = inventoryRequest.strictStock,
                                maxMissingStaples = inventoryRequest.maxMissingStaples
                            )
                        }
""",
    "surface policy",
)
vm = replace_once(
    vm,
    """                                    equipmentFit = match.equipmentFit,
                                    previouslySuccessful = match.previouslySuccessful
""",
    """                                    equipmentFit = match.equipmentFit,
                                    previouslySuccessful = match.previouslySuccessful,
                                    servings = inventoryRequest.servings,
                                    requestedTargetTime = inventoryRequest.targetTime,
                                    canPrepareFromPantry = RecipeMatcher.canPrepareFromPantry(match)
""",
    "mapped option completion fields",
)
vm_path.write_text(vm, encoding="utf-8")


# HomeScreen -------------------------------------------------------------------
home_path = Path("app-android/src/main/java/com/agentickitchen/android/ui/HomeScreen.kt")
home = home_path.read_text(encoding="utf-8")
home = replace_once(
    home,
    "import com.agentickitchen.shared.inventory.ShoppingImportMode\n",
    "import com.agentickitchen.shared.inventory.ShoppingImportMode\n"
    "import com.agentickitchen.shared.scheduler.TargetTimeChoice\n",
    "Home target-time import",
)
home = replace_once(
    home,
    """    var strictStock by remember { mutableStateOf(false) }
    var missingStaples by remember { mutableStateOf(2) }
    var priority by remember { mutableStateOf("") }
""",
    """    var strictStock by remember { mutableStateOf(false) }
    var missingStaples by remember { mutableStateOf(2) }
    var priority by remember { mutableStateOf("") }
    var targetTimeId by remember { mutableStateOf("flexible") }
    val targetTimeOptions = targetTimePresetOptions(L.isTr).filter { it.id != "exact" }
    val targetTime = targetTimeOptions.firstOrNull { it.id == targetTimeId }?.choice ?: TargetTimeChoice.Flexible
""",
    "Home recipe target state",
)
home = replace_once(
    home,
    """            OutlinedTextField(
                value = priority,
""",
    """            Spacer(Modifier.height(8.dp))
            Text(
                if (L.isTr) "Hazır olma hedefi" else "Ready-time target",
                color = colors.onSurface,
                style = MaterialTheme.typography.body2
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                targetTimeOptions.forEach { option ->
                    val selected = option.id == targetTimeId
                    TextButton(
                        onClick = { targetTimeId = option.id },
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .border(
                                1.dp,
                                if (selected) colors.primary else colors.divider,
                                RoundedCornerShape(999.dp)
                            )
                    ) {
                        Text(
                            if (selected) "✓ ${option.label}" else option.label,
                            color = if (selected) colors.primary else colors.onSurfaceSub
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = priority,
""",
    "Home ready-time controls",
)
home = replace_once(
    home,
    """            Text(
                if (L.isTr) "Hazır olma saatini tarif seçerken belirleyeceksin." else "You will choose the ready time after selecting a recipe.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 12.dp)
            )
""",
    """            Text(
                if (L.isTr) "Tarifler stok durumuna ve bu hazır olma hedefine göre sıralanır." else "Recipes are ranked against your pantry and this ready-time target.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 12.dp)
            )
""",
    "Home ready-time explanation",
)
home = replace_once(
    home,
    """                        InventoryRecipeRequest(
                            servings,
                            strictStock,
                            if (strictStock) 0 else missingStaples,
                            priority.split(',').map(String::trim).filter(String::isNotEmpty)
                        )
""",
    """                        InventoryRecipeRequest(
                            servings = servings,
                            strictStock = strictStock,
                            maxMissingStaples = if (strictStock) 0 else missingStaples,
                            prioritizedIngredients = priority.split(',').map(String::trim).filter(String::isNotEmpty),
                            targetTime = targetTime
                        )
""",
    "Home request payload",
)
home_path.write_text(home, encoding="utf-8")


# OptionsScreen ----------------------------------------------------------------
options_path = Path("app-android/src/main/java/com/agentickitchen/android/ui/OptionsScreen.kt")
options = options_path.read_text(encoding="utf-8")
options = replace_once(
    options,
    """internal fun targetTimePresetOptions(isTurkish: Boolean): List<TargetTimeUiOption> = listOf(
    TargetTimeUiOption("after_20", if (isTurkish) "20 dakika" else "20 minutes", TargetTimeChoice.After(Duration.ofMinutes(20))),
    TargetTimeUiOption("after_45", if (isTurkish) "45 dakika" else "45 minutes", TargetTimeChoice.After(Duration.ofMinutes(45))),
    TargetTimeUiOption("after_60", if (isTurkish) "1 saat" else "1 hour", TargetTimeChoice.After(Duration.ofHours(1))),
    TargetTimeUiOption("evening", if (isTurkish) "Bu akşam" else "This evening", TargetTimeChoice.ThisEvening),
    TargetTimeUiOption("flexible", if (isTurkish) "Farketmez" else "Flexible", TargetTimeChoice.Flexible),
    TargetTimeUiOption("exact", if (isTurkish) "Saat seç" else "Choose time", TargetTimeChoice.Exact(LocalTime.of(19, 30)))
)
""",
    """internal fun targetTimePresetOptions(isTurkish: Boolean): List<TargetTimeUiOption> = listOf(
    TargetTimeUiOption("after_20", if (isTurkish) "20 dakika" else "20 minutes", TargetTimeChoice.After(Duration.ofMinutes(20))),
    TargetTimeUiOption("after_45", if (isTurkish) "45 dakika" else "45 minutes", TargetTimeChoice.After(Duration.ofMinutes(45))),
    TargetTimeUiOption("after_60", if (isTurkish) "1 saat" else "1 hour", TargetTimeChoice.After(Duration.ofHours(1))),
    TargetTimeUiOption("evening", if (isTurkish) "Bu akşam" else "This evening", TargetTimeChoice.ThisEvening),
    TargetTimeUiOption("flexible", if (isTurkish) "Farketmez" else "Flexible", TargetTimeChoice.Flexible),
    TargetTimeUiOption("exact", if (isTurkish) "Saat seç" else "Choose time", TargetTimeChoice.Exact(LocalTime.of(19, 30)))
)

internal fun targetTimeChoiceId(choice: TargetTimeChoice): String = when (choice) {
    is TargetTimeChoice.After -> when (choice.duration.toMinutes()) {
        20L -> "after_20"
        45L -> "after_45"
        60L -> "after_60"
        else -> "flexible"
    }
    is TargetTimeChoice.Exact -> "exact"
    TargetTimeChoice.ThisEvening -> "evening"
    TargetTimeChoice.Flexible -> "flexible"
}
""",
    "target-time choice id",
)
options = replace_once(
    options,
    """        EditorialRecipeDetailContent(recipe = recipe, onDismiss = onDismiss, onConfirm = onConfirm)
""",
    """        EditorialRecipeDetailContent(
            recipe = recipe,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            initialTargetId = recipe.requestedTargetTime?.let(::targetTimeChoiceId) ?: "after_20"
        )
""",
    "detail requested target default",
)
options = replace_once(
    options,
    """    var servings by remember(recipe.id) { mutableStateOf(2) }
""",
    """    var servings by remember(recipe.id) { mutableStateOf(recipe.servings.coerceIn(1, 12)) }
""",
    "detail requested servings default",
)
options = replace_once(
    options,
    """                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { selectedChoice?.let { onConfirm(recipeRequestSelection(servings, it)) } },
                        enabled = selectedChoice != null,
""",
    """                    Spacer(Modifier.height(32.dp))
                    if (!recipe.canPrepareFromPantry) {
                        Text(
                            if (L.isTr) "Bu fikir için 3 veya daha fazla ürün eksik. Şimdilik yalnızca fikir olarak gösteriliyor." else "This idea is missing 3 or more items. For now it is shown as inspiration only.",
                            color = Color(0xFF9B3F32),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Button(
                        onClick = { selectedChoice?.let { onConfirm(recipeRequestSelection(servings, it)) } },
                        enabled = selectedChoice != null && recipe.canPrepareFromPantry,
""",
    "AI idea prepare guard",
)
options = replace_once(
    options,
    """    val two = options.count { it.matchTier == RecipeMatchTier.MISSING_TWO }
    return if (isTurkish) {
        "${options.size} sonuç · $ready hazır · $one tek eksik · $two iki eksik"
    } else {
        "${options.size} results · $ready ready · $one missing one · $two missing two"
    }
""",
    """    val two = options.count { it.matchTier == RecipeMatchTier.MISSING_TWO }
    val ai = options.count { it.matchTier == RecipeMatchTier.AI_IDEA }
    return if (isTurkish) {
        "${options.size} sonuç · $ready hazır · $one tek eksik · $two iki eksik · $ai fikir"
    } else {
        "${options.size} results · $ready ready · $one missing one · $two missing two · $ai AI ideas"
    }
""",
    "coverage summary AI count",
)
options = replace_once(
    options,
    """internal fun recipeCoverageSummary(options: List<RecipeOption>, isTurkish: Boolean): String? {
""",
    """internal fun recipeCardFacts(option: RecipeOption, isTurkish: Boolean): String = listOfNotNull(
    option.estimatedMinutes?.let { if (isTurkish) "$it dk" else "$it min" },
    if (isTurkish) "${option.servings} kişilik" else "${option.servings} servings",
    option.pantryCoveragePercent?.let { if (isTurkish) "stok %$it" else "$it% pantry" }
).joinToString(" · ")

internal fun recipeCoverageSummary(options: List<RecipeOption>, isTurkish: Boolean): String? {
""",
    "recipe card facts helper",
)
old_metadata = """                Text(option.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
            option.pantryCoveragePercent?.let { coverage ->
                Spacer(Modifier.height(8.dp))
                Text(
                    if (L.isTr) "Stok eşleşmesi %$coverage" else "$coverage% pantry match",
                    color = colors.primary,
                    style = MaterialTheme.typography.caption
                )
                if (option.expiresTodayMatches > 0 || option.useSoonMatches > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (L.isTr) {
                            listOfNotNull(
                                option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it bugün kullanılmalı" },
                                option.useSoonMatches.takeIf { it > 0 }?.let { "$it yakında kullanılmalı" }
                            ).joinToString(" · ")
                        } else {
                            listOfNotNull(
                                option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it expires today" },
                                option.useSoonMatches.takeIf { it > 0 }?.let { "$it use soon" }
                            ).joinToString(" · ")
                        },
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
"""
new_metadata = """                Text(option.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(8.dp))
                Text(
                    recipeCardFacts(option, L.isTr),
                    color = colors.primary,
                    style = MaterialTheme.typography.caption
                )
                if (option.expiresTodayMatches > 0 || option.useSoonMatches > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (L.isTr) {
                            listOfNotNull(
                                option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it bugün kullanılmalı" },
                                option.useSoonMatches.takeIf { it > 0 }?.let { "$it yakında kullanılmalı" }
                            ).joinToString(" · ")
                        } else {
                            listOfNotNull(
                                option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it expires today" },
                                option.useSoonMatches.takeIf { it > 0 }?.let { "$it use soon" }
                            ).joinToString(" · ")
                        },
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.caption
                    )
                }
"""
options = replace_once(options, old_metadata, new_metadata, "recipe card metadata")
options_path.write_text(options, encoding="utf-8")
