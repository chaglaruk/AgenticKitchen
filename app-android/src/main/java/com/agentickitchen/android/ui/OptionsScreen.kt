package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
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
    onSelectOption: (RecipeOption, TargetTimeChoice) -> Unit,
    onBackToOptions: () -> Unit
) {
    val colors = LocalAppColors.current
    var selectedOptionForTime by remember { mutableStateOf<RecipeOption?>(null) }
    var targetTimeInput by remember { mutableStateOf("19:30") }

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
        Dialog(onDismissRequest = { selectedOptionForTime = null }) {
            Card(
                backgroundColor = colors.surface,
                shape = RoundedCornerShape(22.dp),
                elevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        if (L.isTr) "Operasyon Zamanı" else "Operation Timing",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "Yemek ne zaman hazır olsun?" else "When should the dish be ready?",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(Modifier.height(16.dp))
                    val predefinedTimes = listOf(
                        if (L.isTr) "Hemen (20 dk)" else "Now (20m)",
                        if (L.isTr) "45 Dakika Sonra" else "In 45 Minutes",
                        if (L.isTr) "1 Saat Sonra" else "In 1 Hour",
                        if (L.isTr) "Akşam" else "This Evening",
                        if (L.isTr) "Farketmez" else "Flexible"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedTimes.forEach { time ->
                            val selected = targetTimeInput == time
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) colors.primary else colors.surfaceAlt,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) colors.primary else colors.divider,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .clickable { targetTimeInput = time }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = time,
                                    color = if (selected) colors.onPrimary else colors.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedOptionForTime = null }) {
                            Text(L.cancel, color = colors.onSurfaceSub)
                        }
                        Button(
                            onClick = {
                                onSelectOption(selectedOptionForTime!!, targetTimeChoice(targetTimeInput))
                                selectedOptionForTime = null
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "Operasyonu Başlat" else "Launch Operation", color = colors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialOptionsHeader() {
    val colors = LocalAppColors.current
    Column {
        Text("Agentic Kitchen", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
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

internal fun targetTimeChoice(label: String): TargetTimeChoice = when {
    label.contains("20") -> TargetTimeChoice.After(Duration.ofMinutes(20))
    label.contains("45") -> TargetTimeChoice.After(Duration.ofMinutes(45))
    label.contains("1 Saat") || label.contains("1 Hour") -> TargetTimeChoice.After(Duration.ofHours(1))
    label.contains("Ak") || label.contains("Evening") -> TargetTimeChoice.ThisEvening
    label.contains("Farketmez") || label.contains("Flexible") -> TargetTimeChoice.Flexible
    else -> TargetTimeChoice.Exact(LocalTime.parse(label))
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
