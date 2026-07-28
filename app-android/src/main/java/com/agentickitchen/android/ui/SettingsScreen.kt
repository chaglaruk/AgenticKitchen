package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agentickitchen.android.BuildConfig
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L

@Composable
fun SettingsScreen(
    hw: HardwareSettings,
    diet: DietSettings,
    theme: String,
    language: String,
    selectedEquipment: Set<String>,
    mealTime: String,
    onSaveHardware: (HardwareSettings) -> Unit,
    onSaveDiet: (DietSettings) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onEditSetup: () -> Unit
) {
    var showHwDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDietDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val activeThemeSpec = themeSpec(theme)

    androidx.compose.runtime.LaunchedEffect(Unit) { contentVisible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 36.dp)
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 }
        ) {
            EditorialSettingsMasthead()
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(220, delayMillis = 60)) { it / 14 }
        ) {
            EditorialSettingsSection(number = "01", title = if (L.isTr) "Mutfağın" else "Your kitchen") {
                EditorialSettingsRow(
                    title = if (L.isTr) "Pişirme araçları" else "Cooking equipment",
                    subtitle = if (L.isTr) "${selectedEquipment.size} araç seçili · $mealTime" else "${selectedEquipment.size} items selected · $mealTime",
                    onClick = onEditSetup
                )
                EditorialSettingsRow(
                    title = if (L.isTr) "Donanım profili" else "Hardware profile",
                    subtitle = buildHardwareSummary(hw),
                    onClick = { showHwDialog = true }
                )
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(220, delayMillis = 110)) + slideInVertically(tween(220, delayMillis = 110)) { it / 14 }
        ) {
            EditorialSettingsSection(number = "02", title = if (L.isTr) "Tercihlerin" else "Your preferences") {
                EditorialSettingsRow(
                    title = if (L.isTr) "Beslenme tercihi" else "Dietary preference",
                    subtitle = dietSummary(diet),
                    onClick = { showDietDialog = true }
                )
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(220, delayMillis = 160)) + slideInVertically(tween(220, delayMillis = 160)) { it / 14 }
        ) {
            EditorialSettingsSection(number = "03", title = if (L.isTr) "Uygulama" else "Application") {
                EditorialSettingsRow(
                    title = if (L.isTr) "Dil" else "Language",
                    subtitle = language,
                    onClick = { showLangDialog = true }
                )
                EditorialSettingsRow(
                    title = if (L.isTr) "Uygulama teması" else "App theme",
                    subtitle = activeThemeSpec.title,
                    onClick = { showThemeDialog = true },
                    trailing = { ThemeSwatches(activeThemeSpec) }
                )
                EditorialInfoRow(
                    title = if (L.isTr) "Sürüm" else "Version",
                    value = BuildConfig.VERSION_NAME
                )
                Spacer(Modifier.height(18.dp))
                TextButton(
                    onClick = onEditSetup,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = if (L.isTr) "Kurulumu yeniden yap" else "Redo setup" }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (L.isTr) "Kurulumu yeniden yap" else "Redo setup", color = colors.onSurfaceSub)
                }
            }
        }
    }

    if (showHwDialog) {
        HardwareDialog(
            current = hw,
            colors = colors,
            onSave = { updated -> onSaveHardware(updated); showHwDialog = false },
            onDismiss = { showHwDialog = false }
        )
    }
    if (showLangDialog) {
        ListDialog(
            title = if (L.isTr) "Dil seç" else "Choose language",
            current = language,
            options = listOf(L.Turkish, L.English),
            colors = colors,
            onSelect = { onSetLanguage(it); showLangDialog = false },
            onDismiss = { showLangDialog = false }
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
            current = diet,
            colors = colors,
            onSave = { updated -> onSaveDiet(updated); showDietDialog = false },
            onDismiss = { showDietDialog = false }
        )
    }
}

private fun buildHardwareSummary(hw: HardwareSettings): String = if (L.isTr) {
    "${if (hw.stoveType == "gas") "Gaz ocak" else "Elektrikli ocak"} · ${hw.servingSize} kişi"
} else {
    "${if (hw.stoveType == "gas") "Gas stove" else "Electric stove"} · serves ${hw.servingSize}"
}

private fun dietSummary(diet: DietSettings): String = when (diet.dietType) {
    "none" -> if (L.isTr) "Kısıtlama yok" else "No restrictions"
    else -> diet.dietType.replaceFirstChar { it.uppercase() }
}

@Composable
private fun EditorialSettingsMasthead() {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 34.dp)) {
        Text(if (L.isTr) "MUTFAK DEFTERİ" else "KITCHEN NOTES", color = colors.primary, style = MaterialTheme.typography.caption)
        Spacer(Modifier.size(10.dp))
        Text(if (L.isTr) "Ayarlar" else "Settings", color = colors.onSurface, style = MaterialTheme.typography.h1)
        Spacer(Modifier.size(10.dp))
        Text(
            if (L.isTr) "Mutfağının ayrıntılarını ve uygulama tercihlerini burada düzenleyebilirsin." else "Keep your kitchen details and app preferences close at hand.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun EditorialSettingsSection(number: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Divider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.size(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(number, color = colors.primary, style = MaterialTheme.typography.h6)
            Spacer(Modifier.width(14.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6)
        }
        Spacer(Modifier.size(8.dp))
        content()
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun EditorialSettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title. $subtitle" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.body1, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(3.dp))
            Text(subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.primary)
        }
    }
    Divider(color = colors.divider, thickness = 1.dp)
}

@Composable
private fun EditorialInfoRow(title: String, value: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        Text(value, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
    }
    Divider(color = colors.divider, thickness = 1.dp)
}

@Composable
private fun ThemeSwatches(spec: ThemeSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(spec.colors.primary, spec.colors.accent, spec.colors.surfaceAlt).forEach { color ->
            Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(6.dp)).border(1.dp, LocalAppColors.current.divider, RoundedCornerShape(6.dp)))
        }
    }
}

@Composable
private fun EditorialDialogSurface(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = colors.surface,
            shape = RoundedCornerShape(16.dp),
            elevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), content = content)
        }
    }
}

@Composable
private fun EditorialDialogHeader(title: String, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.semantics { contentDescription = if (L.isTr) "Kapat" else "Close" }
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
        }
    }
    Spacer(Modifier.size(14.dp))
    Divider(color = colors.divider, thickness = 1.dp)
    Spacer(Modifier.size(14.dp))
}

@Composable
fun HardwareDialog(current: HardwareSettings, colors: AppColors, onSave: (HardwareSettings) -> Unit, onDismiss: () -> Unit) {
    var stoveType by remember { mutableStateOf(current.stoveType) }
    var ovenAvailable by remember { mutableStateOf(current.ovenAvailable) }
    var servingSize by remember { mutableStateOf(current.servingSize) }
    var powerLevel by remember { mutableStateOf(current.powerLevel) }
    var geminiKey by remember { mutableStateOf(current.geminiApiKey) }
    var hfKey by remember { mutableStateOf(current.hfApiKey) }
    var aiProvider by remember { mutableStateOf(current.aiProvider) }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(if (L.isTr) "Donanım profili" else "Hardware profile", onDismiss)
        Text(if (L.isTr) "Yapay zekâ sağlayıcısı" else "AI provider", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.size(8.dp))
        listOf(
            "GEMINI" to "Google Gemini",
            "HUGGINGFACE" to "Hugging Face",
            "DUCKDUCKGO" to "DuckDuckGo (No-Key)",
            "FREE" to "Bedava (No-Key)"
        ).forEach { (key, label) ->
            EditorialProviderOption(
                label = label,
                selected = aiProvider == key,
                onSelect = { aiProvider = key }
            )
        }

        Spacer(Modifier.size(14.dp))
        when (aiProvider) {
            "GEMINI" -> CredentialField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = "Gemini API Key",
                onPaste = { clipboardManager.getText()?.text?.takeIf(String::isNotBlank)?.let { geminiKey = it } },
                helpText = if (L.isTr) "Anahtarı aistudio.google.com adresinden alabilirsin." else "Get a key from aistudio.google.com.",
                onHelp = { uriHandler.openUri("https://aistudio.google.com/app/apikey") }
            )
            "HUGGINGFACE" -> CredentialField(
                value = hfKey,
                onValueChange = { hfKey = it },
                label = "Hugging Face Token",
                onPaste = { clipboardManager.getText()?.text?.takeIf(String::isNotBlank)?.let { hfKey = it } },
                helpText = if (L.isTr) "Token'ı huggingface.co adresinden alabilirsin." else "Get a token from huggingface.co.",
                onHelp = { uriHandler.openUri("https://huggingface.co/settings/tokens") }
            )
            else -> Text(
                if (L.isTr) "Bu sağlayıcı için anahtar gerekmez. Görsel analiz sınırlı olabilir." else "This provider does not need a key. Vision analysis may be limited.",
                color = colors.success,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.size(18.dp))
        Divider(color = colors.divider, thickness = 1.dp)
        Spacer(Modifier.size(16.dp))
        Text(if (L.isTr) "Ocak tipi" else "Stove type", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorialChoiceButton(
                label = if (L.isTr) "Elektrik" else "Electric",
                selected = stoveType == "electric",
                onClick = { stoveType = "electric" }
            )
            EditorialChoiceButton(
                label = if (L.isTr) "Gaz" else "Gas",
                selected = stoveType == "gas",
                onClick = { stoveType = "gas" }
            )
        }
        Spacer(Modifier.size(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (L.isTr) "Fırın kullanılabilir" else "Oven available", color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
            Switch(
                checked = ovenAvailable,
                onCheckedChange = { ovenAvailable = it },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primaryLight)
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(if (L.isTr) "Porsiyon: $servingSize kişi" else "Servings: $servingSize", color = colors.onSurface, style = MaterialTheme.typography.body1)
        Slider(
            value = servingSize.toFloat(),
            onValueChange = { servingSize = it.toInt() },
            valueRange = 1f..8f,
            steps = 6,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.divider)
        )
        Spacer(Modifier.size(18.dp))
        DialogActions(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    current.copy(
                        stoveType = stoveType,
                        ovenAvailable = ovenAvailable,
                        servingSize = servingSize,
                        powerLevel = powerLevel,
                        geminiApiKey = geminiKey,
                        hfApiKey = hfKey,
                        aiProvider = aiProvider
                    )
                )
            }
        )
    }
}

@Composable
private fun EditorialProviderOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = "$label, ${if (selected) if (L.isTr) "seçili" else "selected" else if (L.isTr) "seçili değil" else "not selected"}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect, colors = RadioButtonDefaults.colors(selectedColor = colors.primary))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) colors.onSurface else colors.onSurfaceSub, style = MaterialTheme.typography.body1)
    }
    Divider(color = colors.divider, thickness = 1.dp)
}

@Composable
private fun CredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onPaste: () -> Unit,
    helpText: String,
    onHelp: () -> Unit
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = colors.onSurface,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.divider
        ),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            IconButton(
                onClick = onPaste,
                modifier = Modifier.semantics { contentDescription = if (L.isTr) "Panodan yapıştır" else "Paste from clipboard" }
            ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = colors.primary)
            }
        }
    )
    Text(
        helpText,
        color = colors.primary,
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(top = 6.dp).clickable(onClick = onHelp)
    )
}

@Composable
private fun EditorialChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) colors.primary else colors.divider),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (selected) colors.primary.copy(alpha = .12f) else Color.Transparent)
    ) {
        Text(label, color = if (selected) colors.primary else colors.onSurfaceSub)
    }
}

@Composable
fun DietDialog(current: DietSettings, colors: AppColors, onSave: (DietSettings) -> Unit, onDismiss: () -> Unit) {
    var dietType by remember { mutableStateOf(current.dietType) }
    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(if (L.isTr) "Beslenme tercihi" else "Dietary preference", onDismiss)
        listOf(
            "none" to if (L.isTr) "Kısıtlama yok" else "No restrictions",
            "vegetarian" to "Vegetarian",
            "vegan" to "Vegan",
            "keto" to "Keto"
        ).forEach { (key, label) ->
            EditorialSelectionRow(label, dietType == key) { dietType = key }
        }
        Spacer(Modifier.size(18.dp))
        DialogActions(onDismiss = onDismiss, onSave = { onSave(DietSettings(dietType, current.allergies)) })
    }
}

@Composable
fun ListDialog(title: String, current: String, options: List<String>, colors: AppColors, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(title, onDismiss)
        options.forEach { option ->
            EditorialSelectionRow(option, option.equals(current, ignoreCase = true)) { onSelect(option) }
        }
    }
}

@Composable
private fun EditorialSelectionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = "$label, ${if (selected) if (L.isTr) "seçili" else "selected" else if (L.isTr) "seçili değil" else "not selected"}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.success)
    }
    Divider(color = colors.divider, thickness = 1.dp)
}

@Composable
private fun DialogActions(onDismiss: () -> Unit, onSave: () -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDismiss) { Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub) }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSave,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
            elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            modifier = Modifier.semantics { contentDescription = if (L.isTr) "Kaydet" else "Save" }
        ) {
            Text(if (L.isTr) "Kaydet" else "Save", color = colors.onPrimary)
        }
    }
}

@Composable
fun ThemePickerDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(if (L.isTr) "Uygulama teması" else "App theme", onDismiss)
        ThemeCatalog.forEach { spec ->
            val selected = spec.id == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp)
                    .clickable { onSelect(spec.id) }
                    .semantics { contentDescription = "${spec.title}, ${if (selected) if (L.isTr) "seçili" else "selected" else if (L.isTr) "seçili değil" else "not selected"}" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeSwatches(spec)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(spec.title, color = colors.onSurface, style = MaterialTheme.typography.body1, fontWeight = FontWeight.SemiBold)
                    Text(spec.subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                }
                if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary)
            }
            Divider(color = colors.divider, thickness = 1.dp)
        }
    }
}

@Preview(showBackground = true, locale = "tr")
@Composable
private fun TurkishEditorialSettingsPreview() {
    AgenticTheme("editorial") {
        SettingsScreen(
            hw = HardwareSettings(),
            diet = DietSettings(),
            theme = "editorial",
            language = "Türkçe",
            selectedEquipment = setOf("oven", "elec"),
            mealTime = "19:00",
            onSaveHardware = {},
            onSaveDiet = {},
            onSetLanguage = {},
            onSetTheme = {},
            onEditSetup = {}
        )
    }
}

@Preview(showBackground = true, locale = "en")
@Composable
private fun EnglishEditorialSettingsPreview() {
    AgenticTheme("editorial") {
        SettingsScreen(
            hw = HardwareSettings(stoveType = "gas", servingSize = 4, ovenAvailable = true),
            diet = DietSettings(dietType = "vegetarian"),
            theme = "heritage",
            language = "English",
            selectedEquipment = setOf("gas", "oven", "pan", "grill"),
            mealTime = "20:30",
            onSaveHardware = {},
            onSaveDiet = {},
            onSetLanguage = {},
            onSetTheme = {},
            onEditSetup = {}
        )
    }
}
