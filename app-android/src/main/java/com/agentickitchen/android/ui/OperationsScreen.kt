package com.agentickitchen.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus
import androidx.compose.material.Button

@Composable
fun OperationsScreen(
    planState: PlanState,
    pantryIntel: PantryIntelReport,
    hardwareSettings: HardwareSettings,
    selectedEquipment: Set<String>,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (android.graphics.Bitmap) -> Unit,
    onClearVision: () -> Unit,
    onBackToOptions: () -> Unit,
    cookingState: CookingSessionState,
    onStartCooking: () -> Unit, onPauseCooking: () -> Unit, onResumeCooking: () -> Unit,
    onCompleteCookingStep: (String) -> Unit, onSkipCookingStep: (String) -> Unit, onEndCooking: () -> Unit
) {
    val colors = LocalAppColors.current
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
            .background(getBgGradient())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        OperationsHero(pantryIntel = pantryIntel, equipmentCount = selectedEquipment.size)
        Spacer(Modifier.height(18.dp))
        LiveCookingCard(cookingState, onStartCooking, onPauseCooking, onResumeCooking, onCompleteCookingStep, onSkipCookingStep, onEndCooking)
        Spacer(Modifier.height(18.dp))
        OperationsTelemetryCard(
            pantryIntel = pantryIntel,
            hardwareSettings = hardwareSettings,
            selectedEquipment = selectedEquipment
        )
        Spacer(Modifier.height(18.dp))

        when (planState) {
            is PlanState.RecipeActive -> MilitaryRecipeCard(
                state = planState,
                colors = colors,
                onAskAgent = onAskAgent,
                onClearChat = onClearChat,
                onCheckPan = onCheckPan,
                onClearVision = onClearVision,
                onBack = onBackToOptions
            )

            is PlanState.Error -> ErrorCard(planState.message, colors)
            else -> IdleOperationsCard()
        }
    }
}

@Composable private fun LiveCookingCard(state: CookingSessionState, start: () -> Unit, pause: () -> Unit, resume: () -> Unit, complete: (String) -> Unit, skip: (String) -> Unit, end: () -> Unit) {
    val colors = LocalAppColors.current
    Card(Modifier.fillMaxWidth(), backgroundColor = colors.surface, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(20.dp)) {
        Text(state.recipeName.ifBlank { "Live Cooking" }, style = MaterialTheme.typography.h6); Text("${state.completed.size + state.skipped.size} steps done • ${state.elapsedSeconds}s")
        state.error?.let { Text(it, color = colors.accent) }
        state.active.forEach { op -> Text("${op.remainingSeconds}s • ${op.event.resource}\n${op.event.instruction}"); Row { Button({ complete(op.event.id) }) { Text("Complete Step") }; Button({ skip(op.event.id) }) { Text("Skip Step") } } }
        if (state.upcoming.isNotEmpty()) Text("Upcoming: ${state.upcoming.first().instruction}")
        Row { when (state.status) { CookingSessionStatus.READY, CookingSessionStatus.ERROR -> Button(start) { Text("Start Cooking") }; CookingSessionStatus.RUNNING -> Button(pause) { Text("Pause") }; CookingSessionStatus.PAUSED -> Button(resume) { Text("Resume") }; else -> Unit }; Button(end) { Text("End Cooking") } }
        if (state.completed.isNotEmpty()) Text("Completed: ${state.completed.joinToString()}"); if (state.skipped.isNotEmpty()) Text("Skipped: ${state.skipped.joinToString()}")
    } }
}

@Composable
private fun OperationsHero(pantryIntel: PantryIntelReport, equipmentCount: Int) {
    val colors = LocalAppColors.current
    val themeSpec = LocalThemeSpec.current
    val title = when (themeSpec.id) {
        "heritage" -> if (L.isTr) "Operasyon Defteri" else "Operations Ledger"
        "zen" -> if (L.isTr) "Canlı Operasyon" else "Live Operation"
        else -> if (L.isTr) "Signal Deck" else "Signal Deck"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surfaceAlt,
        shape = RoundedCornerShape(24.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                if (L.isTr) "LIVE OPS" else "LIVE OPS",
                color = colors.accent,
                style = MaterialTheme.typography.caption
            )
            Spacer(Modifier.height(8.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h1)
            Spacer(Modifier.height(10.dp))
            Text(
                if (L.isTr) {
                    "Skor ${pantryIntel.readinessScore}/100 • ${equipmentCount} ekipman hattı aktif."
                } else {
                    "Readiness ${pantryIntel.readinessScore}/100 • $equipmentCount equipment lanes active."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
private fun OperationsTelemetryCard(
    pantryIntel: PantryIntelReport,
    hardwareSettings: HardwareSettings,
    selectedEquipment: Set<String>
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                if (L.isTr) "Operasyon Telemetrisi" else "Operational Telemetry",
                color = colors.onSurface,
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TelemetryPill("${pantryIntel.readinessScore}/100", colors.primary)
                TelemetryPill(pantryCategoryLabel(pantryIntel.focusCategoryId), colors.accent)
                TelemetryPill(equipmentLaneLabel(pantryIntel.equipmentLane), colors.success)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (L.isTr) {
                    "Ocak ${hardwareSettings.stoveType} • Güç ${hardwareSettings.stovePowerMax} • ${selectedEquipment.size} ekipman"
                } else {
                    "Stove ${hardwareSettings.stoveType} • Power ${hardwareSettings.stovePowerMax} • ${selectedEquipment.size} tools"
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
            if (pantryIntel.warnings.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(if (L.isTr) "Kırmızı Bayraklar" else "Risk Flags", color = colors.onSurface, style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(8.dp))
                pantryIntel.warnings.take(2).forEach { warning ->
                    Text("• ${pantrySignalText(warning)}", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(if (L.isTr) "Takip Taktikleri" else "Tracking Tactics", color = colors.onSurface, style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(8.dp))
            pantryIntel.tactics.take(3).forEach { tactic ->
                Text("• ${pantrySignalText(tactic)}", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun IdleOperationsCard() {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                if (L.isTr) "Henüz canlı operasyon yok." else "No live operation yet.",
                color = colors.onSurface,
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (L.isTr) "Önce Seçenekler sekmesinden bir tarifi başlat." else "Launch a recipe from the Options tab first.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
private fun TelemetryPill(text: String, color: androidx.compose.ui.graphics.Color) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(colors.surfaceAlt, RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
