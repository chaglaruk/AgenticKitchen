package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L
import com.agentickitchen.android.PendingConsumption
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.cooking.CookingSessionState
import com.agentickitchen.shared.cooking.CookingSessionStatus
import com.agentickitchen.shared.cooking.LiveOperation
import com.agentickitchen.shared.ai.dto.CookingStepDto
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.models.ScheduleEvent
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onCompleteCookingStep: (String) -> Unit, onSkipCookingStep: (String) -> Unit, onEndCooking: () -> Unit,
    pendingConsumption: PendingConsumption? = null,
    inventory: List<PantryStockItem> = emptyList(),
    onConsumePlanned: () -> Unit = {},
    onConsumeActual: (Map<String, Double>) -> Unit = {},
    onCancelConsumption: () -> Unit = {}
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
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        EditorialBrandLockup()
        Spacer(Modifier.height(20.dp))
        val recipeName = cookingState.recipeName.ifBlank {
            (planState as? PlanState.RecipeActive)?.recipe?.name.orEmpty()
        }
        if (shouldShowCookingPanel(cookingState.status, planState is PlanState.RecipeActive)) {
            EditorialCookingHeader(recipeName)
            Spacer(Modifier.height(18.dp))
            EditorialLiveCooking(
                state = cookingState,
                recipeName = recipeName,
                activePlan = planState as? PlanState.RecipeActive,
                onStart = onStartCooking,
                onPause = onPauseCooking,
                onResume = onResumeCooking,
                onComplete = onCompleteCookingStep,
                onSkip = onSkipCookingStep,
                onEnd = onEndCooking
            )
            Spacer(Modifier.height(18.dp))
        }
        when (planState) {
            is PlanState.RecipeActive -> {
                Spacer(Modifier.height(24.dp))
                KitchenAssistantSection(
                    state = planState,
                    onAskAgent = onAskAgent,
                    onClearChat = onClearChat,
                    onCheckPan = onCheckPan,
                    onClearVision = onClearVision
                )
                Spacer(Modifier.height(24.dp))
                KitchenSummary(pantryIntel, hardwareSettings, selectedEquipment)
                Spacer(Modifier.height(12.dp))
                BackToRecipesAction(onBackToOptions)
            }

            is PlanState.Error -> EditorialOperationsError(planState.message, onBackToOptions)
            else -> EditorialIdleOperations(onBackToOptions)
        }
    }
    pendingConsumption?.let {
        ConsumptionConfirmationDialog(
            pending = it,
            inventory = inventory,
            onUsePlanned = onConsumePlanned,
            onUseActual = onConsumeActual,
            onCancel = onCancelConsumption
        )
    }
}

internal fun formatCookingDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, remainder) else "%02d:%02d".format(minutes, remainder)
}

internal fun localizedPlanUnit(unit: String, isTurkish: Boolean): String {
    val normalized = unit.trim().lowercase(Locale.ROOT).removeSuffix(".")
    return if (isTurkish) {
        when (normalized) {
            "count", "adet", "piece", "pieces", "pcs" -> "adet"
            "clove", "cloves", "diş", "dis" -> "diş"
            "slice", "slices", "dilim" -> "dilim"
            "pinch", "pinches", "tutam" -> "tutam"
            "package", "packages", "pack", "packs", "paket" -> "paket"
            "bunch", "bunches", "demet" -> "demet"
            "tsp", "teaspoon", "teaspoons", "çay kaşığı", "cay kasigi" -> "çay kaşığı"
            "tbsp", "tablespoon", "tablespoons", "yemek kaşığı", "yemek kasigi" -> "yemek kaşığı"
            "cup", "cups", "bardak", "su bardağı", "su bardagi" -> "su bardağı"
            "unit", "units", "birim" -> "birim"
            else -> unit.trim()
        }
    } else {
        when (normalized) {
            "count", "adet", "piece", "pieces", "pcs" -> "piece"
            "clove", "cloves", "diş", "dis" -> "clove"
            "slice", "slices", "dilim" -> "slice"
            "pinch", "pinches", "tutam" -> "pinch"
            "package", "packages", "pack", "packs", "paket" -> "package"
            "bunch", "bunches", "demet" -> "bunch"
            "tsp", "teaspoon", "teaspoons", "çay kaşığı", "cay kasigi" -> "tsp"
            "tbsp", "tablespoon", "tablespoons", "yemek kaşığı", "yemek kasigi" -> "tbsp"
            "cup", "cups", "bardak", "su bardağı", "su bardagi" -> "cup"
            "unit", "units", "birim" -> "unit"
            else -> unit.trim()
        }
    }
}

@Composable
private fun ConsumptionConfirmationDialog(
    pending: PendingConsumption,
    inventory: List<PantryStockItem>,
    onUsePlanned: () -> Unit,
    onUseActual: (Map<String, Double>) -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalAppColors.current
    var actual by remember(pending.sessionId) {
        mutableStateOf(pending.usages.associate { it.itemId to formatPlanQuantity(it.plannedQuantity) })
    }
    val parsed = actual.mapValues { (_, value) -> value.replace(',', '.').toDoubleOrNull() }
    val valid = parsed.values.all { it != null && it.isFinite() && it > 0.0 }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.92f)
                .background(colors.surface, RoundedCornerShape(20.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(if (L.isTr) "MUTFAK STOĞU" else "KITCHEN INVENTORY", color = colors.primary, style = MaterialTheme.typography.overline)
            Text(
                if (L.isTr) "Planlanan miktarlar kullanıldı mı?" else "Were the planned amounts used?",
                color = colors.onSurface,
                style = MaterialTheme.typography.h4
            )
            Spacer(Modifier.height(12.dp))
            pending.usages.forEach { usage ->
                val name = inventory.firstOrNull { it.id == usage.itemId }?.originalName ?: usage.itemId
                val displayUnit = localizedPlanUnit(usage.unit, L.isTr)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = colors.onSurface, style = MaterialTheme.typography.body1)
                        Text(
                            "Plan: ${formatPlanQuantity(usage.plannedQuantity)} $displayUnit",
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.caption
                        )
                    }
                    OutlinedTextField(
                        value = actual[usage.itemId].orEmpty(),
                        onValueChange = { actual = actual + (usage.itemId to it) },
                        modifier = Modifier.width(104.dp),
                        label = { Text(displayUnit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                Divider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
            }
            Button(
                onClick = onUsePlanned,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Planlananı kullan" else "Use planned amounts", color = colors.onPrimary)
            }
            TextButton(
                onClick = { onUseActual(parsed.mapValues { requireNotNull(it.value) }) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(if (L.isTr) "Gerçek miktarları uygula" else "Apply actual amounts", color = colors.primary)
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (L.isTr) "Stok tüketmeden iptal et" else "Cancel without consuming stock", color = colors.onSurfaceSub)
            }
        }
    }
}

@Composable
private fun EditorialCookingHeader(recipeName: String) {
    val colors = LocalAppColors.current
    Column {
        Text(
            if (recipeName.isBlank()) {
                if (L.isTr) "Pişirmeye hazır" else "Ready to cook"
            } else {
                if (L.isTr) "Şimdi pişiriyoruz" else "Now cooking"
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.caption
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (recipeName.isBlank()) {
                if (L.isTr) "Bir tarif seçerek başlayabilirsin." else "Choose a recipe to begin."
            } else {
                recipeName
            },
            color = colors.onSurface,
            style = MaterialTheme.typography.h1
        )
    }
}

@Composable
private fun EditorialLiveCooking(
    state: CookingSessionState,
    recipeName: String,
    activePlan: PlanState.RecipeActive?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit,
    onEnd: () -> Unit
) {
    val colors = LocalAppColors.current
    val total = state.completed.size + state.skipped.size + state.active.size + state.upcoming.size
    val processed = state.completed.size + state.skipped.size

    Column(modifier = Modifier.fillMaxWidth()) {
        state.error?.let {
            Text(it, color = androidx.compose.ui.graphics.Color(0xFF9B3F32), style = MaterialTheme.typography.body1)
            Spacer(Modifier.height(12.dp))
        }
        when (state.status) {
            CookingSessionStatus.READY -> ReadyCookingState(recipeName, activePlan, onStart)
            CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED -> ActiveCookingState(
                state = state,
                total = total,
                processed = processed,
                onComplete = onComplete,
                onSkip = onSkip
            )
            CookingSessionStatus.COMPLETED, CookingSessionStatus.ENDED -> TerminalCookingState(state, recipeName, total)
            CookingSessionStatus.ERROR -> ErrorCookingState(recipeName)
        }
        if (state.status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            Spacer(Modifier.height(20.dp))
            GlobalCookingControls(state.status, onPause, onResume, onEnd)
        } else if (state.status == CookingSessionStatus.ERROR) {
            Spacer(Modifier.height(16.dp))
            GlobalCookingControls(state.status, onPause, onResume, onEnd, onStart)
        }
    }
}

@Composable
private fun ReadyCookingState(
    recipeName: String,
    activePlan: PlanState.RecipeActive?,
    onStart: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        IngredientArtwork(recipeName, Modifier.size(108.dp))
        Spacer(Modifier.height(12.dp))
        Text(if (L.isTr) "Tarif hazır." else "The recipe is ready.", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(6.dp))
        Text(
            if (L.isTr) "Adımları başlatmaya hazırsın." else "Start when you are ready.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        activePlan?.cookingPlan?.let { plan ->
            Spacer(Modifier.height(28.dp))
            PlanReview(activePlan)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(if (L.isTr) "Pişirmeye Başla" else "Start Cooking", color = colors.onPrimary)
        }
    }
}

@Composable
private fun PlanReview(active: PlanState.RecipeActive) {
    val colors = LocalAppColors.current
    val plan = active.cookingPlan ?: return
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            if (L.isTr) "PİŞİRME PLANI" else "COOKING PLAN",
            color = colors.primary,
            style = MaterialTheme.typography.caption
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (L.isTr) "${active.servings} kişilik · ${formatReadyTime(active.resolvedReadyTimeIso)} hazır"
            else "Serves ${active.servings} · ready at ${formatReadyTime(active.resolvedReadyTimeIso)}",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(24.dp))
        PlanReviewSectionTitle(if (L.isTr) "Malzemeler" else "Ingredients")
        plan.ingredients.forEach { ingredient ->
            EditorialPlanRow(
                leading = "•",
                title = ingredient.name,
                detail = "${formatPlanQuantity(ingredient.quantity)} ${localizedPlanUnit(ingredient.unit, L.isTr)}"
            )
        }
        if (active.plannedUsage.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            PlanReviewSectionTitle(if (L.isTr) "Stoktan ayrılacak" else "Planned pantry use")
            active.plannedUsage.forEach { usage ->
                val displayUnit = localizedPlanUnit(usage.unit, L.isTr)
                EditorialPlanRow(
                    leading = "−",
                    title = usage.itemName,
                    detail = if (L.isTr) {
                        "Mevcut ${formatPlanQuantity(usage.currentQuantity)} $displayUnit · kullanılacak ${formatPlanQuantity(usage.plannedQuantity)} $displayUnit · kalacak ${formatPlanQuantity(usage.remainingQuantity)} $displayUnit"
                    } else {
                        "Current ${formatPlanQuantity(usage.currentQuantity)} $displayUnit · planned ${formatPlanQuantity(usage.plannedQuantity)} $displayUnit · remaining ${formatPlanQuantity(usage.remainingQuantity)} $displayUnit"
                    }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PlanReviewSectionTitle(if (L.isTr) "Adımlar" else "Steps")
        plan.steps.forEachIndexed { index, step ->
            EditorialPlanStep(index + 1, step)
        }
        if (plan.safetyNotes.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            PlanReviewSectionTitle(if (L.isTr) "Güvenlik notları" else "Safety notes")
            plan.safetyNotes.forEach { note ->
                EditorialPlanRow(leading = "•", title = note, detail = null)
            }
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = colors.divider, thickness = 1.dp)
    }
}

@Composable
private fun PlanReviewSectionTitle(title: String) {
    Text(
        title,
        color = LocalAppColors.current.onSurface,
        style = MaterialTheme.typography.h6
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EditorialPlanRow(leading: String, title: String, detail: String?) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top
    ) {
        Text(leading, color = colors.primary, modifier = Modifier.width(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.body1)
            detail?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
        }
    }
}

internal fun cookingDependencyLabel(dependencyCount: Int, isTurkish: Boolean): String? = when {
    dependencyCount == 1 -> if (isTurkish) "Önceki adımın ardından" else "After the previous step"
    dependencyCount > 1 -> if (isTurkish) "Önceki adımlar tamamlanınca" else "After the previous steps"
    else -> null
}

@Composable
private fun EditorialPlanStep(number: Int, step: CookingStepDto) {
    val meta = buildList {
        add(cookingResourceLabel(step.resource, L.isTr))
        add(formatCookingDuration(step.durationSeconds.toLong()))
        step.targetTemperatureC?.let { add("$it°C") }
        step.powerLevel?.let { add(if (L.isTr) "Seviye $it" else "Level $it") }
        cookingDependencyLabel(step.dependsOn.size, L.isTr)?.let(::add)
    }.joinToString(" · ")
    EditorialPlanRow(
        leading = number.toString().padStart(2, '0'),
        title = step.instruction,
        detail = meta
    )
}

internal fun formatPlanQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) quantity.toLong().toString()
    else String.format(Locale.ROOT, "%.2f", quantity).trimEnd('0').trimEnd('.')

internal fun formatReadyTime(readyTimeIso: String): String = runCatching {
    ZonedDateTime.parse(readyTimeIso).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(if (L.isTr) "belirsiz" else "unspecified")

@Composable
private fun ActiveCookingState(
    state: CookingSessionState,
    total: Int,
    processed: Int,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit
) {
    val primary = state.active.firstOrNull()
    CookingProgressLedger(state, total, processed)
    Spacer(Modifier.height(18.dp))
    if (state.status == CookingSessionStatus.PAUSED) {
        Text(if (L.isTr) "DURAKLATILDI" else "PAUSED", color = LocalAppColors.current.primary, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(8.dp))
    }
    if (primary != null) {
        AnimatedContent(
            targetState = primary.event.id,
            transitionSpec = { fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 } togetherWith fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 10 } },
            label = "activeCookingStep"
        ) { operationId ->
            val operation = state.active.firstOrNull { it.event.id == operationId } ?: primary
            PrimaryCookingOperation(operation, paused = state.status == CookingSessionStatus.PAUSED, onComplete, onSkip)
        }
        if (state.active.size > 1) {
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Aynı anda" else "At the same time", color = LocalAppColors.current.onSurfaceSub, style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(8.dp))
            state.active.drop(1).forEach { operation ->
                ParallelCookingOperation(operation, onComplete, onSkip)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
    if (state.upcoming.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        UpcomingCookingOperations(state.upcoming)
    }
}

@Composable
private fun CookingProgressLedger(state: CookingSessionState, total: Int, processed: Int) {
    val colors = LocalAppColors.current
    val remaining = state.active.size + state.upcoming.size
    val fraction = if (total == 0) 0f else processed.toFloat() / total
    Column {
        Text(
            if (L.isTr) "$processed / $total adım" else "$processed / $total steps",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.caption
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(1f).height(2.dp).background(colors.divider)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth(fraction).height(2.dp).background(colors.primary)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) {
                "${state.completed.size} tamamlandı · ${state.skipped.size} atlandı · $remaining kaldı"
            } else {
                "${state.completed.size} completed · ${state.skipped.size} skipped · $remaining remaining"
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun PrimaryCookingOperation(operation: LiveOperation, paused: Boolean, onComplete: (String) -> Unit, onSkip: (String) -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(targetState = formatCookingDuration(operation.remainingSeconds), label = "primaryCountdown") { value ->
            Text(
                value,
                color = colors.onSurface.copy(alpha = if (paused) .55f else 1f),
                style = MaterialTheme.typography.h1,
                fontSize = 72.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(operation.event.instruction, color = colors.onSurface, style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(10.dp))
        Text(CookingResourceLabel(operation.event.resource), color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onComplete(operation.event.id) },
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                shape = RoundedCornerShape(999.dp)
            ) { Text(if (L.isTr) "Tamamla" else "Complete", color = colors.onPrimary) }
            TextButton(onClick = { onSkip(operation.event.id) }, modifier = Modifier.height(48.dp)) {
                Text(if (L.isTr) "Atla" else "Skip", color = colors.onSurfaceSub)
            }
        }
    }
}

@Composable
private fun ParallelCookingOperation(operation: LiveOperation, onComplete: (String) -> Unit, onSkip: (String) -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, colors.divider, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(targetState = formatCookingDuration(operation.remainingSeconds), label = "parallelCountdown") { value ->
                Text(value, color = colors.primary, style = MaterialTheme.typography.h6)
            }
            Spacer(Modifier.height(4.dp))
            Text(operation.event.instruction, color = colors.onSurface, style = MaterialTheme.typography.body1)
            Spacer(Modifier.height(4.dp))
            Text(CookingResourceLabel(operation.event.resource), color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
            TextButton(onClick = { onComplete(operation.event.id) }, modifier = Modifier.height(48.dp)) { Text(if (L.isTr) "Tamamla" else "Complete", color = colors.primary) }
            TextButton(onClick = { onSkip(operation.event.id) }, modifier = Modifier.height(48.dp)) { Text(if (L.isTr) "Atla" else "Skip", color = colors.onSurfaceSub) }
        }
    }
}

@Composable
private fun UpcomingCookingOperations(upcoming: List<ScheduleEvent>) {
    val colors = LocalAppColors.current
    Column {
        Text(if (L.isTr) "Sıradaki" else "Up next", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        upcoming.take(2).forEachIndexed { index, event ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.width(2.dp).height(34.dp).background(colors.divider))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(event.instruction, color = colors.onSurface, style = MaterialTheme.typography.body1)
                    Text(CookingResourceLabel(event.resource), color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                }
            }
            if (index < upcoming.take(2).lastIndex) Divider(color = colors.divider, thickness = 1.dp)
        }
    }
}

@Composable
private fun GlobalCookingControls(
    status: CookingSessionStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onStart: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        when (status) {
            CookingSessionStatus.RUNNING -> Button(onClick = onPause, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), shape = RoundedCornerShape(999.dp)) {
                Text(if (L.isTr) "Duraklat" else "Pause", color = colors.onPrimary)
            }
            CookingSessionStatus.PAUSED -> Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), shape = RoundedCornerShape(999.dp)) {
                Text(if (L.isTr) "Devam Et" else "Resume", color = colors.onPrimary)
            }
            CookingSessionStatus.ERROR -> Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), shape = RoundedCornerShape(999.dp)) {
                Text(if (L.isTr) "Pişirmeye Başla" else "Start Cooking", color = colors.onPrimary)
            }
            else -> Unit
        }
        if (status in setOf(CookingSessionStatus.RUNNING, CookingSessionStatus.PAUSED)) {
            TextButton(onClick = onEnd, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally).height(48.dp)) {
                Text(if (L.isTr) "Pişirmeyi Bitir" else "End Cooking", color = colors.onSurfaceSub)
            }
        }
    }
}

@Composable
private fun TerminalCookingState(state: CookingSessionState, recipeName: String, total: Int) {
    val colors = LocalAppColors.current
    Column {
        Text(
            if (state.status == CookingSessionStatus.COMPLETED) {
                if (L.isTr) "Afiyet olsun." else "Enjoy your meal."
            } else {
                if (L.isTr) "Pişirme bitti." else "Cooking ended."
            },
            color = colors.onSurface,
            style = MaterialTheme.typography.h3
        )
        Spacer(Modifier.height(8.dp))
        Text(recipeName, color = colors.primary, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.status == CookingSessionStatus.COMPLETED) {
                if (L.isTr) "Pişirme adımları tamamlandı." else "The cooking steps are complete."
            } else {
                if (L.isTr) "Pişirme erken sonlandırıldı." else "Cooking was ended early."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (L.isTr) "${state.completed.size} tamamlandı · ${state.skipped.size} atlandı · ${formatCookingDuration(state.elapsedSeconds)}" else "${state.completed.size} completed · ${state.skipped.size} skipped · ${formatCookingDuration(state.elapsedSeconds)}",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun ErrorCookingState(recipeName: String) {
    val colors = LocalAppColors.current
    Text(
        if (recipeName.isBlank()) {
            if (L.isTr) "Pişirmeyi başlatmak için bir tarif seç." else "Choose a recipe before starting to cook."
        } else {
            if (L.isTr) "Pişirmeye yeniden başlayabilirsin." else "You can start cooking again."
        },
        color = colors.onSurfaceSub,
        style = MaterialTheme.typography.body1
    )
}

private fun CookingResourceLabel(resource: String): String =
    cookingResourceLabel(resource, L.isTr)

internal fun cookingResourceLabel(resource: String, isTurkish: Boolean): String = when (resource.lowercase()) {
    "counter" -> if (isTurkish) "TEZGAH" else "COUNTER"
    "stove", "stovetop" -> if (isTurkish) "OCAK" else "STOVE"
    "oven" -> if (isTurkish) "FIRIN" else "OVEN"
    "airfryer" -> if (isTurkish) "HAVA FRİTÖZÜ" else "AIR FRYER"
    "pan" -> if (isTurkish) "TAVA" else "PAN"
    "pot" -> if (isTurkish) "TENCERE" else "POT"
    "bowl" -> if (isTurkish) "KASE" else "BOWL"
    "microwave" -> if (isTurkish) "MİKRODALGA" else "MICROWAVE"
    "fridge" -> if (isTurkish) "BUZDOLABI" else "FRIDGE"
    "cutting_board" -> if (isTurkish) "KESME TAHTASI" else "CUTTING BOARD"
    "baking_tray" -> if (isTurkish) "FIRIN TEPSİSİ" else "BAKING TRAY"
    else -> if (isTurkish) "MUTFAK ALANI" else "KITCHEN AREA"
}

@Composable
private fun KitchenAssistantSection(
    state: PlanState.RecipeActive,
    onAskAgent: (String) -> Unit,
    onClearChat: () -> Unit,
    onCheckPan: (android.graphics.Bitmap) -> Unit,
    onClearVision: () -> Unit
) {
    val colors = LocalAppColors.current
    var question by remember { mutableStateOf("") }
    var inputFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? -> if (bitmap != null) onCheckPan(bitmap) }
    fun submitQuestion() {
        val trimmed = question.trim()
        if (trimmed.isNotEmpty()) {
            onAskAgent(trimmed)
            question = ""
            focusManager.clearFocus(force = true)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize(tween(240))) {
        Text(if (L.isTr) "Mutfak Asistanı" else "Kitchen Assistant", color = colors.onSurface, style = MaterialTheme.typography.h3)
        Spacer(Modifier.height(8.dp))
        Text(
            if (L.isTr) "Pişirirken bir şey danışabilirsin." else "Ask while you cook.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surfaceAlt, RoundedCornerShape(14.dp))
                .border(1.dp, if (inputFocused) colors.primary else colors.divider, RoundedCornerShape(14.dp))
                .padding(start = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            BasicTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.weight(1f).onFocusChanged { inputFocused = it.isFocused }.padding(vertical = 14.dp),
                textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitQuestion() }),
                singleLine = false,
                decorationBox = { inner ->
                    Box {
                        if (question.isBlank()) Text(
                            if (L.isTr) "Örn: Sos çok koyu oldu, ne yapmalıyım?" else "E.g. The sauce is too thick. What should I do?",
                            color = colors.onSurfaceSub,
                            fontSize = 14.sp
                        )
                        inner()
                    }
                }
            )
            IconButton(onClick = ::submitQuestion, enabled = question.isNotBlank(), modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (L.isTr) "Soruyu gönder" else "Send question",
                    tint = if (question.isNotBlank()) colors.primary else colors.divider
                )
            }
        }
        AnimatedVisibility(
            visible = state.agentChatResponse != null,
            enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 10 },
            exit = fadeOut(tween(200))
        ) {
            state.agentChatResponse?.let { response ->
                Spacer(Modifier.height(16.dp))
                AssistantResponse(response, onClearChat)
            }
        }
        Spacer(Modifier.height(24.dp))
        Divider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.height(20.dp))
        Text(if (L.isTr) "Tavayı kontrol et" else "Check the pan", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(6.dp))
        Text(
            if (L.isTr) "Fotoğraf çekerek pişirme durumunu değerlendirebilirsin." else "Take a photo to assess how the cooking is progressing.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Filled.CameraAlt, contentDescription = if (L.isTr) "Kamerayı aç" else "Open camera", tint = colors.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (L.isTr) "Tavayı kontrol et" else "Check the pan", color = colors.primary)
        }
        AnimatedVisibility(
            visible = state.visionScanResponse != null,
            enter = fadeIn(tween(240)) + scaleIn(tween(240), initialScale = .98f),
            exit = fadeOut(tween(200))
        ) {
            state.visionScanResponse?.let { response ->
                Spacer(Modifier.height(12.dp))
                PanCheckResponse(response, onClearVision)
            }
        }
    }
}

@Composable
private fun AssistantResponse(response: String, onClear: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surfaceAlt, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (L.isTr) "Asistanın notu" else "Assistant note", color = colors.primary, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Asistan notunu temizle" else "Clear assistant response", tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
            }
        }
        Text(response, color = colors.onSurface, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun PanCheckResponse(response: String, onClear: () -> Unit) {
    val colors = LocalAppColors.current
    val isError = response.contains("hata", ignoreCase = true) || response.contains("error", ignoreCase = true) || response.contains("başarısız", ignoreCase = true)
    val accent = if (isError) androidx.compose.ui.graphics.Color(0xFF9B3F32) else colors.success
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.surfaceAlt, RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(12.dp)).padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (L.isTr) "Tava kontrolü" else "Pan check", color = accent, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f))
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Tava kontrolünü temizle" else "Clear pan-check response", tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
            }
        }
        Text(response, color = colors.onSurface, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun KitchenSummary(pantryIntel: PantryIntelReport, hardwareSettings: HardwareSettings, selectedEquipment: Set<String>) {
    val colors = LocalAppColors.current
    val stove = if (hardwareSettings.stoveType == "gas") {
        if (L.isTr) "Gaz" else "Gas"
    } else {
        if (L.isTr) "Elektrikli" else "Electric"
    }
    val notes = (pantryIntel.warnings + pantryIntel.tactics).take(2)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(if (L.isTr) "Mutfak özeti" else "Kitchen summary", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) "Ocak: $stove" else "Stove: $stove",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Text(
            if (L.isTr) "${selectedEquipment.size} ekipman hazır" else "${selectedEquipment.size} tools ready",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        notes.forEach { note ->
            Spacer(Modifier.height(8.dp))
            Divider(color = colors.divider, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            Text("• ${pantrySignalText(note)}", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
private fun BackToRecipesAction(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onBack,
        modifier = Modifier
            .size(width = 150.dp, height = 48.dp)
            .semantics { contentDescription = if (L.isTr) "Tariflere dön" else "Back to recipes" }
    ) {
        Text(if (L.isTr) "Tariflere dön" else "Back to recipes", color = colors.primary)
    }
}

@Composable
private fun EditorialIdleOperations(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        IngredientArtwork("", Modifier.size(100.dp))
        Spacer(Modifier.height(14.dp))
        Text(if (L.isTr) "Henüz pişirilen bir tarif yok." else "Nothing is cooking yet.", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text(
            if (L.isTr) "Tarifler bölümünden bir tarif seçerek başlayabilirsin." else "Choose a recipe from Recipes to begin.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text(if (L.isTr) "Tariflere Git" else "Browse Recipes", color = colors.primary) }
    }
}

@Composable
private fun EditorialOperationsError(message: String, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(if (L.isTr) "Bir sorun oluştu." else "Something went wrong.", color = androidx.compose.ui.graphics.Color(0xFF9B3F32), style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(8.dp))
        Text(message, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        Spacer(Modifier.height(12.dp))
        BackToRecipesAction(onBack)
    }
}

private fun previewCookingEvent(id: String, instruction: String, resource: String) = ScheduleEvent(
    id = id,
    startIso = "2026-07-27T18:00:00Z",
    endIso = "2026-07-27T18:05:00Z",
    instruction = instruction,
    resource = resource
)

@Preview(showBackground = true)
@Composable
private fun ReadyCookingPreview() = CookingPreview(
    CookingSessionState(recipeName = "Kremalı Tavuklu Makarna")
)

@Preview(showBackground = true)
@Composable
private fun RunningCookingPreview() = CookingPreview(
    CookingSessionState(
        recipeName = "Kremalı Tavuklu Makarna",
        status = CookingSessionStatus.RUNNING,
        active = listOf(LiveOperation(previewCookingEvent("cream", "Kremayı ekle ve ateşi azalt.", "stovetop"), 272)),
        upcoming = listOf(previewCookingEvent("onion", "Soğanları ekle.", "stovetop")),
        completed = setOf("pasta"),
        elapsedSeconds = 180
    )
)

@Preview(showBackground = true)
@Composable
private fun ParallelCookingPreview() = CookingPreview(
    CookingSessionState(
        recipeName = "Kremalı Tavuklu Makarna",
        status = CookingSessionStatus.RUNNING,
        active = listOf(
            LiveOperation(previewCookingEvent("cream", "Kremayı ekle ve ateşi azalt.", "stovetop"), 272),
            LiveOperation(previewCookingEvent("pasta", "Makarnayı süz.", "stovetop"), 90)
        ),
        upcoming = listOf(previewCookingEvent("serve", "Tabağa al.", "stovetop")),
        completed = setOf("onion"),
        elapsedSeconds = 180
    )
)

@Preview(showBackground = true)
@Composable
private fun PausedCookingPreview() = CookingPreview(
    CookingSessionState(
        recipeName = "Kremalı Tavuklu Makarna",
        status = CookingSessionStatus.PAUSED,
        active = listOf(LiveOperation(previewCookingEvent("cream", "Kremayı ekle ve ateşi azalt.", "stovetop"), 272)),
        upcoming = listOf(previewCookingEvent("serve", "Tabağa al.", "stovetop")),
        elapsedSeconds = 180
    )
)

@Preview(showBackground = true)
@Composable
private fun CompletedCookingPreview() = CookingPreview(
    CookingSessionState(
        recipeName = "Kremalı Tavuklu Makarna",
        status = CookingSessionStatus.COMPLETED,
        completed = setOf("pasta", "cream"),
        skipped = setOf("onion"),
        elapsedSeconds = 1_265
    )
)

@Composable
private fun CookingPreview(state: CookingSessionState) {
    AgenticTheme("editorial") {
        EditorialLiveCooking(state, state.recipeName, null, {}, {}, {}, {}, {}, {})
    }
}

@Composable
private fun AssistantToolsPreview(active: PlanState.RecipeActive) {
    AgenticTheme("editorial") {
        KitchenAssistantSection(active, {}, {}, {}, {})
    }
}

private fun previewActiveRecipe(
    assistantResponse: String? = null,
    panResponse: String? = null
) = PlanState.RecipeActive(
    recipe = RecipeOption(
        id = "cream-pasta",
        type = "Makarna",
        name = "Kremalı Tavuklu Makarna",
        description = "Tavuk ve taze otlarla hazırlanan sıcak bir makarna."
    ),
    events = listOf(previewCookingEvent("cream", "Kremayı ekle ve ateşi azalt.", "stovetop")),
    agentChatResponse = assistantResponse,
    visionScanResponse = panResponse
)

@Preview(showBackground = true)
@Composable
private fun AssistantToolsEmptyPreview() = AssistantToolsPreview(previewActiveRecipe())

@Preview(showBackground = true)
@Composable
private fun AssistantToolsResponsePreview() = AssistantToolsPreview(
    previewActiveRecipe(assistantResponse = "Sosu biraz sıcak suyla açıp iki dakika daha karıştır.")
)

@Preview(showBackground = true)
@Composable
private fun PanCheckPreview() = AssistantToolsPreview(
    previewActiveRecipe(panResponse = "Isı dengeli görünüyor; ara sıra karıştırmaya devam et.")
)

@Preview(showBackground = true)
@Composable
private fun IdleOperationsPreview() {
    AgenticTheme("editorial") {
        EditorialIdleOperations {}
    }
}
