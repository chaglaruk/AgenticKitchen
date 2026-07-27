package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.INGREDIENT_CATEGORIES
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.ScheduleEvent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private data class IntelligenceCategory(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    chips: List<String>,
    servings: Int,
    scannedIngredients: List<String>?,
    pantryIntel: PantryIntelReport,
    onScanImage: (android.graphics.Bitmap) -> Unit,
    onClearScannedIngredients: () -> Unit,
    onAddChip: (String) -> Unit,
    onAddMultipleChips: (List<String>) -> Unit,
    onRemoveChip: (String) -> Unit,
    onClearAll: () -> Unit,
    onStart: () -> Unit,
    onEditSetup: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var showCameraModal by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalAppColors.current
    val themeSpec = LocalThemeSpec.current

    val allIngredients = remember { INGREDIENT_CATEGORIES.flatMap { it.items } }
    var expandedAuto by remember { mutableStateOf(false) }
    val filteredIngredients = if (input.length >= 2) {
        allIngredients.filter {
            (it.first.contains(input, ignoreCase = true) || it.second.contains(input, ignoreCase = true)) &&
                !chips.contains(if (L.isTr) it.first else it.second)
        }.map { if (L.isTr) it.first else it.second }.take(5)
    } else {
        emptyList()
    }

    LaunchedEffect(filteredIngredients) {
        expandedAuto = filteredIngredients.isNotEmpty()
    }

    val categoryCards = listOf(
        IntelligenceCategory("Vegetation", Icons.Filled.Eco, colors.success),
        IntelligenceCategory("Protein Aqua", Icons.Filled.WaterDrop, colors.primary),
        IntelligenceCategory("Protein Land", Icons.Filled.SetMeal, colors.accent),
        IntelligenceCategory("Carb Matrix", Icons.Filled.Grain, colors.primaryLight),
        IntelligenceCategory("Spice Payload", Icons.Filled.LocalFireDepartment, colors.accent),
        IntelligenceCategory("Liquids", Icons.Filled.LocalDrink, colors.success)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        EditorialHomeHeader(chips = chips, servings = servings)
        IngredientComposer(
            input = input,
            onInputChange = { input = it },
            expandedAuto = expandedAuto,
            filteredIngredients = filteredIngredients,
            onAddSelection = {
                onAddChip(it)
                input = ""
                expandedAuto = false
            },
            onDone = {
                input.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(onAddChip)
                input = ""
                keyboard?.hide()
            },
            canGenerate = chips.isNotEmpty(),
            onStart = onStart,
            onOpenCamera = { showCameraModal = true }
        )

        Spacer(Modifier.height(16.dp))
        EditorialIngredientCollection(chips = chips, onRemove = onRemoveChip)

        Spacer(Modifier.height(20.dp))
        PantryIntelOverviewCard(pantryIntel = pantryIntel, onEditSetup = onEditSetup)
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorialTextAction(
                modifier = Modifier.weight(1f),
                title = if (L.isTr) "Tüm malzemeler" else "All ingredients",
                icon = Icons.Filled.GridView,
                onClick = { showPicker = true }
            )
            EditorialTextAction(
                modifier = Modifier.weight(1f),
                title = if (L.isTr) "Kurulum" else "Setup",
                icon = Icons.Filled.Tune,
                onClick = onEditSetup
            )
            EditorialTextAction(
                modifier = Modifier.weight(1f),
                title = if (L.isTr) "Temizle" else "Clear",
                icon = Icons.Filled.DeleteSweep,
                destructive = true,
                onClick = onClearAll
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            when (themeSpec.id) {
                "editorial" -> if (L.isTr) "Malzeme kategorileri" else "Ingredient categories"
                "heritage" -> if (L.isTr) "Taksonomi" else "Taxonomy"
                "zen" -> if (L.isTr) "Kategori Izgarası" else "Category Grid"
                else -> if (L.isTr) "Operasyon Sınıfları" else "Operational Classes"
            },
            color = colors.onSurface,
            style = MaterialTheme.typography.h2,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categoryCards.chunked(2).forEach { rowCards ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowCards.forEach { item ->
                        when (themeSpec.id) {
                            "heritage" -> HeritageCategoryCard(
                                modifier = Modifier.weight(1f),
                                title = item.title,
                                icon = item.icon,
                                tint = item.tint,
                                colors = colors,
                                onClick = { showPicker = true }
                            )
                            "zen" -> ZenCategoryCard(
                                modifier = Modifier.weight(1f),
                                title = item.title,
                                icon = item.icon,
                                colors = colors,
                                onClick = { showPicker = true }
                            )
                            else -> SignalCategoryCard(
                                modifier = Modifier.weight(1f),
                                title = item.title,
                                icon = item.icon,
                                tint = item.tint,
                                colors = colors,
                                onClick = { showPicker = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        SideTabCategoryPicker(
            alreadyAdded = chips,
            colors = colors,
            onAdd = onAddChip,
            onDismiss = { showPicker = false }
        )
    }

    if (showCameraModal) {
        CameraModal(
            scannedIngredients = scannedIngredients,
            onDismiss = {
                showCameraModal = false
                onClearScannedIngredients()
            },
            onAcceptScan = { scanned ->
                onAddMultipleChips(scanned)
                showCameraModal = false
                onClearScannedIngredients()
            },
            onImageCaptured = { bmp -> onScanImage(bmp) }
        )
    }
}

@Composable
private fun EditorialHomeHeader(chips: List<String>, servings: Int) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { -it / 5 }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text("Agentic Kitchen", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            Spacer(Modifier.height(10.dp))
            Text(
                if (L.isTr) "Bu akşam ne pişiriyoruz?" else "What are we cooking tonight?",
                color = colors.onBackground,
                style = MaterialTheme.typography.h1
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    chips.isEmpty() -> if (L.isTr) {
                        "Mutfağındaki malzemeleri ekleyerek başla."
                    } else {
                        "Start by adding what you have in the kitchen."
                    }
                    L.isTr -> "${chips.size} malzeme · $servings kişilik"
                    else -> "${chips.size} ingredients · serves $servings"
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
private fun IngredientComposer(
    input: String,
    onInputChange: (String) -> Unit,
    expandedAuto: Boolean,
    filteredIngredients: List<String>,
    onAddSelection: (String) -> Unit,
    onDone: () -> Unit,
    canGenerate: Boolean,
    onStart: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val colors = LocalAppColors.current
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            .background(colors.surfaceAlt, RoundedCornerShape(18.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            if (L.isTr) "Mutfağında ne var?" else "What is in your kitchen?",
            color = colors.onSurface,
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(expanded = expandedAuto, onExpandedChange = { }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (isFocused) colors.primary else colors.divider, RoundedCornerShape(12.dp))
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused }.padding(vertical = 13.dp),
                        textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (input.isEmpty()) Text(
                                    if (L.isTr) "Örn: tavuk, pirinç, sarımsak" else "E.g. chicken, rice, garlic",
                                    color = colors.onSurfaceSub,
                                    fontSize = 15.sp
                                )
                                innerTextField()
                            }
                        }
                    )
                    IconButton(onClick = onOpenCamera) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = if (L.isTr) "Kamera" else "Camera",
                            tint = colors.onSurfaceSub
                        )
                    }
                    TextButton(onClick = onDone) {
                        Text(if (L.isTr) "Ekle" else "Add", color = colors.primary)
                    }
                }
                DropdownMenu(
                    expanded = expandedAuto,
                    onDismissRequest = { },
                    modifier = Modifier.background(colors.surface)
                ) {
                    filteredIngredients.forEach { selection ->
                        DropdownMenuItem(onClick = { onAddSelection(selection) }) {
                            Text(text = selection, color = colors.onSurface)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStart,
            enabled = canGenerate,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primary,
                disabledBackgroundColor = colors.divider
            ),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                if (L.isTr) "Tarifleri Gör" else "See Recipes",
                color = colors.onPrimary,
                style = MaterialTheme.typography.button
            )
        }
    }
}

@Composable
private fun IntelligenceHero(themeId: String) {
    val colors = LocalAppColors.current

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (themeId == "signal") Alignment.Start else Alignment.CenterHorizontally
        ) {
            when (themeId) {
                "heritage" -> {
                    Text("VOLUME I", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "Mutfak Arşivi" else "The Culinary Archives",
                        color = colors.onBackground,
                        style = MaterialTheme.typography.h1,
                        textAlign = TextAlign.Center
                    )
                }
                "zen" -> {
                    Text(
                        "The Intelligence Input",
                        color = colors.onBackground,
                        style = MaterialTheme.typography.h1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (L.isTr) "Envanteri gir, sistem optimum planı sentezlesin." else "Provide current inventory constraints for optimal synthesis.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text("SIGNAL DECK", color = colors.primary, style = MaterialTheme.typography.caption)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "Canlı Pantry Akışı" else "Live Pantry Signal",
                        color = colors.onBackground,
                        style = MaterialTheme.typography.h1
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "Malzemeyi kilitle, riskleri gör, sonra görev paketini üret." else "Lock the pantry, surface the risks, then generate the mission package.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
private fun InputManifestCard(
    input: String,
    onInputChange: (String) -> Unit,
    expandedAuto: Boolean,
    filteredIngredients: List<String>,
    onAddSelection: (String) -> Unit,
    onDone: () -> Unit,
    onStart: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val colors = LocalAppColors.current
    val themeSpec = LocalThemeSpec.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        backgroundColor = if (themeSpec.id == "heritage") colors.surfaceAlt else colors.surface,
        shape = RoundedCornerShape(if (themeSpec.id == "heritage") 0.dp else 24.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                when (themeSpec.id) {
                    "heritage" -> if (L.isTr) "MANUEL GİRİŞ" else "MANUAL INPUT"
                    "zen" -> if (L.isTr) "AKILLI GİRİŞ" else "INTELLIGENCE INPUT"
                    else -> if (L.isTr) "PANTRY MANIFEST" else "PANTRY MANIFEST"
                },
                color = if (themeSpec.id == "signal") colors.primary else colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(expanded = expandedAuto, onExpandedChange = { }, modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textStyle = TextStyle(color = colors.onSurface, fontSize = 18.sp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        decorationBox = { innerTextField ->
                            Box(Modifier.fillMaxWidth()) {
                                if (input.isEmpty()) {
                                    Text(
                                        when (themeSpec.id) {
                                            "heritage" -> if (L.isTr) "Mevcut malzemeleri arşive işle..." else "Inscribe available provisions..."
                                            "zen" -> if (L.isTr) "Malzemeleri virgülle ayırarak yaz..." else "Enter specific ingredients separated by commas..."
                                            else -> if (L.isTr) "Örn: tavuk, pirinç, sarımsak" else "E.g. chicken, rice, garlic"
                                        },
                                        color = colors.onSurfaceSub,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = colors.onSurfaceSub,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expandedAuto,
                        onDismissRequest = { },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        filteredIngredients.forEach { selection ->
                            DropdownMenuItem(onClick = { onAddSelection(selection) }) {
                                Text(text = selection, color = colors.onSurface)
                            }
                        }
                    }
                }
                Divider(color = colors.divider, modifier = Modifier.align(Alignment.BottomCenter))
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                    shape = RoundedCornerShape(if (themeSpec.id == "heritage") 0.dp else 999.dp)
                ) {
                    Text("Analyze & Generate Plans", color = colors.onPrimary, style = MaterialTheme.typography.button)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = colors.onPrimary)
                }
                OutlinedButton(
                    onClick = onOpenCamera,
                    modifier = Modifier.height(50.dp),
                    border = BorderStroke(1.dp, colors.divider),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.surfaceAlt),
                    shape = RoundedCornerShape(if (themeSpec.id == "heritage") 0.dp else 16.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun PantryIntelOverviewCard(pantryIntel: PantryIntelReport, onEditSetup: () -> Unit) {
    val colors = LocalAppColors.current
    val themeSpec = LocalThemeSpec.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(if (themeSpec.id == "heritage") 0.dp else 24.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (L.isTr) "MUTFAK ÖZETİ" else "KITCHEN SUMMARY",
                        color = colors.primary,
                        style = MaterialTheme.typography.caption
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${pantryIntel.readinessScore}/100 • ${pantryCategoryLabel(pantryIntel.focusCategoryId)}",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        equipmentLaneLabel(pantryIntel.equipmentLane),
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1
                    )
                }
                OutlinedButton(
                    onClick = onEditSetup,
                    shape = RoundedCornerShape(if (themeSpec.id == "heritage") 0.dp else 999.dp),
                    border = BorderStroke(1.dp, colors.divider),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.surfaceAlt)
                ) {
                    Text(if (L.isTr) "Kurulum" else "Setup", color = colors.onSurface)
                }
            }
            Spacer(Modifier.height(14.dp))
            pantryIntel.warnings.take(1).forEach { warning ->
                Text("• ${pantrySignalText(warning)}", color = colors.accent, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(4.dp))
            }
            pantryIntel.tactics.take(2).forEach { tactic ->
                Text("• ${pantrySignalText(tactic)}", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun EditorialTextAction(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val actionColor = if (destructive) Color(0xFF9B3F32) else colors.primary

    Row(
        modifier = modifier.clickable { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = actionColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(title, color = if (destructive) actionColor else colors.onSurfaceSub, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyEditorialHomePreview() = EditorialHomePreview(emptyList())

@Preview(showBackground = true)
@Composable
private fun PopulatedEditorialHomePreview() = EditorialHomePreview(
    listOf("Domates", "Ispanak", "Tavuk", "Pirinç", "Peynir", "Ekmek")
)

@Composable
private fun EditorialHomePreview(chips: List<String>) {
    AgenticTheme("editorial") {
        HomeScreen(
            chips = chips,
            servings = 2,
            scannedIngredients = null,
            pantryIntel = PantryIntelReport(
                readinessScore = 70,
                focusCategoryId = "vegetables",
                focusCategoryLabel = "Vegetables",
                categoryBreakdown = emptyList(),
                warnings = emptyList(),
                tactics = emptyList(),
                equipmentLane = "stovetop"
            ),
            onScanImage = {},
            onClearScannedIngredients = {},
            onAddChip = {},
            onAddMultipleChips = {},
            onRemoveChip = {},
            onClearAll = {},
            onStart = {},
            onEditSetup = {}
        )
    }
}

@Composable
private fun HeritageCategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    colors: AppColors,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(0.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(colors.surfaceAlt, colors.background)))
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center).size(48.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.92f))
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    title,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.h6
                )
            }
        }
    }
}

@Composable
private fun SignalCategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    colors: AppColors,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(20.dp),
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(tint.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint)
                }
                Spacer(Modifier.width(10.dp))
                Text("LIVE", color = tint, style = MaterialTheme.typography.caption)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(6.dp))
            Text(
                if (L.isTr) "Dokun ve kategori atlasını aç." else "Tap to open the category atlas.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
fun OptionsListCard(options: List<RecipeOption>, colors: AppColors, onRefresh: () -> Unit, onSelect: (RecipeOption) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (L.isTr) "Şefin Önerileri" else "Chef Suggestions", color = colors.onSurface, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.divider),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (L.isTr) "YENİLE" else "REFRESH", color = colors.onSurface, fontSize = 12.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        options.forEach { opt ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(opt) },
                backgroundColor = colors.surface,
                shape = RoundedCornerShape(18.dp),
                elevation = 0.dp,
                border = BorderStroke(1.dp, colors.divider)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(opt.type.uppercase(), color = colors.primary, fontSize = 10.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(opt.name, color = colors.onSurface, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(8.dp))
                    Text(opt.description, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun SideTabCategoryPicker(
    alreadyAdded: List<String>,
    colors: AppColors,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            backgroundColor = colors.background,
            shape = RoundedCornerShape(20.dp),
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L.categoryBtn, color = colors.onSurface, style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
                        }
                    }
                }
                Divider(color = colors.divider)
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .background(colors.surface.copy(alpha = 0.5f))
                            .verticalScroll(rememberScrollState())
                    ) {
                        INGREDIENT_CATEGORIES.forEachIndexed { index, cat ->
                            val isSelected = selectedTabIndex == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { selectedTabIndex = index }
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.icon, fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(cat.label, color = if (isSelected) colors.primary else colors.onSurface, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                            if (index < INGREDIENT_CATEGORIES.size - 1) Divider(color = colors.divider.copy(alpha = 0.3f))
                        }
                    }
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = colors.divider)
                    val activeCat = INGREDIENT_CATEGORIES[selectedTabIndex]
                    Column(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        activeCat.items.map { if (L.isTr) it.first else it.second }.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rowItems.forEach { item ->
                                    val isAdded = alreadyAdded.any { it.equals(item, ignoreCase = true) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isAdded) colors.primary.copy(alpha = 0.12f) else colors.surface)
                                            .border(1.dp, if (isAdded) colors.primary else colors.divider, RoundedCornerShape(12.dp))
                                            .clickable { if (!isAdded) onAdd(item) }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item, color = if (isAdded) colors.primary else colors.onSurface, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                                    }
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary)
                    ) {
                        Text(if (L.isTr) "TAMAM" else "DONE", color = colors.onPrimary, style = MaterialTheme.typography.button)
                    }
                }
            }
        }
    }
}

@Composable
fun ZenCategoryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: AppColors,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        backgroundColor = colors.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, colors.divider),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).background(colors.surfaceAlt, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FlowRow(chips: List<String>, colors: AppColors, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { chip -> IngredientChip(label = chip, colors = colors, onRemove = { onRemove(chip) }) }
            }
        }
    }
}

@Composable
fun IngredientChip(label: String, colors: AppColors, onRemove: () -> Unit) {
    Box(modifier = Modifier.background(colors.surfaceAlt, RoundedCornerShape(999.dp)).border(1.dp, colors.divider, RoundedCornerShape(999.dp))) {
        Row(modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(colors.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, colors: AppColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFBA1A1A))
            Spacer(Modifier.width(12.dp))
            Text(message, color = colors.onSurface, style = MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun PlanStepRow(step: Int, event: ScheduleEvent, colors: AppColors) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).background(colors.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$step", color = colors.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(event.instruction, color = colors.onSurface, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val durationSec = try { java.time.Duration.between(OffsetDateTime.parse(event.startIso), OffsetDateTime.parse(event.endIso)).seconds } catch (_: Exception) { 0 }
                PillBadge(text = "⏰ ${durationSec}s", color = colors.accent, colors = colors)
                PillBadge(text = humanResource(event.resource), color = colors.primaryLight, colors = colors)
            }
        }
    }
}

@Composable
fun PillBadge(text: String, color: Color, colors: AppColors) {
    Box(
        modifier = Modifier
            .background(colors.surfaceAlt, RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

private fun humanResource(resource: String): String = when (resource) {
    "oven" -> if (L.isTr) "🫕 Fırın" else "🫕 Oven"
    "stovetop" -> if (L.isTr) "⚡ Ocak" else "⚡ Stove"
    "grill" -> if (L.isTr) "🪵 Mangal" else "🪵 Grill"
    "airfryer" -> if (L.isTr) "💨 Airfryer" else "💨 Airfryer"
    "microwave" -> if (L.isTr) "🌀 Mikrodalga" else "🌀 Microwave"
    "camping" -> if (L.isTr) "🏕️ Kamp Ocağı" else "🏕️ Camping Stove"
    else -> resource
}

private fun formatTime(iso: String): String = try {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) {
    iso.takeLast(8).take(5)
}
