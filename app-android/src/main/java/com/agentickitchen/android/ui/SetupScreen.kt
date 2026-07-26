package com.agentickitchen.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.ALL_EQUIPMENT
import com.agentickitchen.android.CookingEquipment
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L

@Composable
fun SetupScreen(
    initialHw: HardwareSettings,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onComplete: (equipment: Set<String>, servings: Int, mealTime: String, hw: HardwareSettings) -> Unit
) {
    var selectedEquipment by remember { mutableStateOf(setOf("oven", "elec")) }
    var servings by remember { mutableStateOf(initialHw.servingSize) }
    var mealTime by remember { mutableStateOf("19:00") }
    
    // Hardware Details
    var stovePowerMax by remember { mutableStateOf(initialHw.stovePowerMax) }
    var ovenHasFan by remember { mutableStateOf(initialHw.ovenHasFan) }
    var ovenHasGrill by remember { mutableStateOf(initialHw.ovenHasGrill) }

    val colors = LocalAppColors.current

    BackHandler(enabled = canGoBack) { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getBgGradient())
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canGoBack) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(horizontalAlignment = Alignment.Start) {
                    Text("🍳", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(L.setupTitle, color = colors.onSurface, style = MaterialTheme.typography.h1, fontSize = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Sadece bir kere ayarla, her seferinde akıllı plan al.", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                }
            }
        }

        // ── Ekipman Seçimi ───────────────────────────────────────────
        SetupSection(number = "1", title = L.setupSubtitle, colors = colors) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ALL_EQUIPMENT.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { eq ->
                            EquipmentCard(
                                equipment = eq,
                                selected = selectedEquipment.contains(eq.id),
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onToggle = {
                                    selectedEquipment = if (selectedEquipment.contains(eq.id)) selectedEquipment - eq.id else selectedEquipment + eq.id
                                }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            
            // Detailed Settings for selected equipment
            if (selectedEquipment.contains("elec") || selectedEquipment.contains("gas")) {
                Spacer(Modifier.height(16.dp))
                Card(backgroundColor = colors.surface, shape = RoundedCornerShape(12.dp), elevation = 0.dp, border = BorderStroke(1.dp, colors.divider)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Ocak Maksimum Güç Seviyesi: $stovePowerMax", color = colors.onSurface, style = MaterialTheme.typography.body1)
                        Slider(value = stovePowerMax.toFloat(), onValueChange = { stovePowerMax = it.toInt() }, valueRange = 3f..15f, steps = 11, colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary))
                    }
                }
            }
            if (selectedEquipment.contains("oven")) {
                Spacer(Modifier.height(8.dp))
                Card(backgroundColor = colors.surface, shape = RoundedCornerShape(12.dp), elevation = 0.dp, border = BorderStroke(1.dp, colors.divider)) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Fırında Fan (Turbo) var mı?", color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                            Switch(checked = ovenHasFan, onCheckedChange = { ovenHasFan = it }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary))
                        }
                        Divider(color = colors.divider, modifier = Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Fırında Izgara (Grill) var mı?", color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                            Switch(checked = ovenHasGrill, onCheckedChange = { ovenHasGrill = it }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Porsiyon ─────────────────────────────────────────────────
        SetupSection(number = "2", title = L.setupServings, colors = colors) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (servings > 1) colors.primaryDark else colors.divider)
                        .clickable { if (servings > 1) servings-- },
                    contentAlignment = Alignment.Center
                ) { Text("−", color = if (servings > 1) colors.onPrimary else colors.onSurfaceSub, fontSize = 22.sp, fontWeight = FontWeight.Bold) }

                Spacer(Modifier.width(24.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$servings", color = colors.onSurface, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text(L.persons, color = colors.onSurfaceSub, fontSize = 13.sp)
                }
                Spacer(Modifier.width(24.dp))

                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (servings < 12) colors.primary else colors.divider)
                        .clickable { if (servings < 12) servings++ },
                    contentAlignment = Alignment.Center
                ) { Text("+", color = colors.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Saat ─────────────────────────────────────────────────────
        SetupSection(number = "3", title = L.setupTime, colors = colors) {
            val parts = mealTime.split(":")
            var hour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 19) }
            var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

            LaunchedEffect(hour, minute) { mealTime = "%02d:%02d".format(hour, minute) }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TimeSpinner(value = hour, onUp = { hour = (hour + 1) % 24 }, onDown = { hour = (hour + 23) % 24 }, label = if (L.isTr) "Saat" else "Hour", colors = colors)
                Text(" : ", color = colors.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                TimeSpinner(value = minute, onUp = { minute = (minute + 15) % 60 }, onDown = { minute = (minute + 45) % 60 }, label = if (L.isTr) "Dakika" else "Min", format = { "%02d".format(it) }, colors = colors)
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Başla Butonu ─────────────────────────────────────────────
        Button(
            onClick = {
                val updatedHw = initialHw.copy(
                    stoveType = if (selectedEquipment.contains("gas")) "gas" else "electric",
                    ovenAvailable = selectedEquipment.contains("oven"),
                    stovePowerMax = stovePowerMax,
                    ovenHasFan = ovenHasFan,
                    ovenHasGrill = ovenHasGrill,
                    servingSize = servings
                )
                onComplete(selectedEquipment, servings, mealTime, updatedHw)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = if (selectedEquipment.isNotEmpty()) colors.primary else colors.divider),
            elevation = ButtonDefaults.elevation(8.dp),
            enabled = selectedEquipment.isNotEmpty()
        ) {
            if (canGoBack) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(L.save, color = colors.onPrimary, style = MaterialTheme.typography.button, fontSize = 17.sp)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = colors.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text(L.setupStart, color = colors.onPrimary, style = MaterialTheme.typography.button, fontSize = 17.sp)
            }
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun SetupSection(number: String, title: String, colors: AppColors, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).background(colors.primaryLight, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Text(number, color = colors.background, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            Spacer(Modifier.width(12.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun EquipmentCard(equipment: CookingEquipment, selected: Boolean, colors: AppColors, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val bgColor = if (selected) colors.primaryDark else colors.surface
    val borderColor = if (selected) colors.primary else colors.divider

    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)).clickable { onToggle() }.padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(equipment.icon, fontSize = 28.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                equipment.label, color = if (selected) colors.onPrimary else colors.onSurfaceSub,
                fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center, maxLines = 2
            )
            if (selected) {
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primaryLight, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TimeSpinner(value: Int, onUp: () -> Unit, onDown: () -> Unit, label: String, colors: AppColors, format: (Int) -> String = { it.toString() }) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface).clickable { onUp() },
            contentAlignment = Alignment.Center
        ) { Text("▲", color = colors.primaryLight, fontSize = 14.sp) }
        Spacer(Modifier.height(8.dp))
        Text(format(value), color = colors.onSurface, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.onSurfaceSub, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface).clickable { onDown() },
            contentAlignment = Alignment.Center
        ) { Text("▼", color = colors.primaryLight, fontSize = 14.sp) }
    }
}
