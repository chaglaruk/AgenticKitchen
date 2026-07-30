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
import com.agentickitchen.android.AllergyCatalog
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.L
import com.agentickitchen.android.CookingProviderSelection

@Composable
fun SettingsScreen(
    hw: HardwareSettings,
    diet: DietSettings,
    theme: String,
    language: String,
    selectedEquipment: Set<String>,
    onSaveHardware: (HardwareSettings) -> Unit,
    onSaveDiet: (DietSettings) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onEditSetup: () -> Unit
) {
    var showHwDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showDietDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current

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
                    subtitle = if (L.isTr) "${selectedEquipment.size} araç seçili" else "${selectedEquipment.size} items selected",
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
                    title = if (L.isTr) "Görünüm" else "Appearance",
                    subtitle = appearanceLabel(theme),
                    onClick = { showAppearanceDialog = true }
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
    if (showAppearanceDialog) {
        AppearancePickerDialog(
            current = themeSpec(theme).id,
            onSelect = { appearance -> onSetTheme(appearance); showAppearanceDialog = false },
            onDismiss = { showAppearanceDialog = false }
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

private fun buildHardwareSummary(hw: HardwareSettings): String = when (hw.stoveType) {
    "gas" -> if (L.isTr) "Gazlı ocak" else "Gas stove"
    "electric" -> if (L.isTr) "Elektrikli ocak" else "Electric stove"
    else -> if (L.isTr) "Ocak seçilmedi" else "No stove selected"
}

private fun appearanceLabel(theme: String): String = when (themeSpec(theme).id) {
    "editorial-dark" -> if (L.isTr) "Koyu Editoryal" else "Dark Editorial"
    else -> if (L.isTr) "Açık Editoryal" else "Light Editorial"
}

private fun dietSummary(diet: DietSettings): String {
    val dietLabel = when (diet.dietType) {
        "none" -> if (L.isTr) "Kısıtlama yok" else "No restrictions"
        else -> diet.dietType.replaceFirstChar { it.uppercase() }
    }
    val allergies = AllergyCatalog.normalize(diet.allergies)
        .joinToString { AllergyCatalog.label(it, L.isTr) }
    return if (allergies.isBlank()) dietLabel
    else if (L.isTr) "$dietLabel · Alerjiler: $allergies"
    else "$dietLabel · Allergies: $allergies"
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
    var powerLevel by remember { mutableStateOf(current.powerLevel) }
    var geminiKey by remember { mutableStateOf(current.geminiApiKey) }
    var hfKey by remember { mutableStateOf(current.hfApiKey) }
    var aiProvider by remember(current.aiProvider) {
        mutableStateOf(CookingProviderSelection.normalize(current.aiProvider))
    }
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(if (L.isTr) "Donanım profili" else "Hardware profile", onDismiss)
        Text(if (L.isTr) "Yapay zekâ sağlayıcısı" else "AI provider", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.size(8.dp))
        listOf(
            CookingProviderSelection.Gemini to "Google Gemini",
            CookingProviderSelection.HuggingFace to "Hugging Face",
            CookingProviderSelection.Free to if (L.isTr) "Ücretsiz (anahtar gerekmez)" else "Free (no key required)"
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
        Spacer(Modifier.size(18.dp))
        DialogActions(
            onDismiss = onDismiss,
            onSave = {
                onSave(
                    current.copy(
                        stoveType = stoveType,
                        ovenAvailable = ovenAvailable,
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
    var allergies by remember(current.allergies) {
        mutableStateOf(AllergyCatalog.normalize(current.allergies))
    }
    var customAllergy by remember { mutableStateOf("") }
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
        Text(
            if (L.isTr) "Alerjiler" else "Allergies",
            color = colors.onSurface,
            style = MaterialTheme.typography.h6
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (L.isTr) "Birden fazla seçim yapabilirsin." else "You can select more than one.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body2
        )
        Spacer(Modifier.height(8.dp))
        AllergyCatalog.definitions.forEach { allergy ->
            val selected = allergy.id in allergies
            EditorialSelectionRow(AllergyCatalog.label(allergy.id, L.isTr), selected) {
                allergies = if (selected) allergies - allergy.id else allergies + allergy.id
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customAllergy,
                onValueChange = { customAllergy = it },
                label = { Text(if (L.isTr) "Başka bir alerji" else "Another allergy") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.divider,
                    textColor = colors.onSurface
                )
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = {
                    AllergyCatalog.normalizeCustom(customAllergy)?.let { allergies = allergies + it }
                    customAllergy = ""
                },
                enabled = AllergyCatalog.normalizeCustom(customAllergy) != null
            ) {
                Text(if (L.isTr) "Ekle" else "Add", color = colors.primary)
            }
        }
        allergies.filter { it.startsWith("custom:") }.forEach { allergy ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    AllergyCatalog.label(allergy, L.isTr),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { allergies = allergies - allergy }) {
                    Text(if (L.isTr) "Kaldır" else "Remove", color = colors.primary)
                }
            }
            Divider(color = colors.divider, thickness = 1.dp)
        }
        Spacer(Modifier.size(18.dp))
        DialogActions(onDismiss = onDismiss, onSave = { onSave(DietSettings(dietType, allergies)) })
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
private fun AppearancePickerDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    EditorialDialogSurface(onDismiss) {
        EditorialDialogHeader(if (L.isTr) "Görünüm seç" else "Choose appearance", onDismiss)
        listOf("editorial-light", "editorial-dark").forEach { appearance ->
            EditorialSelectionRow(appearanceLabel(appearance), appearance == current) { onSelect(appearance) }
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
    AgenticTheme("editorial-dark") {
        SettingsScreen(
            hw = HardwareSettings(stoveType = "gas", ovenAvailable = true),
            diet = DietSettings(dietType = "vegetarian"),
            theme = "editorial-dark",
            language = "English",
            selectedEquipment = setOf("gas", "oven", "pan", "grill"),
            onSaveHardware = {},
            onSaveDiet = {},
            onSetLanguage = {},
            onSetTheme = {},
            onEditSetup = {}
        )
    }
}
