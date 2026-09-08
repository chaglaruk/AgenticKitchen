package com.agentickitchen.android.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L
import com.agentickitchen.android.PendingConsumption
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.android.RecipeRequestSelection
import com.agentickitchen.android.SubstitutionState
import com.agentickitchen.android.canStartPreparedCooking
import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.RecipeMatchTier
import com.agentickitchen.shared.models.PantryIntelReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ReferenceOptionsScreen(
    chips: List<String>,
    planState: PlanState,
    pantryIntel: PantryIntelReport,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onUseOffline: () -> Unit,
    onSelectOption: (RecipeOption, RecipeRequestSelection) -> Unit,
    onBackToOptions: () -> Unit
) {
    if (planState !is PlanState.OptionsReady) {
        OptionsScreen(
            chips = chips,
            planState = planState,
            pantryIntel = pantryIntel,
            onStart = onStart,
            onRefresh = onRefresh,
            onUseOffline = onUseOffline,
            onSelectOption = onSelectOption,
            onBackToOptions = onBackToOptions
        )
        return
    }

    var selectedOption by remember(planState.options) { mutableStateOf<RecipeOption?>(null) }
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val summary = recipeCoverageSummary(planState.options, L.isTr)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (spec.dense) 16.dp else 18.dp, vertical = 14.dp)
    ) {
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
            Text(
                if (L.isTr) "${planState.options.size} fikir" else "${planState.options.size} ideas",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
        }

        Spacer(Modifier.height(if (spec.dense) 10.dp else 14.dp))
        Text(if (L.isTr) "Elindekilerle" else "With what you have", color = colors.onBackground, style = MaterialTheme.typography.h1)
        Spacer(Modifier.height(5.dp))
        Text(
            if (L.isTr) "Mutfağındaki malzemelere göre hazırlanmış tarifler."
            else "Recipes composed around what is already in your kitchen.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        if (pantryIntel.readinessScore > 0) {
            Spacer(Modifier.height(9.dp))
            Text(
                if (L.isTr) "Kiler hazırlığı ${pantryIntel.readinessScore}/100"
                else "Pantry readiness ${pantryIntel.readinessScore}/100",
                color = colors.primary,
                style = MaterialTheme.typography.caption
            )
        }

        Spacer(Modifier.height(18.dp))
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
        planState.options.forEachIndexed { index, option ->
            ReferenceRecipeCandidateCard(option = option, index = index) { selectedOption = option }
            if (index < planState.options.lastIndex) Spacer(Modifier.height(if (spec.dense) 9.dp else 12.dp))
        }
        Spacer(Modifier.height(18.dp))
    }

    selectedOption?.let { recipe ->
        ReferenceRecipeDetailDialog(
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
private fun ReferenceRecipeCandidateCard(option: RecipeOption, index: Int, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val statusColor = when (option.matchTier) {
        RecipeMatchTier.READY_NOW -> colors.success
        RecipeMatchTier.MISSING_ONE -> colors.warn
        RecipeMatchTier.MISSING_TWO -> colors.danger
        RecipeMatchTier.AI_IDEA -> colors.ai
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        backgroundColor = colors.surface,
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.border),
        shape = RoundedCornerShape(referenceRadius(spec))
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(if (spec.dense) 106.dp else 116.dp)
                    .height(if (spec.dense) 166.dp else 182.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                IngredientArtwork(option.name, Modifier.fillMaxSize().padding(if (spec.dense) 10.dp else 12.dp))
                Text(
                    "%02d".format(index + 1),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = if (spec.dense) 10.dp else 12.dp)
            ) {
                Text(
                    listOfNotNull(recipeTypeLabel(option.type, L.isTr), localizedRecipeSourceLabel(option.sourceLabel, L.isTr)).joinToString(" · "),
                    color = colors.primary,
                    style = MaterialTheme.typography.overline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(option.name, color = colors.onSurface, style = MaterialTheme.typography.h6, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(option.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text(recipeCardFacts(option, L.isTr), color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(5.dp))
                Text(referenceCandidateStatus(option), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (option.expiresTodayMatches > 0 || option.useSoonMatches > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(referenceFreshnessLabel(option), color = colors.warn, style = MaterialTheme.typography.caption)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (L.isTr) "Tarifi keşfet →" else "Explore recipe →", color = colors.primary, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferenceRecipeDetailDialog(recipe: RecipeOption, onDismiss: () -> Unit, onConfirm: (RecipeRequestSelection) -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val colors = LocalAppColors.current
        val spec = LocalThemeSpec.current
        val presets = targetTimePresetOptions(L.isTr)
        var selectedTargetId by remember(recipe.id) { mutableStateOf(recipe.requestedTargetTime?.let(::targetTimeChoiceId) ?: "after_20") }
        var exactTime by remember(recipe.id) { mutableStateOf("19:30") }
        var servings by remember(recipe.id) { mutableStateOf(recipe.servings.coerceIn(1, 12)) }
        val exactRequester = remember { BringIntoViewRequester() }
        val coroutineScope = rememberCoroutineScope()
        val selected = presets.firstOrNull { it.id == selectedTargetId } ?: presets.first()
        val selectedChoice = if (selected.id == "exact") exactTargetTimeChoice(exactTime) else selected.choice

        LaunchedEffect(selectedTargetId, exactTime) {
            if (selectedTargetId == "exact") {
                delay(180)
                exactRequester.bringIntoView()
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(colors.background).imePadding()) {
            Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = if (L.isTr) "Tarif ayrıntısını kapat" else "Close recipe detail" }) {
                    Text("×", color = colors.onSurface, fontSize = 24.sp)
                }
                EditorialBrandMark(size = 20.dp)
                Spacer(Modifier.width(7.dp))
                Text("AgenticKitchen", color = colors.onSurface, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (L.isTr) "Tarif ayrıntısı" else "Recipe detail", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = if (spec.dense) 16.dp else 18.dp)
            ) {
                ReferenceRecipeHero(recipe)
                Spacer(Modifier.height(12.dp))
                ReferenceFactGrid(recipe)
                Spacer(Modifier.height(12.dp))
                ReferenceDetailSection(
                    title = if (L.isTr) "Ekipman" else "Equipment",
                    summary = if (recipe.equipmentFit) {
                        if (L.isTr) "Seçili mutfak kurulumunla uyumlu" else "Fits your selected kitchen setup"
                    } else {
                        if (L.isTr) "Pişirmeden önce ekipmanı kontrol et" else "Check required equipment before cooking"
                    },
                    accent = if (recipe.equipmentFit) colors.success else colors.warn
                )
                Spacer(Modifier.height(9.dp))
                val ingredientSummary = if (L.isTr) "${recipe.proposedIngredients.size} malzeme · ${recipe.shortages.size} eksik"
                else "${recipe.proposedIngredients.size} ingredients · ${recipe.shortages.size} missing"
                ReferenceDetailSection(
                    title = if (L.isTr) "Malzemeler" else "Ingredients",
                    summary = ingredientSummary,
                    accent = if (recipe.shortages.isEmpty()) colors.success else colors.warn,
                    details = recipe.proposedIngredients.take(4).map { ingredient ->
                        "${ingredient.name} · ${referenceQuantity(ingredient.quantity)} ${localizedPlanUnit(ingredient.unit, L.isTr)}"
                    } + recipe.shortages.take(2).map { shortage -> if (L.isTr) "Eksik: $shortage" else "Missing: $shortage" }
                )
                Spacer(Modifier.height(9.dp))
                ReferenceDetailSection(
                    title = if (L.isTr) "Hazırlık adımları" else "Preparation steps",
                    summary = if (L.isTr) "Pişirme planı, tarifi hazırladığında doğrulanmış adımlarla oluşturulur."
                    else "The validated cooking plan is generated after you prepare this recipe.",
                    accent = colors.primary
                )

                Spacer(Modifier.height(18.dp))
                Divider(color = colors.divider)
                Spacer(Modifier.height(16.dp))
                Text(if (L.isTr) "Kaç kişilik?" else "How many servings?", color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(10.dp))
                ReferenceServingsSelector(servings, { servings = (servings - 1).coerceAtLeast(1) }, { servings = (servings + 1).coerceAtMost(12) })

                Spacer(Modifier.height(18.dp))
                Text(if (L.isTr) "Ne zaman hazır olsun?" else "When should it be ready?", color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { option -> ReferenceChoicePill(option.label, option.id == selectedTargetId) { selectedTargetId = option.id } }
                }

                Column(Modifier.bringIntoViewRequester(exactRequester)) {
                    if (selected.id == "exact") {
                        Spacer(Modifier.height(12.dp))
                        ReferenceExactTimeEditor(
                            value = exactTime,
                            valid = selectedChoice != null,
                            onValueChange = { exactTime = formatExactTimeInput(it) },
                            onFocus = { coroutineScope.launch { delay(180); exactRequester.bringIntoView() } }
                        )
                    }
                    if (!recipe.canPrepareFromPantry) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (L.isTr) "Bu fikir, mevcut stokla güvenli şekilde hazırlanamaz."
                            else "This idea cannot be prepared safely from the current pantry.",
                            color = colors.danger,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Divider(color = colors.divider)
            Button(
                onClick = { selectedChoice?.let { onConfirm(recipeRequestSelection(servings, it)) } },
                enabled = selectedChoice != null && recipe.canPrepareFromPantry,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, disabledBackgroundColor = colors.divider),
                shape = RoundedCornerShape(if (spec.dense) 14.dp else 999.dp)
            ) {
                Text(if (L.isTr) "Tarifi Hazırla" else "Prepare Recipe", color = colors.onPrimary)
            }
            Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ReferenceRecipeHero(recipe: RecipeOption) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceRadius(spec))) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(if (spec.dense) 150.dp else 178.dp).background(colors.surface2), contentAlignment = Alignment.Center) {
                IngredientArtwork(recipe.name, Modifier.size(if (spec.dense) 132.dp else 158.dp))
                recipe.pantryCoveragePercent?.let { coverage ->
                    Text(
                        if (L.isTr) "%$coverage stok eşleşmesi" else "$coverage% pantry match",
                        color = colors.success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).background(colors.background.copy(alpha = .88f), RoundedCornerShape(999.dp)).padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
            Column(Modifier.padding(if (spec.dense) 14.dp else 16.dp)) {
                Text(listOfNotNull(recipeTypeLabel(recipe.type, L.isTr), localizedRecipeSourceLabel(recipe.sourceLabel, L.isTr)).joinToString(" · "), color = colors.primary, style = MaterialTheme.typography.overline)
                Spacer(Modifier.height(5.dp))
                Text(recipe.name, color = colors.onSurface, style = MaterialTheme.typography.h2)
                Spacer(Modifier.height(6.dp))
                Text(recipe.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body2)
            }
        }
    }
}

@Composable
private fun ReferenceFactGrid(recipe: RecipeOption) {
    val colors = LocalAppColors.current
    val facts = listOf(
        Pair(recipe.estimatedMinutes?.let { if (L.isTr) "$it dk" else "$it min" } ?: "—", if (L.isTr) "Süre" else "Time"),
        Pair(if (L.isTr) "${recipe.servings} kişi" else "${recipe.servings} servings", if (L.isTr) "Porsiyon" else "Serves"),
        Pair(recipe.pantryCoveragePercent?.let { "%$it" } ?: "—", if (L.isTr) "Stok" else "Pantry"),
        Pair(recipe.shortages.size.toString(), if (L.isTr) "Eksik" else "Missing")
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        facts.forEach { (value, label) ->
            Card(modifier = Modifier.weight(1f), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(label, color = colors.onSurfaceSub, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun ReferenceDetailSection(title: String, summary: String, accent: Color, details: List<String> = emptyList()) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceRadius(spec))) {
        Column(Modifier.padding(if (spec.dense) 12.dp else 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(accent, RoundedCornerShape(999.dp)))
                Spacer(Modifier.width(8.dp))
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(summary, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            details.forEach { detail -> Spacer(Modifier.height(5.dp)); Text("• $detail", color = colors.onSurface, style = MaterialTheme.typography.caption) }
        }
    }
}

@Composable
private fun ReferenceServingsSelector(servings: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDecrease, enabled = servings > 1, modifier = Modifier.size(48.dp).border(1.dp, colors.border, RoundedCornerShape(12.dp)).semantics { contentDescription = if (L.isTr) "Porsiyonu azalt" else "Decrease servings" }) { Text("−", color = colors.primary, fontSize = 22.sp) }
        Text(if (L.isTr) "$servings kişi" else "$servings servings", color = colors.onSurface, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        TextButton(onClick = onIncrease, enabled = servings < 12, modifier = Modifier.size(48.dp).border(1.dp, colors.border, RoundedCornerShape(12.dp)).semantics { contentDescription = if (L.isTr) "Porsiyonu artır" else "Increase servings" }) { Text("+", color = colors.primary, fontSize = 22.sp) }
    }
}

@Composable
private fun ReferenceChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.defaultMinSize(minHeight = 44.dp).background(if (selected) colors.primary else colors.surface2, RoundedCornerShape(999.dp)).border(1.dp, if (selected) colors.primary else colors.border, RoundedCornerShape(999.dp)).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(if (selected) "✓ $label" else label, color = if (selected) colors.onPrimary else colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ReferenceExactTimeEditor(value: String, valid: Boolean, onValueChange: (String) -> Unit, onFocus: () -> Unit) {
    val colors = LocalAppColors.current
    Column {
        Text(if (L.isTr) "Hazır olma saati" else "Ready time", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocus() }.semantics { contentDescription = if (L.isTr) "Hazır olma saatini gir" else "Enter ready time" }.background(colors.surface2, RoundedCornerShape(12.dp)).border(1.dp, if (valid) colors.border else colors.danger, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onFocus() }),
            singleLine = true
        )
        if (!valid) {
            Spacer(Modifier.height(5.dp))
            Text(if (L.isTr) "Saati SS:DD biçiminde gir." else "Enter time as HH:MM.", color = colors.danger, fontSize = 12.sp)
        }
    }
}

@Composable
fun ReferenceOperationsScreen(
    planState: PlanState,
    pantryIntel: PantryIntelReport,
    hardwareSettings: HardwareSettings,
    selectedEquipment: Set<String>,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (Bitmap) -> Unit,
    onClearVision: () -> Unit,
    onBackToOptions: () -> Unit,
    cookingState: CookingSessionState,
    onStartCooking: () -> Unit,
    onPauseCooking: () -> Unit,
    onResumeCooking: () -> Unit,
    onCompleteCookingStep: (String) -> Unit,
    onSkipCookingStep: (String) -> Unit,
    onEndCooking: () -> Unit,
    pendingConsumption: PendingConsumption? = null,
    inventory: List<PantryStockItem> = emptyList(),
    onConsumePlanned: () -> Unit = {},
    onConsumeActual: (Map<String, Double>) -> Unit = {},
    onCancelConsumption: () -> Unit = {},
    onRequestSubstitution: (String) -> Unit = {},
    onApplySubstitution: () -> Unit = {},
    onDismissSubstitution: () -> Unit = {},
    onAddShortagesToShoppingList: () -> Unit = {}
) {
    val active = planState as? PlanState.RecipeActive
    if (active == null || cookingState.status != CookingSessionStatus.READY || pendingConsumption != null) {
        OperationsScreen(
            planState = planState,
            pantryIntel = pantryIntel,
            hardwareSettings = hardwareSettings,
            selectedEquipment = selectedEquipment,
            onAskAgent = onAskAgent,
            onClearChat = onClearChat,
            onCheckPan = onCheckPan,
            onClearVision = onClearVision,
            onBackToOptions = onBackToOptions,
            cookingState = cookingState,
            onStartCooking = onStartCooking,
            onPauseCooking = onPauseCooking,
            onResumeCooking = onResumeCooking,
            onCompleteCookingStep = onCompleteCookingStep,
            onSkipCookingStep = onSkipCookingStep,
            onEndCooking = onEndCooking,
            pendingConsumption = pendingConsumption,
            inventory = inventory,
            onConsumePlanned = onConsumePlanned,
            onConsumeActual = onConsumeActual,
            onCancelConsumption = onCancelConsumption,
            onRequestSubstitution = onRequestSubstitution,
            onApplySubstitution = onApplySubstitution,
            onDismissSubstitution = onDismissSubstitution,
            onAddShortagesToShoppingList = onAddShortagesToShoppingList
        )
        return
    }

    ReferencePlanReview(
        state = active,
        pantryIntel = pantryIntel,
        hardwareSettings = hardwareSettings,
        selectedEquipment = selectedEquipment,
        onStartCooking = onStartCooking,
        onBackToOptions = onBackToOptions,
        onAskAgent = onAskAgent,
        onClearChat = onClearChat,
        onCheckPan = onCheckPan,
        onClearVision = onClearVision,
        onRequestSubstitution = onRequestSubstitution,
        onApplySubstitution = onApplySubstitution,
        onDismissSubstitution = onDismissSubstitution,
        onAddShortagesToShoppingList = onAddShortagesToShoppingList
    )
}

@Composable
private fun ReferencePlanReview(
    state: PlanState.RecipeActive,
    pantryIntel: PantryIntelReport,
    hardwareSettings: HardwareSettings,
    selectedEquipment: Set<String>,
    onStartCooking: () -> Unit,
    onBackToOptions: () -> Unit,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (Bitmap) -> Unit,
    onClearVision: () -> Unit,
    onRequestSubstitution: (String) -> Unit,
    onApplySubstitution: () -> Unit,
    onDismissSubstitution: () -> Unit,
    onAddShortagesToShoppingList: () -> Unit
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val plan = state.cookingPlan
    val canStart = canStartPreparedCooking(state.shortages)

    Column(modifier = Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(horizontal = if (spec.dense) 16.dp else 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBackToOptions, modifier = Modifier.semantics { contentDescription = if (L.isTr) "Tariflere dön" else "Back to recipes" }) { Text("‹", color = colors.onSurface, fontSize = 25.sp) }
            EditorialBrandMark(size = 20.dp)
            Spacer(Modifier.width(7.dp))
            Text("AgenticKitchen", color = colors.onSurface, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Text(if (L.isTr) "Plan İncelemesi" else "Plan Review", color = colors.onSurface, style = MaterialTheme.typography.h1)
        Spacer(Modifier.height(4.dp))
        Text(
            if (L.isTr) "Pişirmeden önce planı, güvenliği ve mutfak araçlarını gözden geçir."
            else "Review the steps, safety, and kitchen setup before cooking.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body2
        )
        Spacer(Modifier.height(14.dp))
        ReferencePlanHero(state)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStartCooking,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, disabledBackgroundColor = colors.divider),
            shape = RoundedCornerShape(if (spec.dense) 14.dp else 999.dp)
        ) { Text(if (L.isTr) "Pişirmeye Başla" else "Start Cooking", color = colors.onPrimary) }
        if (!canStart) {
            Spacer(Modifier.height(7.dp))
            Text(
                if (L.isTr) "Başlamadan önce eksikleri tamamla veya güvenli bir alternatif uygula."
                else "Resolve shortages or apply a safe substitution before starting.",
                color = colors.warn,
                style = MaterialTheme.typography.caption
            )
        }

        Spacer(Modifier.height(18.dp))
        ReferencePlanSectionHeader(
            title = if (L.isTr) "Adım adım plan" else "Step-by-step plan",
            trailing = plan?.let {
                if (L.isTr) "${it.steps.size} adım · ${referenceMinutes(it.steps.sumOf { step -> step.durationSeconds })} dk"
                else "${it.steps.size} steps · ${referenceMinutes(it.steps.sumOf { step -> step.durationSeconds })} min"
            }
        )
        if (plan == null || plan.steps.isEmpty()) {
            ReferenceInfoCard(
                title = if (L.isTr) "Plan bekleniyor" else "Plan unavailable",
                body = if (L.isTr) "Doğrulanmış pişirme adımları henüz yok." else "No validated cooking steps are available yet.",
                accent = colors.warn
            )
        } else {
            plan.steps.forEachIndexed { index, step -> ReferencePlanStep(index + 1, step.instruction, step.resource, step.durationSeconds) }
        }

        Spacer(Modifier.height(16.dp))
        ReferenceSubstitutionCard(state, onRequestSubstitution, onApplySubstitution, onDismissSubstitution, onAddShortagesToShoppingList)
        Spacer(Modifier.height(10.dp))
        val safetyBody = when {
            plan == null -> if (L.isTr) "Doğrulanmış plan yok." else "No validated plan is available."
            plan.safetyNotes.isNotEmpty() -> plan.safetyNotes.joinToString(" · ")
            else -> if (L.isTr) "Ek güvenlik notu yok." else "No additional safety notes."
        }
        ReferenceInfoCard(
            title = if (L.isTr) "Güvenlik ve alerjenler" else "Safety & allergens",
            body = safetyBody,
            accent = if (plan?.safetyNotes.isNullOrEmpty()) colors.success else colors.warn
        )
        Spacer(Modifier.height(10.dp))
        ReferenceInfoCard(
            title = if (L.isTr) "Mutfak kurulumu" else "Kitchen setup",
            body = referenceEquipmentSummary(hardwareSettings, selectedEquipment),
            accent = colors.primary
        )
        Spacer(Modifier.height(10.dp))
        ReferenceAssistantCard(state, onAskAgent, onClearChat, onCheckPan, onClearVision)
        if (pantryIntel.readinessScore > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                if (L.isTr) "Kiler hazırlığı ${pantryIntel.readinessScore}/100" else "Pantry readiness ${pantryIntel.readinessScore}/100",
                color = colors.primary,
                style = MaterialTheme.typography.caption
            )
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToOptions, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (L.isTr) "Tariflere dön" else "Back to recipes", color = colors.onSurfaceSub)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReferencePlanHero(state: PlanState.RecipeActive) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val plan = state.cookingPlan
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceRadius(spec))) {
        Row(Modifier.fillMaxWidth().padding(if (spec.dense) 12.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (spec.dense) 88.dp else 102.dp).background(colors.surface2, RoundedCornerShape(referenceRadius(spec))), contentAlignment = Alignment.Center) {
                IngredientArtwork(state.recipe.name, Modifier.fillMaxSize().padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (L.isTr) "PİŞİRME PLANI" else "COOKING PLAN", color = colors.primary, style = MaterialTheme.typography.overline)
                Spacer(Modifier.height(4.dp))
                Text(state.recipe.name, color = colors.onSurface, style = MaterialTheme.typography.h5)
                Spacer(Modifier.height(5.dp))
                val totalMinutes = plan?.steps?.sumOf { it.durationSeconds }?.let(::referenceMinutes)
                Text(
                    listOfNotNull(totalMinutes?.let { if (L.isTr) "$it dk" else "$it min" }, if (L.isTr) "${state.servings} kişilik" else "${state.servings} servings", recipeTypeLabel(state.recipe.type, L.isTr)).joinToString(" · "),
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
private fun ReferencePlanSectionHeader(title: String, trailing: String?) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, color = colors.primary, style = MaterialTheme.typography.caption) }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReferencePlanStep(index: Int, instruction: String, resource: String, durationSeconds: Int) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = if (spec.dense) 7.dp else 9.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(24.dp).background(colors.surface2, RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
            Text(index.toString(), color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(instruction, color = colors.onSurface, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(cookingResourceLabel(resource, L.isTr), color = colors.warn, style = MaterialTheme.typography.caption)
        }
        Text(if (L.isTr) "${referenceMinutes(durationSeconds)} dk" else "${referenceMinutes(durationSeconds)} min", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
    }
    Divider(color = colors.divider)
}

@Composable
private fun ReferenceSubstitutionCard(
    state: PlanState.RecipeActive,
    onRequest: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    onAddToShopping: () -> Unit
) {
    val colors = LocalAppColors.current
    val body = if (state.shortages.isEmpty()) {
        if (L.isTr) "Bu plan için stoktan değişiklik gerekmiyor." else "No pantry substitution is needed for this plan."
    } else {
        if (L.isTr) "${state.shortages.size} eksik malzeme için güvenli alternatif ara."
        else "Find a safe substitute for ${state.shortages.size} missing ingredient(s)."
    }
    ReferenceInfoCard(if (L.isTr) "Akıllı değişiklikler" else "Smart substitutions", body, if (state.shortages.isEmpty()) colors.success else colors.warn)
    if (state.shortages.isNotEmpty()) {
        state.shortages.take(3).forEach { shortage ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(shortage, color = colors.onSurface, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRequest(shortage) }) { Text(if (L.isTr) "Alternatif bul" else "Find substitute", color = colors.primary) }
            }
        }
        OutlinedButton(onClick = onAddToShopping, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp), border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(999.dp)) {
            Text(if (L.isTr) "Eksikleri alışverişe ekle" else "Add shortages to shopping", color = colors.primary)
        }
    }
    when (val substitution = state.substitutionState) {
        SubstitutionState.Idle -> Unit
        is SubstitutionState.Loading -> Text(
            if (L.isTr) "${substitution.originalIngredientName} için alternatif aranıyor…" else "Finding a substitute for ${substitution.originalIngredientName}…",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.caption
        )
        is SubstitutionState.Error -> {
            Text(substitution.message, color = colors.danger, style = MaterialTheme.typography.caption)
            TextButton(onClick = onDismiss) { Text(if (L.isTr) "Kapat" else "Dismiss") }
        }
        is SubstitutionState.Review -> {
            Spacer(Modifier.height(8.dp))
            Text("${substitution.originalIngredientName} → ${substitution.response.replacementIngredient.name}", color = colors.onSurface, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(4.dp))
            Text(substitution.response.reason, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary)) { Text(if (L.isTr) "Uygula" else "Apply", color = colors.onPrimary) }
                TextButton(onClick = onDismiss) { Text(if (L.isTr) "Vazgeç" else "Cancel") }
            }
        }
    }
}

@Composable
private fun ReferenceInfoCard(title: String, body: String, accent: Color) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceRadius(spec))) {
        Row(Modifier.padding(if (spec.dense) 12.dp else 14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(9.dp).background(accent, RoundedCornerShape(999.dp)))
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(body, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun ReferenceAssistantCard(
    state: PlanState.RecipeActive,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (Bitmap) -> Unit,
    onClearVision: () -> Unit
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    var question by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap -> if (bitmap != null) onCheckPan(bitmap) }
    fun submit() {
        val trimmed = question.trim()
        if (trimmed.isNotEmpty()) {
            onAskAgent(trimmed)
            question = ""
            focusManager.clearFocus(force = true)
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.aiBg, elevation = 0.dp, border = BorderStroke(1.dp, colors.ai.copy(alpha = .35f)), shape = RoundedCornerShape(referenceRadius(spec))) {
        Column(Modifier.padding(if (spec.dense) 12.dp else 14.dp)) {
            Text(if (L.isTr) "AI Mutfak Asistanı" else "AI Kitchen Assistant", color = colors.ai, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (L.isTr) "Başlamadan önce malzeme, zamanlama veya teknik sor."
                else "Ask about ingredients, timing, or technique before you cook.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(1.dp, if (focused) colors.ai else colors.border, RoundedCornerShape(12.dp)).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused }.padding(vertical = 12.dp),
                    textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    decorationBox = { inner ->
                        Box {
                            if (question.isBlank()) Text(if (L.isTr) "Bu tarif hakkında sor…" else "Ask about this recipe…", color = colors.onSurfaceSub, fontSize = 13.sp)
                            inner()
                        }
                    }
                )
                TextButton(onClick = ::submit, enabled = question.isNotBlank()) { Text(if (L.isTr) "Gönder" else "Send", color = colors.ai) }
            }
            state.agentChatResponse?.let { response ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(response, color = colors.onSurface, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearChat) { Text(if (L.isTr) "Temizle" else "Clear", color = colors.onSurfaceSub) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Divider(color = colors.border)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (L.isTr) "Tava kontrolü" else "Pan Check", color = colors.onSurface, fontWeight = FontWeight.Bold)
                    Text(if (L.isTr) "Fotoğrafla pişirme hazırlığını kontrol et." else "Use a photo to check cooking readiness.", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                }
                TextButton(onClick = { cameraLauncher.launch(null) }) { Text(if (L.isTr) "Fotoğraf çek" else "Take photo", color = colors.primary) }
            }
            state.visionScanResponse?.let { response ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(response, color = colors.onSurface, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearVision) { Text(if (L.isTr) "Temizle" else "Clear", color = colors.onSurfaceSub) }
                }
            }
        }
    }
}

private fun referenceCandidateStatus(option: RecipeOption): String = when (option.matchTier) {
    RecipeMatchTier.READY_NOW -> option.pantryCoveragePercent?.let { if (L.isTr) "%$it eşleşme" else "$it% match" } ?: if (L.isTr) "Hazır şimdi" else "Ready now"
    RecipeMatchTier.MISSING_ONE -> if (L.isTr) "1 eksik" else "Missing 1"
    RecipeMatchTier.MISSING_TWO -> if (L.isTr) "2 eksik" else "Missing 2"
    RecipeMatchTier.AI_IDEA -> if (L.isTr) "AI fikri" else "AI idea"
}

private fun referenceFreshnessLabel(option: RecipeOption): String = if (L.isTr) {
    listOfNotNull(
        option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it bugün kullanılmalı" },
        option.useSoonMatches.takeIf { it > 0 }?.let { "$it yakında kullanılmalı" }
    ).joinToString(" · ")
} else {
    listOfNotNull(
        option.expiresTodayMatches.takeIf { it > 0 }?.let { "$it expires today" },
        option.useSoonMatches.takeIf { it > 0 }?.let { "$it use soon" }
    ).joinToString(" · ")
}

private fun referenceRadius(spec: ThemeSpec) = when (spec.typographyProfile) {
    TypographyProfile.MODERN_SANS -> 18.dp
    TypographyProfile.PREMIUM_CINEMATIC -> 16.dp
    TypographyProfile.APPLIANCE_CONTROL -> 12.dp
    TypographyProfile.WARM_EDITORIAL -> 22.dp
    TypographyProfile.MINIMAL_PRO -> 14.dp
}

private fun referenceQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.ROOT, "%.1f", value).trimEnd('0').trimEnd('.')

private fun referenceMinutes(seconds: Int): Int = ((seconds.coerceAtLeast(0) + 59) / 60).coerceAtLeast(1)

private fun referenceEquipmentSummary(hardwareSettings: HardwareSettings, selectedEquipment: Set<String>): String {
    val stove = when (hardwareSettings.stoveType.lowercase(Locale.ROOT)) {
        "gas" -> if (L.isTr) "gaz ocak" else "gas stove"
        else -> if (L.isTr) "elektrikli ocak" else "electric stove"
    }
    val oven = if (hardwareSettings.ovenAvailable) {
        if (L.isTr) "fırın hazır" else "oven available"
    } else {
        if (L.isTr) "fırın yok" else "no oven"
    }
    val tools = if (selectedEquipment.isEmpty()) {
        if (L.isTr) "ek araç seçilmedi" else "no extra tools selected"
    } else {
        if (L.isTr) "${selectedEquipment.size} araç seçili" else "${selectedEquipment.size} tools selected"
    }
    return "$stove · $oven · $tools"
}
