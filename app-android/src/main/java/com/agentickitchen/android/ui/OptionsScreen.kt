package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeRequestSelection
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.scheduler.TargetTimeChoice
import java.time.Duration
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(
    chips: List<String>,
    planState: PlanState,
    pantryIntel: PantryIntelReport,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onSelectOption: (RecipeOption, RecipeRequestSelection) -> Unit,
    onBackToOptions: () -> Unit
) {
    val colors = LocalAppColors.current
    var selectedOptionForTime by remember { mutableStateOf<RecipeOption?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        EditorialOptionsHeader()
        Spacer(Modifier.height(28.dp))

        when (planState) {
            is PlanState.Idle -> EditorialEmptyOptions(onStart = onStart)
            is PlanState.Loading -> EditorialOptionsLoading()

            is PlanState.OptionsReady -> {
                EditorialRecipeList(
                    options = planState.options,
                    onRefresh = onRefresh,
                    onSelect = { selectedOptionForTime = it }
                )
            }

            is PlanState.RecipeActive -> EditorialRecipeActive(planState.recipe, onBackToOptions)

            is PlanState.Error -> EditorialOptionsError(planState.message, onStart)
        }
    }

    if (selectedOptionForTime != null) {
        EditorialRecipeDetailOverlay(
            recipe = selectedOptionForTime!!,
            onDismiss = { selectedOptionForTime = null },
            onConfirm = { selection ->
                onSelectOption(selectedOptionForTime!!, selection)
                selectedOptionForTime = null
            }
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

internal fun exactTargetTimeChoice(value: String): TargetTimeChoice.Exact? =
    runCatching { TargetTimeChoice.Exact(LocalTime.parse(value)) }.getOrNull()

internal fun recipeRequestSelection(servings: Int, targetTime: TargetTimeChoice) =
    RecipeRequestSelection(servings = servings.coerceIn(1, 12), targetTime = targetTime)

@Composable
private fun EditorialRecipeDetailOverlay(
    recipe: RecipeOption,
    onDismiss: () -> Unit,
    onConfirm: (RecipeRequestSelection) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EditorialRecipeDetailContent(recipe = recipe, onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
private fun EditorialRecipeDetailContent(
    recipe: RecipeOption,
    onDismiss: () -> Unit,
    onConfirm: (RecipeRequestSelection) -> Unit,
    initialTargetId: String = "after_20"
) {
    val colors = LocalAppColors.current
    val presets = targetTimePresetOptions(L.isTr)
    var selectedTargetId by remember(recipe.id, initialTargetId) { mutableStateOf(initialTargetId) }
    var exactTime by remember(recipe.id) { mutableStateOf("19:30") }
    var servings by remember(recipe.id) { mutableStateOf(2) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val selected = presets.firstOrNull { it.id == selectedTargetId } ?: presets.first()
    val selectedChoice = if (selected.id == "exact") exactTargetTimeChoice(exactTime) else selected.choice

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 12 } + scaleIn(tween(320), initialScale = .96f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.semantics {
                        contentDescription = if (L.isTr) "Tarif ayrıntısını kapat" else "Close recipe detail"
                    }) {
                        Text(if (L.isTr) "Kapat" else "Close", color = colors.onSurfaceSub)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(196.dp).semantics {
                        contentDescription = if (L.isTr) "${recipe.name} tarif görseli" else "${recipe.name} recipe artwork"
                    },
                    contentAlignment = Alignment.Center
                ) {
                    IngredientArtwork(recipe.name, Modifier.size(176.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(recipe.type.uppercase(), color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(10.dp))
                Text(recipe.name, color = colors.onSurface, style = MaterialTheme.typography.h1)
                Spacer(Modifier.height(12.dp))
                Text(recipe.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(28.dp))
                Divider(color = colors.divider, thickness = 1.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    if (L.isTr) "Kaç kişilik?" else "How many servings?",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h6
                )
                Spacer(Modifier.height(12.dp))
                RecipeServingsSelector(
                    servings = servings,
                    onDecrease = { servings = (servings - 1).coerceAtLeast(1) },
                    onIncrease = { servings = (servings + 1).coerceAtMost(12) }
                )
                Spacer(Modifier.height(24.dp))
                Divider(color = colors.divider, thickness = 1.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    if (L.isTr) "Ne zaman hazır olsun?" else "When should it be ready?",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h6
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { option ->
                        TargetTimeChoicePill(
                            option = option,
                            selected = option.id == selectedTargetId,
                            onClick = { selectedTargetId = option.id }
                        )
                    }
                }
                if (selected.id == "exact") {
                    Spacer(Modifier.height(16.dp))
                    ExactTimeEditor(
                        value = exactTime,
                        onValueChange = { exactTime = it },
                        valid = selectedChoice != null
                    )
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { selectedChoice?.let { onConfirm(recipeRequestSelection(servings, it)) } },
                    enabled = selectedChoice != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, disabledBackgroundColor = colors.divider),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Tarifi Hazırla" else "Prepare Recipe", color = colors.onPrimary)
                }
            }
        }
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
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .semantics { contentDescription = if (L.isTr) "Porsiyonu azalt" else "Decrease servings" }
        ) { Text("−", color = colors.primary, fontSize = 22.sp) }
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
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .semantics { contentDescription = if (L.isTr) "Porsiyonu artır" else "Increase servings" }
        ) { Text("+", color = colors.primary, fontSize = 22.sp) }
    }
}

@Composable
private fun TargetTimeChoicePill(option: TargetTimeUiOption, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val background by animateColorAsState(if (selected) colors.primary else colors.surfaceAlt, tween(240), label = "targetBackground")
    val textColor by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface, tween(240), label = "targetText")
    val scale by animateFloatAsState(if (selected) 1f else .97f, tween(240), label = "targetScale")
    Box(
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .background(background, RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) colors.primary else colors.divider, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (selected) "✓ ${option.label}" else option.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExactTimeEditor(value: String, onValueChange: (String) -> Unit, valid: Boolean) {
    val colors = LocalAppColors.current
    Column {
        Text(if (L.isTr) "Hazır olma saati" else "Ready time", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = if (L.isTr) "Hazır olma saatini gir" else "Enter ready time"
            }.background(colors.surfaceAlt, RoundedCornerShape(12.dp))
                .border(1.dp, if (valid) colors.divider else Color(0xFF9B3F32), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        if (!valid) {
            Spacer(Modifier.height(6.dp))
            Text(if (L.isTr) "Saati SS:DD biçiminde gir." else "Enter time as HH:MM.", color = Color(0xFF9B3F32), fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialRecipeDetailPresetPreview() {
    AgenticTheme("editorial") {
        EditorialRecipeDetailContent(
            recipe = RecipeOption("1", "Makarna", "Kremalı Tavuklu Makarna", "Tavuk ve kiler malzemeleriyle sakin, kremalı bir akşam yemeği."),
            onDismiss = {}, onConfirm = {}, initialTargetId = "after_45"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialRecipeDetailExactPreview() {
    AgenticTheme("editorial") {
        EditorialRecipeDetailContent(
            recipe = RecipeOption("1", "Makarna", "Kremalı Tavuklu Makarna", "Tavuk ve kiler malzemeleriyle sakin, kremalı bir akşam yemeği."),
            onDismiss = {}, onConfirm = {}, initialTargetId = "exact"
        )
    }
}

@Composable
private fun EditorialOptionsHeader() {
    val colors = LocalAppColors.current
    Column {
        EditorialBrandLockup()
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) "Bu malzemelerle" else "With what you have",
            color = colors.onBackground,
            style = MaterialTheme.typography.h1
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) {
                "Mutfağındaki malzemelere göre hazırlanmış tarifler."
            } else {
                "Recipes composed around what is already in your kitchen."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun EditorialRecipeList(
    options: List<RecipeOption>,
    onRefresh: () -> Unit,
    onSelect: (RecipeOption) -> Unit
) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(options) { visible = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (L.isTr) "Tarif önerileri" else "Recipe ideas",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh) {
                Text(if (L.isTr) "Başka öneriler" else "More ideas", color = colors.primary)
            }
        }
        Spacer(Modifier.height(8.dp))
        options.forEachIndexed { index, option ->
            EditorialRecipeRow(
                option = option,
                index = index,
                visible = visible,
                onClick = { onSelect(option) }
            )
            if (index < options.lastIndex) Divider(color = colors.divider, thickness = 1.dp)
        }
    }
}

@Composable
private fun EditorialRecipeRow(
    option: RecipeOption,
    index: Int,
    visible: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, tween(110), label = "recipePress")

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260, index * 40)) + slideInVertically(tween(260, index * 40)) { -it / 8 }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clickable(interactionSource = interactions, indication = null, onClick = onClick)
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "%02d".format(index + 1),
                color = colors.primary,
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(option.name, color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(7.dp))
                Text(
                    option.type.uppercase(),
                    color = colors.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(option.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (L.isTr) "Tarifi incele →" else "Explore recipe →",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.button
                )
            }
            IngredientArtwork(option.name, Modifier.size(58.dp))
        }
    }
}

@Composable
private fun EditorialEmptyOptions(onStart: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
        IngredientArtwork("", Modifier.size(76.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (L.isTr) "Henüz tarif yok." else "No recipes yet.", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(6.dp))
        Text(
            if (L.isTr) "Malzemelerini ekledikten sonra önerileri oluştur." else "Add your ingredients, then generate some ideas.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onStart) {
            Text(if (L.isTr) "Tarifleri oluştur" else "Generate ideas", color = colors.primary)
        }
    }
}

@Composable
private fun EditorialOptionsLoading() {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "optionLoading")
    val alpha by transition.animateFloat(
        initialValue = .25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800)),
        label = "optionLoadingAlpha"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IngredientArtwork("", Modifier.size(82.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            if (L.isTr) "Şef seçenekleri hazırlıyor…" else "The chef is preparing ideas…",
            color = colors.onSurface,
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        Text("•••", color = colors.primary.copy(alpha = alpha), letterSpacing = 4.sp)
    }
}

@Composable
private fun EditorialOptionsError(message: String, onRetry: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(if (L.isTr) "Tarifler hazırlanamadı." else "Recipes could not be prepared.", color = Color(0xFF9B3F32), style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text(message, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text(if (L.isTr) "Tekrar dene" else "Try again", color = colors.primary)
        }
    }
}

@Composable
private fun EditorialRecipeActive(recipe: RecipeOption, onBackToOptions: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(
            if (L.isTr) "Bu tarif pişirmeye hazır." else "This recipe is ready to cook.",
            color = colors.onSurface,
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        Text(recipe.name, color = colors.primary, style = MaterialTheme.typography.h3)
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onBackToOptions) {
            Text(if (L.isTr) "Başka tarif seç" else "Choose another recipe", color = colors.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialRecipeOptionsPreview() {
    AgenticTheme("editorial") {
        OptionsScreen(
            chips = listOf("Domates", "Tavuk", "Pirinç"),
            planState = PlanState.OptionsReady(
                listOf(
                    RecipeOption("1", "Makarna", "Kremalı Tavuklu Makarna", "Tavuk ve kiler malzemeleriyle sakin, kremalı bir akşam yemeği."),
                    RecipeOption("2", "Tava Yemeği", "Baharatlı Tavuk Tavası", "Tek tavada, malzemeleri öne çıkaran pratik bir tarif."),
                    RecipeOption("3", "Fırın", "Domatesli Pirinç", "Fırında yavaşça pişen sıcak ve sade bir tabak.")
                )
            ),
            pantryIntel = PantryIntelReport(70, "vegetables", "Vegetables", emptyList(), emptyList(), emptyList(), "stovetop"),
            onStart = {},
            onRefresh = {},
            onSelectOption = { _, _ -> },
            onBackToOptions = {}
        )
    }
}

@Composable
private fun OptionsHero(title: String, pantryIntel: PantryIntelReport) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surfaceAlt,
        shape = RoundedCornerShape(24.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                if (L.isTr) "PANTRY INTEL" else "PANTRY INTEL",
                color = colors.primary,
                style = MaterialTheme.typography.caption
            )
            Spacer(Modifier.height(8.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h1, lineHeight = 42.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                if (L.isTr) {
                    "${pantryCategoryLabel(pantryIntel.focusCategoryId)} odaklı, skor ${pantryIntel.readinessScore}/100."
                } else {
                    "Focused on ${pantryCategoryLabel(pantryIntel.focusCategoryId)}, readiness ${pantryIntel.readinessScore}/100."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
private fun EmptyOptionsCard(chips: List<String>, colors: AppColors, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                if (L.isTr) "Henüz tarif alternatifi yok." else "No recipe options yet.",
                color = colors.onSurface,
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (chips.isEmpty()) {
                    if (L.isTr) "Önce İstihbarat sekmesinde malzemeleri gir." else "Start by entering ingredients on the Intelligence tab."
                } else {
                    if (L.isTr) "Hazırsan üretimi başlat." else "Your pantry is loaded. Generate the option set when ready."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Alternatifleri Üret" else "Generate Options", color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun OptionsSummaryCard(pantryIntel: PantryIntelReport) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                if (L.isTr) "Hazırlık Özeti" else "Readiness Snapshot",
                color = colors.onSurface,
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${pantryIntel.readinessScore}/100 • ${pantryCategoryLabel(pantryIntel.focusCategoryId)}",
                color = colors.primary,
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(Modifier.height(8.dp))
            pantryIntel.tactics.take(2).forEach { tactic ->
                Text("• ${pantrySignalText(tactic)}", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
