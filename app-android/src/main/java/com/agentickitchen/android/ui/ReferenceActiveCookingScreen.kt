package com.agentickitchen.android.ui

import android.app.Activity
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L
import com.agentickitchen.android.PendingConsumption
import com.agentickitchen.android.PlanState
import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus
import com.agentickitchen.shared.cooking.LiveOperation
import com.agentickitchen.shared.cooking.cookingAddMinuteCommand
import com.agentickitchen.shared.cooking.cookingVisibleCompleted
import com.agentickitchen.shared.cooking.cookingVisibleSkipped
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.ScheduleEvent
import java.util.Locale

@Composable
fun ReferenceActiveCookingScreen(
    active: PlanState.RecipeActive,
    pantryIntel: PantryIntelReport,
    hardwareSettings: HardwareSettings,
    selectedEquipment: Set<String>,
    cookingState: CookingSessionState,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (Bitmap) -> Unit,
    onClearVision: () -> Unit,
    onPauseCooking: () -> Unit,
    onResumeCooking: () -> Unit,
    onCompleteCookingStep: (String) -> Unit,
    onSkipCookingStep: (String) -> Unit,
    onEndCooking: () -> Unit,
    onBackToOptions: () -> Unit,
    pendingConsumption: PendingConsumption? = null,
    inventory: List<PantryStockItem> = emptyList(),
    onConsumePlanned: () -> Unit = {},
    onConsumeActual: (Map<String, Double>) -> Unit = {},
    onCancelConsumption: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val activity = LocalContext.current as? Activity
    DisposableEffect(cookingState.status) {
        if (cookingState.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = if (spec.dense) 16.dp else 18.dp, vertical = 14.dp)
    ) {
        ActiveCookingBrand(active.recipe.name)
        Spacer(Modifier.height(if (spec.dense) 10.dp else 14.dp))

        when (cookingState.status) {
            CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED -> ReferenceRunningCooking(
                active = active,
                state = cookingState,
                onPause = onPauseCooking,
                onResume = onResumeCooking,
                onComplete = onCompleteCookingStep,
                onSkip = onSkipCookingStep,
                onEnd = onEndCooking
            )
            CookingSessionStatus.COMPLETED, CookingSessionStatus.ENDED -> ReferenceCompletedCooking(
                active = active,
                state = cookingState,
                onBackToOptions = onBackToOptions
            )
            else -> Unit
        }

        if (cookingState.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            Spacer(Modifier.height(14.dp))
            ReferenceActiveAssistant(
                active = active,
                onAskAgent = onAskAgent,
                onClearChat = onClearChat,
                onCheckPan = onCheckPan,
                onClearVision = onClearVision
            )
            Spacer(Modifier.height(10.dp))
            ReferenceActiveKitchenSummary(pantryIntel, hardwareSettings, selectedEquipment)
        }
        Spacer(Modifier.height(16.dp))
    }

    pendingConsumption
        ?.takeIf { cookingState.status in setOf(CookingSessionStatus.COMPLETED, CookingSessionStatus.ENDED) }
        ?.let {
            ReferenceConsumptionDialog(
                pending = it,
                inventory = inventory,
                onUsePlanned = onConsumePlanned,
                onUseActual = onConsumeActual,
                onCancel = onCancelConsumption
            )
        }
}

@Composable
private fun ActiveCookingBrand(recipeName: String) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EditorialBrandMark(size = 21.dp)
        Spacer(Modifier.width(8.dp))
        Text("AgenticKitchen", color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (spec.typographyProfile == TypographyProfile.APPLIANCE_CONTROL && recipeName.isNotBlank()) {
            Text(if (L.isTr) "CANLI" else "LIVE", color = colors.success, style = androidx.compose.material.MaterialTheme.typography.overline)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(if (L.isTr) "Aktif Pişirme" else "Active Cooking", color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h1)
    if (recipeName.isNotBlank() && spec.typographyProfile != TypographyProfile.APPLIANCE_CONTROL) {
        Spacer(Modifier.height(3.dp))
        Text(recipeName, color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
    }
}

@Composable
private fun ReferenceRunningCooking(
    active: PlanState.RecipeActive,
    state: CookingSessionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit,
    onEnd: () -> Unit
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val completed = cookingVisibleCompleted(state.completed)
    val skipped = cookingVisibleSkipped(state.skipped)
    val total = active.events.size.coerceAtLeast(completed.size + skipped.size + state.active.size + state.upcoming.size)
    val primary = state.active.firstOrNull()
    val primaryIndex = primary?.let { op -> active.events.indexOfFirst { it.id == op.event.id }.takeIf { it >= 0 } } ?: -1
    val previousEvent = active.events.lastOrNull { it.id in completed || it.id in skipped }
    var reviewingPrevious by remember(primary?.event?.id, completed, skipped) { mutableStateOf(false) }
    val shownEvent = if (reviewingPrevious) previousEvent else primary?.event
    val shownStep = shownEvent?.let { event -> active.cookingPlan?.steps?.firstOrNull { it.id == event.id } }
    val processed = completed.size + skipped.size

    when (spec.typographyProfile) {
        TypographyProfile.MODERN_SANS -> ModernActiveHero(shownEvent, shownStep, primaryIndex, total, processed, reviewingPrevious)
        TypographyProfile.PREMIUM_CINEMATIC -> PremiumActiveHero(shownEvent, shownStep, primaryIndex, total, processed, reviewingPrevious)
        TypographyProfile.APPLIANCE_CONTROL -> ApplianceActiveHero(shownEvent, shownStep, primaryIndex, total, processed, reviewingPrevious)
        TypographyProfile.WARM_EDITORIAL -> EditorialActiveHero(shownEvent, shownStep, primaryIndex, total, processed, reviewingPrevious)
        TypographyProfile.MINIMAL_PRO -> ProActiveHero(shownEvent, shownStep, primaryIndex, total, processed, reviewingPrevious)
    }

    Spacer(Modifier.height(10.dp))
    if (primary != null) {
        ActiveCookingTimerPanel(
            operation = primary,
            paused = state.status == CookingSessionStatus.PAUSED,
            onPause = onPause,
            onResume = onResume,
            onAddMinute = {
                val command = cookingAddMinuteCommand(primary.event.id)
                if (state.status == CookingSessionStatus.PAUSED) {
                    // Keep the visible paused state while routing the persisted timer mutation through the
                    // existing ViewModel callback path. Resume starts the controller, Complete applies the
                    // namespaced +1-minute command while RUNNING, and Pause immediately restores PAUSED.
                    onResume()
                    onComplete(command)
                    onPause()
                } else {
                    onComplete(command)
                }
            },
            onEnd = onEnd
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { reviewingPrevious = !reviewingPrevious },
                enabled = previousEvent != null,
                modifier = Modifier.heightIn(min = 44.dp).weight(1f)
            ) {
                Text(
                    if (reviewingPrevious) {
                        if (L.isTr) "Şimdiki adıma dön" else "Current step"
                    } else {
                        if (L.isTr) "Önceki adım" else "Previous"
                    },
                    color = if (previousEvent != null) colors.onSurfaceSub else colors.divider,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { onComplete(primary.event.id) },
                enabled = !reviewingPrevious,
                modifier = Modifier.height(44.dp).weight(1.15f),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                shape = RoundedCornerShape(referenceActiveRadius(spec))
            ) { Text(if (L.isTr) "Tamamla" else "Complete", color = colors.onPrimary, fontSize = 12.sp) }
            TextButton(
                onClick = { onSkip(primary.event.id) },
                enabled = !reviewingPrevious,
                modifier = Modifier.heightIn(min = 44.dp).weight(.8f)
            ) { Text(if (L.isTr) "Atla" else "Skip", color = colors.onSurfaceSub, fontSize = 12.sp) }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp,
            border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceActiveRadius(spec))
        ) {
            Text(
                if (L.isTr) "Sıradaki adımın başlaması bekleniyor." else "Waiting for the next scheduled step.",
                color = colors.onSurfaceSub,
                modifier = Modifier.padding(14.dp)
            )
        }
    }

    if (state.active.size > 1) {
        Spacer(Modifier.height(10.dp))
        Text(if (L.isTr) "Aynı anda" else "At the same time", color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
        state.active.drop(1).forEach { operation ->
            Spacer(Modifier.height(7.dp))
            ParallelOperationCard(operation, onComplete, onSkip)
        }
    }
    state.upcoming.firstOrNull()?.let { next ->
        Spacer(Modifier.height(10.dp))
        UpcomingStepCard(next)
    }
}

@Composable
private fun ModernActiveHero(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?, index: Int, total: Int, processed: Int, previous: Boolean) {
    val colors = LocalAppColors.current
    Column {
        ActiveStepTopLine(index, total, processed, step)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(if (previous) activePreviousLabel() else activeNowLabel(), color = if (previous) colors.onSurfaceSub else colors.warn, style = androidx.compose.material.MaterialTheme.typography.overline)
                Spacer(Modifier.height(8.dp))
                Text(event?.instruction ?: activeWaitingLabel(), color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h5)
                ActiveTechniqueLine(event, step)
            }
        }
    }
}

@Composable
private fun PremiumActiveHero(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?, index: Int, total: Int, processed: Int, previous: Boolean) {
    val colors = LocalAppColors.current
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            ActiveStepTopLine(index, total, processed, step)
            Spacer(Modifier.height(14.dp))
            Divider(color = colors.border)
            Spacer(Modifier.height(14.dp))
            Text(if (previous) activePreviousLabel() else activeNowLabel(), color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline)
            Spacer(Modifier.height(8.dp))
            Text(event?.instruction ?: activeWaitingLabel(), color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h4)
            ActiveTechniqueLine(event, step)
        }
    }
}

@Composable
private fun ApplianceActiveHero(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?, index: Int, total: Int, processed: Int, previous: Boolean) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(13.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(if (previous) "HISTORY" else "ACTIVE STEP", color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline, modifier = Modifier.weight(1f))
            Text(if (index >= 0) "%02d / %02d".format(index + 1, total) else "$processed / $total", color = colors.success, style = androidx.compose.material.MaterialTheme.typography.overline)
        }
        Spacer(Modifier.height(8.dp))
        Text(event?.instruction ?: activeWaitingLabel(), color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h5)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActiveTelemetryChip(event?.resource?.let { cookingResourceLabel(it, L.isTr) } ?: "—", colors.warn)
            step?.targetTemperatureC?.let { ActiveTelemetryChip("$it°C", colors.success) }
            step?.powerLevel?.let { ActiveTelemetryChip(if (L.isTr) "SEVİYE $it" else "LEVEL $it", colors.primary) }
        }
    }
}

@Composable
private fun EditorialActiveHero(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?, index: Int, total: Int, processed: Int, previous: Boolean) {
    val colors = LocalAppColors.current
    Column {
        Text(if (index >= 0) (if (L.isTr) "Adım ${index + 1} / $total" else "Step ${index + 1} of $total") else "$processed / $total", color = colors.warn, style = androidx.compose.material.MaterialTheme.typography.caption)
        Spacer(Modifier.height(7.dp))
        Divider(color = colors.border)
        Spacer(Modifier.height(12.dp))
        Text(if (previous) activePreviousLabel() else activeNowLabel(), color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline)
        Spacer(Modifier.height(7.dp))
        Text(event?.instruction ?: activeWaitingLabel(), color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h2)
        ActiveTechniqueLine(event, step)
        Spacer(Modifier.height(12.dp))
        Divider(color = colors.border)
    }
}

@Composable
private fun ProActiveHero(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?, index: Int, total: Int, processed: Int, previous: Boolean) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (previous) "PREVIOUS" else "COOKING NOW", color = colors.warn, style = androidx.compose.material.MaterialTheme.typography.overline, modifier = Modifier.weight(1f))
            Text(if (index >= 0) "${index + 1}/$total" else "$processed/$total", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(event?.instruction ?: activeWaitingLabel(), color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h6)
        ActiveTechniqueLine(event, step)
    }
}

@Composable
private fun ActiveStepTopLine(index: Int, total: Int, processed: Int, step: com.agentickitchen.shared.ai.dto.CookingStepDto?) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (index >= 0) {
                if (L.isTr) "Adım ${index + 1} / $total" else "Step ${index + 1} of $total"
            } else {
                if (L.isTr) "$processed / $total tamamlandı" else "$processed / $total processed"
            },
            color = colors.warn,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        val meta = listOfNotNull(
            step?.resource?.let { cookingResourceLabel(it, L.isTr) },
            step?.powerLevel?.let { if (L.isTr) "Seviye $it" else "Level $it" },
            step?.targetTemperatureC?.let { "$it°C" }
        ).joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
    }
}

@Composable
private fun ActiveTechniqueLine(event: ScheduleEvent?, step: com.agentickitchen.shared.ai.dto.CookingStepDto?) {
    val colors = LocalAppColors.current
    val details = listOfNotNull(
        event?.resource?.let { cookingResourceLabel(it, L.isTr) },
        step?.targetTemperatureC?.let { "$it°C" },
        step?.powerLevel?.let { if (L.isTr) "Güç $it" else "Power $it" }
    ).joinToString(" · ")
    if (details.isNotBlank()) {
        Spacer(Modifier.height(9.dp))
        Text(details, color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
    }
}

@Composable
private fun ActiveTelemetryChip(label: String, accent: Color) {
    Text(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp))
}

@Composable
private fun ActiveCookingTimerPanel(
    operation: LiveOperation,
    paused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddMinute: () -> Unit,
    onEnd: () -> Unit
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val remaining = formatActiveDuration(operation.remainingSeconds)
    val timerShape = RoundedCornerShape(referenceActiveRadius(spec))

    when (spec.typographyProfile) {
        TypographyProfile.PREMIUM_CINEMATIC -> Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = timerShape) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(176.dp).border(5.dp, colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(remaining, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 44.sp)
                        Text(if (L.isTr) "KALAN" else "REMAINING", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.overline)
                    }
                }
                Spacer(Modifier.height(14.dp))
                TimerControls(paused, onPause, onResume, onAddMinute, onEnd)
            }
        }
        TypographyProfile.APPLIANCE_CONTROL -> Column(Modifier.fillMaxWidth().background(colors.surface, timerShape).border(1.dp, colors.border, timerShape).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TIMER / REMAINING", color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline)
                    Text(remaining, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 54.sp, letterSpacing = 1.sp)
                }
                Text(if (paused) "PAUSED" else "RUNNING", color = if (paused) colors.warn else colors.success, style = androidx.compose.material.MaterialTheme.typography.overline)
            }
            Spacer(Modifier.height(8.dp))
            TimerControls(paused, onPause, onResume, onAddMinute, onEnd)
        }
        TypographyProfile.WARM_EDITORIAL -> Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface2, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = timerShape) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (L.isTr) "Pişirme süresi" else "Cooking timer", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
                Text(remaining, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h1, fontSize = 62.sp)
                Text(if (L.isTr) "tahmini kalan süre" else "estimated time remaining", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
                Spacer(Modifier.height(12.dp))
                TimerControls(paused, onPause, onResume, onAddMinute, onEnd)
            }
        }
        TypographyProfile.MINIMAL_PRO -> Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = timerShape) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (L.isTr) "KALAN SÜRE" else "TIME REMAINING", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.overline)
                        Text(remaining, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 48.sp)
                    }
                    Text(if (paused) "PAUSED" else "LIVE", color = if (paused) colors.warn else colors.success, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                TimerControls(paused, onPause, onResume, onAddMinute, onEnd)
            }
        }
        else -> Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface2, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = timerShape) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(remaining, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 64.sp)
                Text(if (L.isTr) "kalan süre" else "time remaining", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
                Spacer(Modifier.height(12.dp))
                TimerControls(paused, onPause, onResume, onAddMinute, onEnd)
            }
        }
    }
}

@Composable
private fun TimerControls(paused: Boolean, onPause: () -> Unit, onResume: () -> Unit, onAddMinute: () -> Unit, onEnd: () -> Unit) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = if (paused) onResume else onPause,
            modifier = Modifier.height(48.dp).weight(1.6f),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
            shape = RoundedCornerShape(referenceActiveRadius(spec))
        ) { Text(if (paused) { if (L.isTr) "Devam Et" else "Resume" } else { if (L.isTr) "Duraklat" else "Pause" }, color = colors.onPrimary) }
        OutlinedButton(
            onClick = onAddMinute,
            modifier = Modifier.height(48.dp).weight(.9f),
            border = BorderStroke(1.dp, colors.border),
            shape = RoundedCornerShape(referenceActiveRadius(spec))
        ) { Text("+1 min", color = colors.onSurface) }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onEnd,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        border = BorderStroke(1.dp, colors.danger.copy(alpha = .7f)),
        shape = RoundedCornerShape(referenceActiveRadius(spec))
    ) { Text(if (L.isTr) "Pişirmeyi Bitir" else "End Cooking", color = colors.danger) }
}

@Composable
private fun ParallelOperationCard(operation: LiveOperation, onComplete: (String) -> Unit, onSkip: (String) -> Unit) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    Row(Modifier.fillMaxWidth().border(1.dp, colors.border, RoundedCornerShape(referenceActiveRadius(spec))).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(formatActiveDuration(operation.remainingSeconds), color = colors.primary, fontWeight = FontWeight.Bold)
            Text(operation.event.instruction, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.body2)
        }
        TextButton(onClick = { onComplete(operation.event.id) }) { Text(if (L.isTr) "Tamamla" else "Done", color = colors.primary) }
        TextButton(onClick = { onSkip(operation.event.id) }) { Text(if (L.isTr) "Atla" else "Skip", color = colors.onSurfaceSub) }
    }
}

@Composable
private fun UpcomingStepCard(event: ScheduleEvent) {
    val colors = LocalAppColors.current
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceActiveRadius(LocalThemeSpec.current))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (L.isTr) "SIRADAKİ" else "UP NEXT", color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline)
            Spacer(Modifier.width(10.dp))
            Text(event.instruction, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReferenceActiveAssistant(
    active: PlanState.RecipeActive,
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

    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.aiBg, elevation = 0.dp, border = BorderStroke(1.dp, colors.ai.copy(alpha = .35f)), shape = RoundedCornerShape(referenceActiveRadius(spec))) {
        Column(Modifier.padding(if (spec.dense) 12.dp else 14.dp)) {
            Text(if (L.isTr) "Mutfak Asistanı" else "Kitchen Assistant", color = colors.ai, style = androidx.compose.material.MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Text(if (L.isTr) "Pişirirken sor." else "Ask while you cook.", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(1.dp, if (focused) colors.ai else colors.border, RoundedCornerShape(12.dp)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused }.padding(vertical = 12.dp),
                    textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    decorationBox = { inner ->
                        Box {
                            if (question.isBlank()) Text(if (L.isTr) "Örn: Sos çok koyu oldu…" else "E.g. The sauce is too thick…", color = colors.onSurfaceSub, fontSize = 13.sp)
                            inner()
                        }
                    }
                )
                TextButton(onClick = ::submit, enabled = question.isNotBlank(), modifier = Modifier.heightIn(min = 44.dp)) { Text(if (L.isTr) "Gönder" else "Send", color = colors.ai) }
            }
            active.agentChatResponse?.let { response ->
                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(response, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearChat) { Text(if (L.isTr) "Temizle" else "Clear", color = colors.onSurfaceSub) }
                }
            }
            Spacer(Modifier.height(11.dp))
            Divider(color = colors.border)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (L.isTr) "Tavayı kontrol et" else "Check the pan", color = colors.onSurface, fontWeight = FontWeight.Bold)
                    Text(if (L.isTr) "Fotoğrafla pişirme durumunu değerlendir." else "Take a photo to assess how cooking is progressing.", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
                }
                TextButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.semantics { contentDescription = if (L.isTr) "Tava fotoğrafı çek" else "Take pan photo" }) { Text(if (L.isTr) "Fotoğraf" else "Take photo", color = colors.primary) }
            }
            active.visionScanResponse?.let { response ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(response, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearVision) { Text(if (L.isTr) "Temizle" else "Clear", color = colors.onSurfaceSub) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceActiveKitchenSummary(pantryIntel: PantryIntelReport, hardwareSettings: HardwareSettings, selectedEquipment: Set<String>) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val stove = if (hardwareSettings.stoveType.lowercase(Locale.ROOT) == "gas") {
        if (L.isTr) "Gaz" else "Gas"
    } else {
        if (L.isTr) "Elektrikli" else "Electric"
    }
    val notes = (pantryIntel.warnings + pantryIntel.tactics).take(2)
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceActiveRadius(spec))) {
        Column(Modifier.padding(if (spec.dense) 12.dp else 14.dp)) {
            Text(if (L.isTr) "Mutfak özeti" else "Kitchen summary", color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(if (L.isTr) "Ocak: $stove · ${selectedEquipment.size} araç hazır" else "Stove: $stove · ${selectedEquipment.size} tools ready", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
            notes.forEach { note ->
                Spacer(Modifier.height(5.dp))
                Text("• ${pantrySignalText(note)}", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun ReferenceCompletedCooking(active: PlanState.RecipeActive, state: CookingSessionState, onBackToOptions: () -> Unit) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val completed = cookingVisibleCompleted(state.completed).size
    val skipped = cookingVisibleSkipped(state.skipped).size
    Card(modifier = Modifier.fillMaxWidth(), backgroundColor = colors.surface, elevation = 0.dp, border = BorderStroke(1.dp, colors.border), shape = RoundedCornerShape(referenceActiveRadius(spec))) {
        Column(Modifier.padding(if (spec.dense) 16.dp else 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IngredientArtwork(active.recipe.name, Modifier.size(if (spec.dense) 96.dp else 118.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (state.status == CookingSessionStatus.COMPLETED) {
                    if (L.isTr) "Pişirme tamamlandı" else "Cooking completed"
                } else {
                    if (L.isTr) "Pişirme sonlandırıldı" else "Cooking ended"
                },
                color = colors.onSurface,
                style = androidx.compose.material.MaterialTheme.typography.h2,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(5.dp))
            Text(active.recipe.name, color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.h6, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                if (L.isTr) "$completed tamamlandı · $skipped atlandı · ${formatActiveDuration(state.elapsedSeconds)}" else "$completed completed · $skipped skipped · ${formatActiveDuration(state.elapsedSeconds)}",
                color = colors.onSurfaceSub,
                style = androidx.compose.material.MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
            if (state.status == CookingSessionStatus.COMPLETED) {
                Spacer(Modifier.height(7.dp))
                Text(if (L.isTr) "Afiyet olsun." else "Enjoy your meal.", color = colors.success, style = androidx.compose.material.MaterialTheme.typography.body1)
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBackToOptions, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (L.isTr) "Tariflere dön" else "Back to recipes", color = colors.primary)
            }
        }
    }
}

@Composable
private fun ReferenceConsumptionDialog(
    pending: PendingConsumption,
    inventory: List<PantryStockItem>,
    onUsePlanned: () -> Unit,
    onUseActual: (Map<String, Double>) -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    var actual by remember(pending.sessionId) { mutableStateOf(pending.usages.associate { it.itemId to formatActiveQuantity(it.plannedQuantity) }) }
    val parsed = actual.mapValues { (_, value) -> value.replace(',', '.').toDoubleOrNull() }
    val valid = parsed.values.all { it != null && it.isFinite() && it > 0.0 }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.92f)
                .background(colors.surface, RoundedCornerShape(referenceActiveRadius(spec)))
                .border(1.dp, colors.border, RoundedCornerShape(referenceActiveRadius(spec)))
                .verticalScroll(rememberScrollState())
                .padding(if (spec.dense) 16.dp else 20.dp)
        ) {
            Text(if (L.isTr) "MUTFAK STOĞU" else "KITCHEN INVENTORY", color = colors.primary, style = androidx.compose.material.MaterialTheme.typography.overline)
            Spacer(Modifier.height(4.dp))
            Text(if (L.isTr) "Planlanan miktarlar kullanıldı mı?" else "Were the planned amounts used?", color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.h4)
            Spacer(Modifier.height(12.dp))
            if (pending.usages.isEmpty()) {
                Text(if (L.isTr) "Bu tarif için ayrılmış stok yok." else "No pantry stock was reserved for this recipe.", color = colors.onSurfaceSub)
            }
            pending.usages.forEach { usage ->
                val name = inventory.firstOrNull { it.id == usage.itemId }?.originalName ?: usage.itemId
                val unit = localizedActiveUnit(usage.unit)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = colors.onSurface, style = androidx.compose.material.MaterialTheme.typography.body1)
                        Text(if (L.isTr) "Plan: ${formatActiveQuantity(usage.plannedQuantity)} $unit" else "Plan: ${formatActiveQuantity(usage.plannedQuantity)} $unit", color = colors.onSurfaceSub, style = androidx.compose.material.MaterialTheme.typography.caption)
                    }
                    OutlinedTextField(
                        value = actual[usage.itemId].orEmpty(),
                        onValueChange = { actual = actual + (usage.itemId to it) },
                        modifier = Modifier.width(104.dp),
                        label = { Text(unit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Divider(color = colors.border, modifier = Modifier.padding(vertical = 8.dp))
            }
            Button(onClick = onUsePlanned, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), shape = RoundedCornerShape(referenceActiveRadius(spec))) {
                Text(if (L.isTr) "Planlananı kullan" else "Use planned amounts", color = colors.onPrimary)
            }
            TextButton(onClick = { onUseActual(parsed.mapValues { requireNotNull(it.value) }) }, enabled = valid, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (L.isTr) "Gerçek miktarları uygula" else "Apply actual amounts", color = colors.primary)
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (L.isTr) "Stok tüketmeden iptal et" else "Cancel without consuming stock", color = colors.onSurfaceSub)
            }
        }
    }
}

private fun referenceActiveRadius(spec: ThemeSpec) = when (spec.typographyProfile) {
    TypographyProfile.MODERN_SANS -> 18.dp
    TypographyProfile.PREMIUM_CINEMATIC -> 18.dp
    TypographyProfile.APPLIANCE_CONTROL -> 10.dp
    TypographyProfile.WARM_EDITORIAL -> 22.dp
    TypographyProfile.MINIMAL_PRO -> 12.dp
}

private fun formatActiveDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, remainder) else "%02d:%02d".format(minutes, remainder)
}

private fun formatActiveQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')

private fun localizedActiveUnit(unit: String): String {
    val normalized = unit.trim().lowercase(Locale.ROOT).removeSuffix(".")
    return if (L.isTr) when (normalized) {
        "count", "piece", "pieces", "pcs", "adet" -> "adet"
        "tsp", "teaspoon", "teaspoons" -> "çay kaşığı"
        "tbsp", "tablespoon", "tablespoons" -> "yemek kaşığı"
        "cup", "cups" -> "su bardağı"
        else -> unit.trim()
    } else when (normalized) {
        "count", "adet", "piece", "pieces", "pcs" -> "piece"
        "teaspoon", "teaspoons" -> "tsp"
        "tablespoon", "tablespoons" -> "tbsp"
        else -> unit.trim()
    }
}

private fun activeNowLabel() = if (L.isTr) "ŞİMDİ PİŞİRİLİYOR" else "COOKING NOW"
private fun activePreviousLabel() = if (L.isTr) "ÖNCEKİ ADIM" else "PREVIOUS STEP"
private fun activeWaitingLabel() = if (L.isTr) "Sıradaki adım bekleniyor." else "Waiting for the next step."
