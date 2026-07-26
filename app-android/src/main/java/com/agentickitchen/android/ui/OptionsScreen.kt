package com.agentickitchen.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val themeSpec = LocalThemeSpec.current
    var selectedOptionForTime by remember { mutableStateOf<RecipeOption?>(null) }
    var targetTimeInput by remember { mutableStateOf("19:30") }

    val title = when (themeSpec.id) {
        "heritage" -> if (L.isTr) "Arşiv Önerileri" else "Archive Options"
        "zen" -> if (L.isTr) "Seçilmiş Planlar" else "Curated Options"
        else -> if (L.isTr) "Görev Adayları" else "Mission Candidates"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getBgGradient())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        OptionsHero(title = title, pantryIntel = pantryIntel)
        Spacer(Modifier.height(20.dp))

        when (planState) {
            is PlanState.Idle -> EmptyOptionsCard(chips = chips, colors = colors, onStart = onStart)
            is PlanState.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.primary)
            }

            is PlanState.OptionsReady -> {
                OptionsSummaryCard(pantryIntel = pantryIntel)
                Spacer(Modifier.height(16.dp))
                OptionsListCard(
                    options = planState.options,
                    colors = colors,
                    onRefresh = onRefresh,
                    onSelect = { selectedOptionForTime = it }
                )
            }

            is PlanState.RecipeActive -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = colors.surface,
                    shape = RoundedCornerShape(20.dp),
                    elevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            if (L.isTr) "Operasyon zaten başlatıldı." else "Operation already launched.",
                            color = colors.onSurface,
                            style = MaterialTheme.typography.h6
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            planState.recipe.name,
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onBackToOptions,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.surfaceAlt),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(if (L.isTr) "Seçeneklere Dön" else "Back To Options", color = colors.onSurface)
                            }
                        }
                    }
                }
            }

            is PlanState.Error -> ErrorCard(planState.message, colors)
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
