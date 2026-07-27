package com.agentickitchen.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.ALL_EQUIPMENT
import com.agentickitchen.android.CookingEquipment
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L

private val defaultSetupEquipment = setOf("oven", "elec")
private val mealTimePattern = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")

@Composable
fun SetupScreen(
    initialHw: HardwareSettings,
    initialEquipment: Set<String>,
    initialMealTime: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onComplete: (equipment: Set<String>, servings: Int, mealTime: String, hw: HardwareSettings) -> Unit
) {
    val validEquipment = remember(initialEquipment) {
        initialEquipment.filterTo(linkedSetOf()) { id -> ALL_EQUIPMENT.any { it.id == id } }
            .ifEmpty { defaultSetupEquipment }
    }
    val validMealTime = remember(initialMealTime) { initialMealTime.takeIf { mealTimePattern.matches(it) } ?: "19:00" }
    val initialTimeParts = remember(validMealTime) { validMealTime.split(":").map(String::toInt) }

    var selectedEquipment by remember(validEquipment) { mutableStateOf(validEquipment) }
    var servings by remember(initialHw) { mutableStateOf(initialHw.servingSize.coerceIn(1, 12)) }
    var hour by remember(validMealTime) { mutableStateOf(initialTimeParts[0]) }
    var minute by remember(validMealTime) { mutableStateOf(initialTimeParts[1]) }
    var stovePowerMax by remember(initialHw) { mutableStateOf(initialHw.stovePowerMax.coerceIn(3, 15)) }
    var ovenHasFan by remember(initialHw) { mutableStateOf(initialHw.ovenHasFan) }
    var ovenHasGrill by remember(initialHw) { mutableStateOf(initialHw.ovenHasGrill) }
    var contentVisible by remember { mutableStateOf(false) }
    val mealTime = "%02d:%02d".format(hour, minute)
    val colors = LocalAppColors.current

    LaunchedEffect(Unit) { contentVisible = true }
    BackHandler(enabled = canGoBack) { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 10 }
        ) {
            EditorialSetupHeader(canGoBack, onBack)
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(260, delayMillis = 80)) + slideInVertically(tween(260, delayMillis = 80)) { it / 12 }
        ) {
            EditorialSetupSection(number = "01", title = if (L.isTr) "Pişirme araçların" else "Cooking equipment") {
                EquipmentGrid(
                    selectedEquipment = selectedEquipment,
                    onToggle = { id ->
                        selectedEquipment = if (id in selectedEquipment) selectedEquipment - id else selectedEquipment + id
                    }
                )

                if (selectedEquipment.any { it == "elec" || it == "gas" }) {
                    Spacer(Modifier.height(20.dp))
                    StovePowerDetails(stovePowerMax) { stovePowerMax = it }
                }
                if ("oven" in selectedEquipment) {
                    Spacer(Modifier.height(20.dp))
                    OvenDetails(
                        ovenHasFan = ovenHasFan,
                        ovenHasGrill = ovenHasGrill,
                        onFanChange = { ovenHasFan = it },
                        onGrillChange = { ovenHasGrill = it }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(260, delayMillis = 140)) + slideInVertically(tween(260, delayMillis = 140)) { it / 12 }
        ) {
            EditorialSetupSection(number = "02", title = if (L.isTr) "Kaç kişilik?" else "How many people?") {
                ServingSelector(servings, onDecrease = { servings = (servings - 1).coerceAtLeast(1) }) {
                    servings = (servings + 1).coerceAtMost(12)
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(260, delayMillis = 200)) + slideInVertically(tween(260, delayMillis = 200)) { it / 12 }
        ) {
            EditorialSetupSection(
                number = "03",
                title = if (L.isTr) "Ne zaman hazır olsun?" else "When should it be ready?"
            ) {
                Text(
                    if (L.isTr) "Yemek genellikle ne zaman hazır olsun?" else "When should meals usually be ready?",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body1
                )
                Spacer(Modifier.height(18.dp))
                MealTimeSelector(
                    hour = hour,
                    minute = minute,
                    onHourDown = { hour = (hour + 23) % 24 },
                    onHourUp = { hour = (hour + 1) % 24 },
                    onMinuteDown = { minute = (minute + 45) % 60 },
                    onMinuteUp = { minute = (minute + 15) % 60 }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val updatedHw = initialHw.copy(
                    stoveType = if ("gas" in selectedEquipment) "gas" else "electric",
                    ovenAvailable = "oven" in selectedEquipment,
                    stovePowerMax = stovePowerMax,
                    ovenHasFan = ovenHasFan,
                    ovenHasGrill = ovenHasGrill,
                    servingSize = servings
                )
                onComplete(selectedEquipment, servings, mealTime, updatedHw)
            },
            enabled = selectedEquipment.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(54.dp)
                .semantics {
                    contentDescription = if (L.isTr) {
                        if (canGoBack) "Değişiklikleri Kaydet" else "Mutfağımı Kaydet"
                    } else {
                        if (canGoBack) "Save Changes" else "Save My Kitchen"
                    }
                },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primary,
                disabledBackgroundColor = colors.divider
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                if (canGoBack) {
                    if (L.isTr) "Değişiklikleri Kaydet" else "Save Changes"
                } else {
                    if (L.isTr) "Mutfağımı Kaydet" else "Save My Kitchen"
                },
                color = colors.onPrimary,
                style = MaterialTheme.typography.button
            )
        }
    }
}

@Composable
private fun EditorialSetupHeader(canGoBack: Boolean, onBack: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 36.dp)) {
        if (canGoBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = if (L.isTr) "Geri" else "Back" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("Agentic Kitchen", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) "Mutfağını tanıyalım" else "Let’s get to know your kitchen",
            color = colors.onSurface,
            style = MaterialTheme.typography.h1
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (L.isTr) "Birkaç seçimle tarifleri mutfağına göre uyarlayalım." else "A few choices will tailor every recipe to your kitchen.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun EditorialSetupSection(
    number: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Divider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(number, color = colors.primary, style = MaterialTheme.typography.h6)
            Spacer(Modifier.width(14.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6)
        }
        Spacer(Modifier.height(18.dp))
        content()
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun EquipmentGrid(selectedEquipment: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ALL_EQUIPMENT.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { equipment ->
                    EditorialEquipmentItem(
                        equipment = equipment,
                        selected = equipment.id in selectedEquipment,
                        modifier = Modifier.weight(1f),
                        onToggle = { onToggle(equipment.id) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditorialEquipmentItem(
    equipment: CookingEquipment,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription = if (L.isTr) {
                    "${equipment.label}, ${if (selected) "seçili" else "seçili değil"}"
                } else {
                    "${equipment.label}, ${if (selected) "selected" else "not selected"}"
                }
            }
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(equipment.icon, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    equipment.label,
                    color = if (selected) colors.onSurface else colors.onSurfaceSub,
                    style = MaterialTheme.typography.body1,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .8f),
                    exit = fadeOut(tween(160))
                ) {
                    Text("✓", color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(9.dp))
            Divider(color = if (selected) colors.primary else colors.divider, thickness = if (selected) 2.dp else 1.dp)
        }
    }
}

@Composable
private fun StovePowerDetails(stovePowerMax: Int, onPowerChange: (Int) -> Unit) {
    val colors = LocalAppColors.current
    Divider(color = colors.divider, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(if (L.isTr) "Ocak güç aralığı" else "Stove power range", color = colors.onSurface, style = MaterialTheme.typography.subtitle1)
            Spacer(Modifier.height(4.dp))
            Text(if (L.isTr) "Tariflerde kullanılacak en yüksek seviye." else "The highest level recipes may use.", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        }
        Text("$stovePowerMax / 15", color = colors.primary, style = MaterialTheme.typography.h6)
    }
    Slider(
        value = stovePowerMax.toFloat(),
        onValueChange = { onPowerChange(it.toInt()) },
        valueRange = 3f..15f,
        steps = 11,
        colors = SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.divider
        )
    )
}

@Composable
private fun OvenDetails(
    ovenHasFan: Boolean,
    ovenHasGrill: Boolean,
    onFanChange: (Boolean) -> Unit,
    onGrillChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    Divider(color = colors.divider, thickness = 1.dp)
    Spacer(Modifier.height(8.dp))
    EditorialSwitchRow(
        title = if (L.isTr) "Fanlı pişirme" else "Convection fan",
        checked = ovenHasFan,
        onCheckedChange = onFanChange
    )
    Divider(color = colors.divider, thickness = 1.dp)
    EditorialSwitchRow(
        title = if (L.isTr) "Izgara özelliği" else "Grill function",
        checked = ovenHasGrill,
        onCheckedChange = onGrillChange
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EditorialSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primaryLight)
        )
    }
}

@Composable
private fun ServingSelector(servings: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        SetupAdjustButton(
            label = "−",
            enabled = servings > 1,
            description = if (L.isTr) "Porsiyonu azalt" else "Decrease servings",
            onClick = onDecrease
        )
        Spacer(Modifier.width(34.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
            Crossfade(targetState = servings, animationSpec = tween(180), label = "servings") { value ->
                Text("$value", color = colors.onSurface, style = MaterialTheme.typography.h1, textAlign = TextAlign.Center)
            }
            Text(if (L.isTr) "kişi" else "people", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        }
        Spacer(Modifier.width(34.dp))
        SetupAdjustButton(
            label = "+",
            enabled = servings < 12,
            description = if (L.isTr) "Porsiyonu artır" else "Increase servings",
            onClick = onIncrease
        )
    }
}

@Composable
private fun MealTimeSelector(
    hour: Int,
    minute: Int,
    onHourDown: () -> Unit,
    onHourUp: () -> Unit,
    onMinuteDown: () -> Unit,
    onMinuteUp: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("%02d:%02d".format(hour, minute), color = colors.onSurface, style = MaterialTheme.typography.h1)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TimeAdjuster(
                value = "%02d".format(hour),
                label = if (L.isTr) "Saat" else "Hour",
                upDescription = if (L.isTr) "Saati artır" else "Increase hour",
                downDescription = if (L.isTr) "Saati azalt" else "Decrease hour",
                onUp = onHourUp,
                onDown = onHourDown
            )
            TimeAdjuster(
                value = "%02d".format(minute),
                label = if (L.isTr) "Dakika" else "Minute",
                upDescription = if (L.isTr) "Dakikayı artır" else "Increase minute",
                downDescription = if (L.isTr) "Dakikayı azalt" else "Decrease minute",
                onUp = onMinuteUp,
                onDown = onMinuteDown
            )
        }
    }
}

@Composable
private fun TimeAdjuster(
    value: String,
    label: String,
    upDescription: String,
    downDescription: String,
    onUp: () -> Unit,
    onDown: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SetupAdjustButton("↑", true, upDescription, onUp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = colors.onSurface, style = MaterialTheme.typography.h6)
        Text(label, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(4.dp))
        SetupAdjustButton("↓", true, downDescription, onDown)
    }
}

@Composable
private fun SetupAdjustButton(label: String, enabled: Boolean, description: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, if (enabled) colors.divider else colors.divider.copy(alpha = .45f), RoundedCornerShape(12.dp))
            .semantics { contentDescription = description }
    ) {
        Text(label, color = if (enabled) colors.primary else colors.onSurfaceSub, fontSize = 22.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun FirstRunSetupPreview() {
    AgenticTheme("editorial") {
        SetupScreen(
            initialHw = HardwareSettings(),
            initialEquipment = emptySet(),
            initialMealTime = "",
            canGoBack = false,
            onBack = {},
            onComplete = { _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditingSetupPreview() {
    AgenticTheme("editorial") {
        SetupScreen(
            initialHw = HardwareSettings(stoveType = "gas", servingSize = 4, ovenAvailable = true, ovenHasFan = true),
            initialEquipment = setOf("gas", "oven", "pan"),
            initialMealTime = "20:30",
            canGoBack = true,
            onBack = {},
            onComplete = { _, _, _, _ -> }
        )
    }
}
