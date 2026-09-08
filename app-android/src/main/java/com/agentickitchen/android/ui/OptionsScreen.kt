package com.agentickitchen.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.android.RecipeRequestSelection
import com.agentickitchen.shared.inventory.RecipeMatchTier
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.scheduler.TargetTimeChoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.util.Locale

@Composable
fun OptionsScreen(
    chips: List<String>,
    planState: PlanState,
    pantryIntel: PantryIntelReport,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onUseOffline: () -> Unit,
    onSelectOption: (RecipeOption, RecipeRequestSelection) -> Unit,
    onBackToOptions: () -> Unit
) {
    var selectedOption by remember { mutableStateOf<RecipeOption?>(null) }
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        RecipeCandidatesHeader(
            optionCount = (planState as? PlanState.OptionsReady)?.options?.size,
            pantryIntel = pantryIntel
        )
        Spacer(Modifier.height(18.dp))

        when (planState) {
            is PlanState.Idle -> RecipeCandidatesEmpty(chips = chips, onStart = onStart)
            is PlanState.Loading -> RecipeCandidatesLoading()

            is PlanState.OptionsReady -> ThemedRecipeCandidateList(
                options = planState.options,
                onRefresh = onRefresh,
                onSelect = { selectedOption = it }
            )

            is PlanState.RecipeActive -> RecipeActiveSummary(planState.recipe, onBackToOptions)

            is PlanState.Error -> RecipeCandidatesError(
                message = planState.message,
                canUseOffline = planState.canUseOffline,
                onRetry = onStart,
                onUseOffline = onUseOffline
            )
        }
        Spacer(Modifier.height(18.dp))
    }

    selectedOption?.let { recipe ->
        RecipeDetailOverlay(
            recipe = recipe,
            onDismiss = { selectedOption = null },
            onConfirm = { selection ->
                onSelectOption(recipe, selection)
                selectedOption = null
            }
        )
    }
}

@Composable
private fun RecipeCandidatesHeader(optionCount: Int?, pantryIntel: PantryIntelReport) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EditorialBrandMark(size = 21.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "AgenticKitchen",
            color = colors.onBackground,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (optionCount != null) {
            Text(
                if (L.isTr) "$optionCount fikir" else "$optionCount ideas",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
        }
    }

    Spacer(Modifier.height(if (spec.dense) 10.dp else 14.dp))
    Text(
        if (L.isTr) "Elindekilerle" else "With what you have",
        color = colors.onBackground,
        style = MaterialTheme.typography.h1
    )
    Spacer(Modifier.height(5.dp))
    Text(
        if (L.isTr) {
            "Mutfağındaki malzemelere göre hazırlanmış tarifler."
        } else {
            "Recipes composed around what is already in your kitchen."
        },
        color = colors.onSurfaceSub,
        style = MaterialTheme.typography.body1
    )

    if (pantryIntel.readinessScore > 0) {
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) "Kiler hazırlığı ${pantryIntel.readinessScore}/100" else "Pantry readiness ${pantryIntel.readinessScore}/100",
            color = colors.primary,
            style = MaterialTheme.typography.caption
        )
    }
}

internal data class TargetTimeUiOption(
    val id: String,
    val label: String,
    val choice: TargetTimeChoice
)

internal fun targetTimePresetOptions(isTurkish: Boolean): List<TargetTimeUiOption> = listOf(
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

internal fun formatExactTimeInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(4)
    return if (digits.length <= 2) digits else "${digits.take(2)}:${digits.drop(2)}"
}

internal fun exactTargetTimeChoice(value: String): TargetTimeChoice.Exact? {
    if (!value.matches(Regex("""\d{2}:\d{2}"""))) return null
    val (hour, minute) = value.split(':').map(String::toInt)
    return runCatching { TargetTimeChoice.Exact(LocalTime.of(hour, minute)) }.getOrNull()
}

internal fun recipeRequestSelection(servings: Int, targetTime: TargetTimeChoice) =
    RecipeRequestSelection(servings = servings.coerceIn(1, 12), targetTime = targetTime)

internal fun localizedRecipeSourceLabel(sourceLabel: String?, isTurkish: Boolean): String? = when {
    sourceLabel.isNullOrBlank() -> null
    sourceLabel.equals("Offline", ignoreCase = true) -> if (isTurkish) "ÇEVRİMDIŞI" else "OFFLINE"
    else -> sourceLabel
}

internal fun recipeTypeLabel(type: String, isTurkish: Boolean): String {
    val normalized = type.trim()
    return when (normalized.lowercase(Locale.ROOT)) {
        "easy" -> if (isTurkish) "KOLAY" else "EASY"
        "medium" -> if (isTurkish) "ORTA" else "MEDIUM"
        "hard" -> if (isTurkish) "ZOR" else "HARD"
        else -> normalized.uppercase(if (isTurkish) Locale.forLanguageTag("tr-TR") else Locale.ROOT)
    }
}

internal fun recipeMatchTierLabel(tier: RecipeMatchTier, isTurkish: Boolean): String = when (tier) {
    RecipeMatchTier.READY_NOW -> if (isTurkish) "HAZIR ŞİMDİ" else "READY NOW"
    RecipeMatchTier.MISSING_ONE -> if (isTurkish) "1 EKSİK" else "MISSING 1"
    RecipeMatchTier.MISSING_TWO -> if (isTurkish) "2 EKSİK" else "MISSING 2"
    RecipeMatchTier.AI_IDEA -> if (isTurkish) "AI FİKİRLERİ" else "AI IDEAS"
}

internal fun recipeCardFacts(option: RecipeOption, isTurkish: Boolean): String = listOfNotNull(
    option.estimatedMinutes?.let { if (isTurkish) "$it dk" else "$it min" },
    if (isTurkish) "${option.servings} kişilik" else "${option.servings} servings",
    option.pantryCoveragePercent?.let { if (isTurkish) "stok %$it" else "$it% pantry" }
).joinToString(" · ")

internal fun recipeCoverageSummary(options: List<RecipeOption>, isTurkish: Boolean): String? {
    if (options.none { it.pantryCoveragePercent != null }) return null
    val ready = options.count { it.matchTier == RecipeMatchTier.READY_NOW }
    val one = options.count { it.matchTier == RecipeMatchTier.MISSING_ONE }
    val two = options.count { it.matchTier == RecipeMatchTier.MISSING_TWO }
    val ai = options.count { it.matchTier == RecipeMatchTier.AI_IDEA }
    return if (isTurkish) {
        "${options.size} sonuç · $ready hazır · $one tek eksik · $two iki eksik · $ai fikir"
    } else {
        "${options.size} results · $ready ready · $one missing one · $two missing two · $ai AI ideas"
    }
}

@Composable
private fun ThemedRecipeCandidateList(
    options: List<RecipeOption>,
    onRefresh: () -> Unit,
    onSelect: (RecipeOption) -> Unit
) {
    val colors = LocalAppColors.current
    val summary = recipeCoverageSummary(options, L.isTr)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (L.isTr) "Tarif fikirleri" else "Recipe ideas",
                color = colors.onSurface,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            summary?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
        }
        TextButton(onClick = onRefresh, modifier = Modifier.height(44.dp)) {
            Text(if (L.isTr) "Başka öneriler →" else "More ideas →", color = colors.primary)
        }
    }

    Spacer(Modifier.height(8.dp))
    options.forEachIndexed { index, option ->
        RecipeCandidateCard(
            option = option,
            index = index,
            onClick = { onSelect(option) }
        )
        if (index < options.lastIndex) Spacer(Modifier.height(if (LocalThemeSpec.current.dense) 9.dp else 12.dp))
    }
}

@Composable
private fun RecipeCandidateCard(option: RecipeOption, index: Int, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val radius = when (spec.typographyProfile) {
        TypographyProfile.MODERN_SANS -> 18.dp
        TypographyProfile.PREMIUM_CINEMATIC -> 16.dp
        TypographyProfile.APPLIANCE_CONTROL -> 12.dp
        TypographyProfile.WARM_EDITORIAL -> 22.dp
        TypographyProfile.MINIMAL_PRO -> 16.dp
    }
    val imageWidth = when (spec.typographyProfile) {
        TypographyProfile.PREMIUM_CINEMATIC, TypographyProfile.APPLIANCE_CONTROL -> 112.dp
        TypographyProfile.WARM_EDITORIAL -> 120.dp
        else -> 116.dp
    }
    val scale = if (pressed) .985f else 1f
    val statusColor = when (option.matchTier) {
        RecipeMatchTier.READY_NOW -> colors.success
        RecipeMatchTier.MISSING_ONE -> colors.warn
        RecipeMatchTier.MISSING_TWO -> colors.danger
        RecipeMatchTier.AI_IDEA -> colors.ai
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp,
        shape = RoundedCornerShape(radius)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .height(if (spec.dense) 170.dp else 184.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                IngredientArtwork(
                    option.name,
                    Modifier.fillMaxSize().padding(if (spec.dense) 10.dp else 12.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(statusColor.copy(alpha = if (spec.isLight) .13f else .20f), RoundedCornerShape(999.dp))
                        .border(1.dp, statusColor.copy(alpha = .55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        candidateStatusLabel(option, L.isTr),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = if (spec.dense) 10.dp else 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%02d".format(index + 1),
                        color = colors.primary,
                        style = MaterialTheme.typography.caption
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        recipeTypeLabel(option.type, L.isTr),
                        color = colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = .7.sp
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    option.name,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h6,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    option.description,
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2,
                    maxLines = if (spec.dense) 3 else 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    recipeCardFacts(option, L.isTr),
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (option.shortages.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (L.isTr) "Eksik: ${option.shortages.joinToString()}" else "Missing: ${option.shortages.joinToString()}",
                        color = statusColor,
                        style = MaterialTheme.typography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (spec.isLight) {
                    Text(
                        if (L.isTr) "Tarifi incele →" else "Explore recipe →",
                        color = colors.primary,
                        style = MaterialTheme.typography.button
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.primary.copy(alpha = .20f), RoundedCornerShape(999.dp))
                            .border(1.dp, colors.primary.copy(alpha = .55f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (L.isTr) "Tarifi incele →" else "Explore recipe →",
                            color = colors.primaryLight,
                            style = MaterialTheme.typography.button
                        )
                    }
                }
            }
        }
    }
}

private fun candidateStatusLabel(option: RecipeOption, isTurkish: Boolean): String = when {
    option.matchTier == RecipeMatchTier.READY_NOW && option.pantryCoveragePercent != null ->
        if (isTurkish) "%${option.pantryCoveragePercent} eşleşme" else "${option.pantryCoveragePercent}% match"
    option.matchTier == RecipeMatchTier.MISSING_ONE ->
        if (isTurkish) "1 eksik" else "1 missing"
    option.matchTier == RecipeMatchTier.MISSING_TWO ->
        if (isTurkish) "2 eksik" else "2 missing"
    option.matchTier == RecipeMatchTier.AI_IDEA ->
        if (isTurkish) "AI fikri" else "AI idea"
    else -> recipeMatchTierLabel(option.matchTier, isTurkish)
}

@Composable
private fun RecipeCandidatesEmpty(chips: List<String>, onStart: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IngredientArtwork("", Modifier.size(76.dp))
        Spacer(Modifier.height(14.dp))
        Text(if (L.isTr) "Henüz tarif yok." else "No recipes yet.", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(5.dp))
        Text(
            if (chips.isEmpty()) {
                if (L.isTr) "Önce mutfağına malzeme ekle." else "Add ingredients to your Kitchen first."
            } else {
                if (L.isTr) "Seçili malzemeler için önerileri oluştur." else "Generate ideas for your selected ingredients."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
        if (chips.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onStart) {
                Text(if (L.isTr) "Tarifleri oluştur" else "Generate ideas", color = colors.primary)
            }
        }
    }
}

@Composable
private fun RecipeCandidatesLoading() {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IngredientArtwork("", Modifier.size(82.dp))
        Spacer(Modifier.height(14.dp))
        Text(L.thinking, color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(6.dp))
        Text(if (L.isTr) "Kiler ve seçili malzemeler eşleştiriliyor." else "Matching your pantry and selected ingredients.", color = colors.onSurfaceSub)
    }
}

@Composable
private fun RecipeCandidatesError(
    message: String,
    canUseOffline: Boolean,
    onRetry: () -> Unit,
    onUseOffline: () -> Unit
) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.danger),
        elevation = 0.dp,
        shape = RoundedCornerShape(LocalThemeSpec.current.cornerRadius.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(if (L.isTr) "Tarifler hazırlanamadı." else "Recipes could not be prepared.", color = colors.danger, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(7.dp))
            Text(message, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) {
                Text(if (L.isTr) "Tekrar dene" else "Try again", color = colors.primary)
            }
            if (canUseOffline) {
                TextButton(onClick = onUseOffline) {
                    Text(if (L.isTr) "Çevrimdışı modu kullan" else "Use Offline mode", color = colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun RecipeActiveSummary(recipe: RecipeOption, onBackToOptions: () -> Unit) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp,
        shape = RoundedCornerShape(LocalThemeSpec.current.cornerRadius.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(if (L.isTr) "Bu tarif pişirmeye hazır." else "This recipe is ready to cook.", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
            Spacer(Modifier.height(6.dp))
            Text(recipe.name, color = colors.primary, style = MaterialTheme.typography.h3)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBackToOptions) {
                Text(if (L.isTr) "Başka tarif seç" else "Choose another recipe", color = colors.primary)
            }
        }
    }
}

@Composable
private fun RecipeDetailOverlay(
    recipe: RecipeOption,
    onDismiss: () -> Unit,
    onConfirm: (RecipeRequestSelection) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RecipeDetailContent(
            recipe = recipe,
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            initialTargetId = recipe.requestedTargetTime?.let(::targetTimeChoiceId) ?: "after_20"
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeDetailContent(
    recipe: RecipeOption,
    onDismiss: () -> Unit,
    onConfirm: (RecipeRequestSelection) -> Unit,
    initialTargetId: String
) {
    val colors = LocalAppColors.current
    val presets = targetTimePresetOptions(L.isTr)
    var selectedTargetId by remember(recipe.id, initialTargetId) { mutableStateOf(initialTargetId) }
    var exactTime by remember(recipe.id) { mutableStateOf("19:30") }
    var servings by remember(recipe.id) { mutableStateOf(recipe.servings.coerceIn(1, 12)) }
    val selected = presets.firstOrNull { it.id == selectedTargetId } ?: presets.first()
    val selectedChoice = if (selected.id == "exact") exactTargetTimeChoice(exactTime) else selected.choice

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (L.isTr) "Tarif ayrıntısı" else "Recipe detail",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss, modifier = Modifier.semantics {
                    contentDescription = if (L.isTr) "Tarif ayrıntısını kapat" else "Close recipe detail"
                }) {
                    Text(if (L.isTr) "Kapat" else "Close", color = colors.onSurfaceSub)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(190.dp).background(colors.surface2, RoundedCornerShape(LocalThemeSpec.current.cornerRadius.dp)),
                contentAlignment = Alignment.Center
            ) {
                IngredientArtwork(recipe.name, Modifier.size(170.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text(recipeTypeLabel(recipe.type, L.isTr), color = colors.primary, style = MaterialTheme.typography.overline)
            Spacer(Modifier.height(6.dp))
            Text(recipe.name, color = colors.onSurface, style = MaterialTheme.typography.h1)
            Spacer(Modifier.height(8.dp))
            Text(recipe.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.divider)
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Kaç kişilik?" else "How many servings?", color = colors.onSurface, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(10.dp))
            RecipeServingsSelector(
                servings = servings,
                onDecrease = { servings = (servings - 1).coerceAtLeast(1) },
                onIncrease = { servings = (servings + 1).coerceAtMost(12) }
            )

            Spacer(Modifier.height(18.dp))
            Divider(color = colors.divider)
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Ne zaman hazır olsun?" else "When should it be ready?", color = colors.onSurface, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { option ->
                    TargetTimeChoicePill(option, option.id == selectedTargetId) { selectedTargetId = option.id }
                }
            }

            if (selected.id == "exact") {
                Spacer(Modifier.height(12.dp))
                ExactTimeEditor(
                    value = exactTime,
                    onValueChange = { exactTime = formatExactTimeInput(it) },
                    valid = selectedChoice != null
                )
            }

            if (!recipe.canPrepareFromPantry) {
                Spacer(Modifier.height(16.dp))
                Text(
                    if (L.isTr) "Bu fikir için 3 veya daha fazla ürün eksik. Şimdilik yalnızca fikir olarak gösteriliyor." else "This idea is missing 3 or more items. For now it is shown as inspiration only.",
                    color = colors.danger,
                    style = MaterialTheme.typography.body2
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { selectedChoice?.let { onConfirm(recipeRequestSelection(servings, it)) } },
                enabled = selectedChoice != null && recipe.canPrepareFromPantry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colors.primary,
                    disabledBackgroundColor = colors.divider
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Tarifi Hazırla" else "Prepare Recipe", color = colors.onPrimary)
            }
        }
        Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun RecipeServingsSelector(servings: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onDecrease,
            enabled = servings > 1,
            modifier = Modifier.size(48.dp).border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .semantics { contentDescription = if (L.isTr) "Porsiyonu azalt" else "Decrease servings" }
        ) {
            Text("−", color = colors.primary, fontSize = 22.sp)
        }
        Text(
            if (L.isTr) "$servings kişi" else "$servings servings",
            color = colors.onSurface,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = onIncrease,
            enabled = servings < 12,
            modifier = Modifier.size(48.dp).border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .semantics { contentDescription = if (L.isTr) "Porsiyonu artır" else "Increase servings" }
        ) {
            Text("+", color = colors.primary, fontSize = 22.sp)
        }
    }
}

@Composable
private fun TargetTimeChoicePill(option: TargetTimeUiOption, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(if (selected) colors.primary else colors.surface2, RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) colors.primary else colors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (selected) "✓ ${option.label}" else option.label,
            color = if (selected) colors.onPrimary else colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExactTimeEditor(value: String, onValueChange: (String) -> Unit, valid: Boolean) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    Column {
        Text(if (L.isTr) "Hazır olma saati" else "Ready time", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        scope.launch { delay(120) }
                    }
                }
                .semantics { contentDescription = if (L.isTr) "Hazır olma saatini gir" else "Enter ready time" }
                .background(colors.surface2, RoundedCornerShape(12.dp))
                .border(1.dp, if (valid) colors.border else colors.danger, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(),
            singleLine = true
        )
        if (!valid) {
            Spacer(Modifier.height(5.dp))
            Text(if (L.isTr) "Saati SS:DD biçiminde gir." else "Enter time as HH:MM.", color = colors.danger, fontSize = 12.sp)
        }
    }
}
