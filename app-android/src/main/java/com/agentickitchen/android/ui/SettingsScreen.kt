package com.agentickitchen.android.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L

@Composable
fun SettingsScreen(
    hw: HardwareSettings,
    diet: DietSettings,
    theme: String,
    notificationsEnabled: Boolean,
    language: String,
    selectedEquipment: Set<String>,
    mealTime: String,
    onSaveHardware: (HardwareSettings) -> Unit,
    onSaveDiet: (DietSettings) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onEditSetup: () -> Unit
) {
    var showHwDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDietDialog by remember { mutableStateOf(false) }

    val colors = LocalAppColors.current
    val activeThemeSpec = themeSpec(theme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getBgGradient())
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column {
                Text("⚙️ ${L.settings}", color = colors.onSurface, style = MaterialTheme.typography.h1)
                Text(
                    if (L.isTr) "Uygulama tercihlerini yönet" else "Manage your preferences",
                    color = colors.onSurfaceSub, style = MaterialTheme.typography.body1, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ── Mutfak Kurulumu ────────────────────────────────────────────
        SettingsSectionHeader(title = if (L.isTr) "Mutfak" else "Kitchen", colors = colors)

        SettingsItemClickable(
            icon = Icons.Filled.Restaurant, iconTint = colors.accent,
            title = if (L.isTr) "Pişirme Araçları" else "Cooking Equipment",
            subtitle = "${selectedEquipment.size} ${if (L.isTr) "araç seçili" else "items selected"} • $mealTime",
            colors = colors, onClick = onEditSetup
        )

        SettingsItemClickable(
            icon = Icons.Filled.Build, iconTint = colors.primaryLight,
            title = L.hardwareProfile,
            subtitle = "${if (hw.stoveType == "gas") "Gaz ocak" else "Elektrik ocak"} • ${hw.servingSize} ${L.persons}",
            colors = colors, onClick = { showHwDialog = true }
        )

        Spacer(Modifier.height(8.dp))

        // ── Kişisel ───────────────────────────────────────────────────
        SettingsSectionHeader(title = if (L.isTr) "Kişisel Tercihler" else "Personal Preferences", colors = colors)

        SettingsItemClickable(
            icon = Icons.Filled.LocalDining, iconTint = Color(0xFFEF4444),
            title = L.dietary,
            subtitle = if (diet.dietType == "none") (if (L.isTr) "Kısıtlama yok" else "No restrictions") else diet.dietType.replaceFirstChar { it.uppercase() },
            colors = colors, onClick = { showDietDialog = true }
        )

        Spacer(Modifier.height(8.dp))

        // ── Uygulama ──────────────────────────────────────────────────
        SettingsSectionHeader(title = L.app, colors = colors)

        SettingsItemToggle(
            icon = Icons.Filled.Notifications, iconTint = Color(0xFF7C83FD),
            title = L.notifications, subtitle = L.notifSubtitle,
            checked = notificationsEnabled, onCheckedChange = onToggleNotifications, colors = colors
        )

        SettingsItemClickable(
            icon = Icons.Filled.Language, iconTint = colors.primaryLight,
            title = L.language, subtitle = language,
            colors = colors, onClick = { showLangDialog = true }
        )

        SettingsItemClickable(
            icon = Icons.Filled.Palette, iconTint = colors.accent,
            title = L.theme, subtitle = activeThemeSpec.title,
            colors = colors, onClick = { showThemeDialog = true }
        )

        ThemeShowcaseStrip(currentTheme = theme, onOpen = { showThemeDialog = true })

        SettingsItemInfo(
            icon = Icons.Filled.Info, iconTint = colors.onSurfaceSub,
            title = L.version, subtitle = "Agentic Kitchen v1.10-beta", colors = colors
        )

        Spacer(Modifier.height(24.dp))

        // Yeniden kurulum butonu
        OutlinedButton(
            onClick = onEditSetup,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colors.divider),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (L.isTr) "Kurulumu Yeniden Yap" else "Redo Setup", color = colors.onSurfaceSub, style = MaterialTheme.typography.button)
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showHwDialog) {
        HardwareDialog(
            current = hw, colors = colors,
            onSave = { updated -> onSaveHardware(updated); showHwDialog = false },
            onDismiss = { showHwDialog = false }
        )
    }

    if (showLangDialog) {
        ListDialog(
            title = L.selectLanguage, current = language, options = listOf("Türkçe", "English", "Deutsch", "Français"),
            colors = colors, onSelect = { onSetLanguage(it); showLangDialog = false }, onDismiss = { showLangDialog = false }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            current = theme,
            onSelect = { onSetTheme(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showDietDialog) {
        DietDialog(
            current = diet, colors = colors,
            onSave = { updated -> onSaveDiet(updated); showDietDialog = false },
            onDismiss = { showDietDialog = false }
        )
    }
}

// ── Settings Item Components ──────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String, colors: AppColors) {
    Text(
        title.uppercase(), color = colors.primaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsItemClickable(icon: ImageVector, iconTint: Color, title: String, subtitle: String, colors: AppColors, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        backgroundColor = colors.surface, shape = RoundedCornerShape(16.dp), elevation = 0.dp, border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, fontSize = 16.sp)
                Text(subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceSub)
        }
    }
}

@Composable
fun SettingsItemToggle(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, colors: AppColors, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundColor = colors.surface, shape = RoundedCornerShape(16.dp), elevation = 0.dp, border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, fontSize = 16.sp)
                Text(subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.5f)))
        }
    }
}

@Composable
fun SettingsItemInfo(icon: ImageVector, iconTint: Color, title: String, subtitle: String, colors: AppColors) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        backgroundColor = colors.surface, shape = RoundedCornerShape(16.dp), elevation = 0.dp, border = BorderStroke(1.dp, colors.divider)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).background(iconTint.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, fontSize = 16.sp)
                Text(subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun ThemeShowcaseStrip(currentTheme: String, onOpen: () -> Unit) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onOpen() },
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (L.isTr) "Tema Galerisi" else "Theme Gallery", color = colors.onSurface, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeCatalog.take(3).forEach { spec ->
                    val selected = spec.id == currentTheme
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(spec.colors.surfaceAlt, RoundedCornerShape(16.dp))
                            .border(1.dp, if (selected) spec.colors.primary else colors.divider, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(spec.colors.primary, spec.colors.accent, spec.colors.heroStart).forEach { preview ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(preview, RoundedCornerShape(6.dp))
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(spec.title, color = spec.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────

@Composable
fun HardwareDialog(current: HardwareSettings, colors: AppColors, onSave: (HardwareSettings) -> Unit, onDismiss: () -> Unit) {
    var stoveType by remember { mutableStateOf(current.stoveType) }
    var ovenAvailable by remember { mutableStateOf(current.ovenAvailable) }
    var servingSize by remember { mutableStateOf(current.servingSize) }
    var powerLevel by remember { mutableStateOf(current.powerLevel) }
    var geminiKey by remember { mutableStateOf(current.geminiApiKey) }
    var hfKey by remember { mutableStateOf(current.hfApiKey) }
    var aiProvider by remember { mutableStateOf(current.aiProvider) }

    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = colors.surface, shape = RoundedCornerShape(24.dp), elevation = 16.dp) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(L.hardwareProfile, color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(20.dp))

                // AI Provider Selection
                Text("AI Model Sağlayıcı", color = colors.onSurfaceSub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "GEMINI" to "Google Gemini", 
                        "HUGGINGFACE" to "Hugging Face", 
                        "DUCKDUCKGO" to "DuckDuckGo (No-Key)",
                        "FREE" to "Bedava (No-Key)"
                    ).forEach { (key, label) ->
                        val selected = aiProvider == key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .border(1.dp, if (selected) colors.primary else colors.divider, RoundedCornerShape(12.dp))
                                .clickable { aiProvider = key }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected, onClick = { aiProvider = key },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.primary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = if (selected) colors.primary else colors.onSurface, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                if (aiProvider == "GEMINI") {
                    OutlinedTextField(
                        value = geminiKey, onValueChange = { geminiKey = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Gemini API Key", color = colors.onSurfaceSub) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = colors.onSurface, focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { clipboardManager.getText()?.text?.let { if (it.isNotBlank()) geminiKey = it } }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = colors.primary)
                            }
                        }
                    )
                    Text("aistudio.google.com adresinden alabilirsiniz.", color = colors.primaryLight, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp).clickable { uriHandler.openUri("https://aistudio.google.com/app/apikey") })
                } else if (aiProvider == "HUGGINGFACE") {
                    OutlinedTextField(
                        value = hfKey, onValueChange = { hfKey = it }, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Hugging Face Token", color = colors.onSurfaceSub) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = colors.onSurface, focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { clipboardManager.getText()?.text?.let { if (it.isNotBlank()) hfKey = it } }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = colors.primary)
                            }
                        }
                    )
                    Text("huggingface.co/settings/tokens adresinden alabilirsiniz.", color = colors.primaryLight, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp).clickable { uriHandler.openUri("https://huggingface.co/settings/tokens") })
                } else {
                    Box(modifier = Modifier.fillMaxWidth().background(colors.accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Column {
                            Text(
                                if (aiProvider == "DUCKDUCKGO") "DuckDuckGo üzerinden GPT-4o-mini kullanılır. Anahtar gerekmez."
                                else "Bedava modda anahtar gerekmez. (Pollinations.ai / Mistral-7B)", 
                                color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "⚠️ Görsel analiz (Vision) bu modlarda simüle edilir. Tam performans için Gemini API anahtarı önerilir.",
                                color = colors.onSurfaceSub, fontSize = 11.sp
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp), color = colors.divider)

                Text(L.stoveType, color = colors.onSurfaceSub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("electric" to L.electric, "gas" to L.gas).forEach { (key, label) ->
                        val selected = stoveType == key
                        OutlinedButton(
                            onClick = { stoveType = key },
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selected) colors.primary else colors.background),
                            border = BorderStroke(1.dp, if (selected) colors.primary else colors.divider),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(label, color = if (selected) colors.onPrimary else colors.onSurfaceSub, style = MaterialTheme.typography.button) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L.hasOven, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                    Switch(checked = ovenAvailable, onCheckedChange = { ovenAvailable = it }, colors = SwitchDefaults.colors(checkedThumbColor = colors.primary))
                }

                Spacer(Modifier.height(12.dp))
                Text("${L.serving}: $servingSize ${L.persons}", color = colors.onSurface, style = MaterialTheme.typography.body1)
                Slider(value = servingSize.toFloat(), onValueChange = { servingSize = it.toInt() }, valueRange = 1f..8f, steps = 6, colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary))

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(L.cancel, color = colors.onSurfaceSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onSave(current.copy(
                                stoveType = stoveType, 
                                ovenAvailable = ovenAvailable, 
                                servingSize = servingSize, 
                                powerLevel = powerLevel, 
                                geminiApiKey = geminiKey,
                                hfApiKey = hfKey,
                                aiProvider = aiProvider
                            )) 
                        }, 
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(L.save, color = colors.onPrimary, style = MaterialTheme.typography.button)
                    }
                }
            }
        }
    }
}

@Composable
fun DietDialog(current: DietSettings, colors: AppColors, onSave: (DietSettings) -> Unit, onDismiss: () -> Unit) {
    var dietType by remember { mutableStateOf(current.dietType) }
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = colors.surface, shape = RoundedCornerShape(24.dp), elevation = 16.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(L.dietary, color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(16.dp))
                listOf("none" to (if (L.isTr) "Kısıtlama Yok" else "None"), "vegetarian" to "Vegetarian", "vegan" to "Vegan", "keto" to "Keto").forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { dietType = key }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                        if (dietType == key) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary)
                    }
                    Divider(color = colors.divider)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(L.cancel, color = colors.onSurfaceSub) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(DietSettings(dietType, current.allergies)) }, colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary), shape = RoundedCornerShape(12.dp)) {
                        Text(L.save, color = colors.onPrimary, style = MaterialTheme.typography.button)
                    }
                }
            }
        }
    }
}

@Composable
fun ListDialog(title: String, current: String, options: List<String>, colors: AppColors, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = colors.surface, shape = RoundedCornerShape(24.dp), elevation = 16.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(16.dp))
                options.forEach { opt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(opt) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt.replaceFirstChar { it.uppercase() }, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                        if (opt.equals(current, ignoreCase = true)) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary)
                    }
                    Divider(color = colors.divider)
                }
            }
        }
    }
}

@Composable
fun ThemePickerDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(backgroundColor = colors.surface, shape = RoundedCornerShape(24.dp), elevation = 16.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(L.theme, color = colors.onSurface, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(16.dp))
                ThemeCatalog.forEach { spec ->
                    val selected = spec.id == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(spec.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(spec.colors.primary, spec.colors.accent, spec.colors.heroStart).forEach { preview ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(preview, RoundedCornerShape(6.dp))
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(spec.title, color = colors.onSurface, style = MaterialTheme.typography.body1, fontWeight = FontWeight.SemiBold)
                            Text(spec.subtitle, color = colors.onSurfaceSub, fontSize = 12.sp)
                        }
                        if (selected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = spec.colors.primary)
                        }
                    }
                    Divider(color = colors.divider)
                }
            }
        }
    }
}
