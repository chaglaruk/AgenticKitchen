package com.agentickitchen.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.INGREDIENT_CATEGORIES
import com.agentickitchen.android.InventoryRecipeRequest
import com.agentickitchen.android.L
import com.agentickitchen.android.PlanState
import com.agentickitchen.android.RecipeOption
import com.agentickitchen.android.ShoppingImportState
import com.agentickitchen.android.searchIngredientCatalog
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.models.PantryIntelReport
import com.agentickitchen.shared.models.PantryIntelSignal
import com.agentickitchen.shared.models.ScheduleEvent
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.InventoryAdjustmentRecord
import com.agentickitchen.shared.inventory.AdjustmentReason
import com.agentickitchen.shared.inventory.ShoppingImportMode
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    chips: List<String>,
    inventory: List<PantryStockItem>,
    inventoryAdjustments: Map<String, List<InventoryAdjustmentRecord>>,
    shoppingImportState: ShoppingImportState = ShoppingImportState.Idle,
    scannedIngredients: List<String>?,
    pantryIntel: PantryIntelReport,
    onScanImage: (android.graphics.Bitmap) -> Unit,
    onClearScannedIngredients: () -> Unit,
    onAddChip: (String) -> Unit,
    onAddMultipleChips: (List<String>) -> Unit,
    onRemoveChip: (String) -> Unit,
    onSaveInventoryItem: (PantryStockItem?, String, Double, String, String?) -> Unit,
    onDeleteInventoryItem: (PantryStockItem) -> Unit,
    onImportShoppingText: (String, ShoppingImportMode) -> Unit = { _, _ -> },
    onImportShoppingPhoto: (Bitmap, ShoppingImportMode) -> Unit = { _, _ -> },
    onConfirmShoppingImport: (List<ShoppingCandidate>, ShoppingImportMode) -> Boolean = { _, _ -> false },
    onClearShoppingImport: () -> Unit = {},
    onStartInventorySession: (InventoryRecipeRequest) -> Unit = {},
    onClearAll: () -> Unit,
    onStart: () -> Unit,
    onEditSetup: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var showCameraModal by remember { mutableStateOf(false) }
    var showInventoryEditor by remember { mutableStateOf(false) }
    var showShoppingImport by remember { mutableStateOf(false) }
    var showInventoryRecipe by remember { mutableStateOf(false) }
    var editingInventoryItem by remember { mutableStateOf<PantryStockItem?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalAppColors.current

    var expandedAuto by remember { mutableStateOf(false) }
    val filteredIngredients = if (input.length >= 2) {
        searchIngredientCatalog(input, chips, L.isTr).map { it.name(L.isTr) }
    } else {
        emptyList()
    }

    LaunchedEffect(filteredIngredients) {
        expandedAuto = filteredIngredients.isNotEmpty()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            EditorialHomeHeader(chips = chips, modifier = Modifier.fillMaxWidth())
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
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
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            EditorialSectionHeading(
                eyebrow = if (L.isTr) "TARİF TASLAĞI" else "RECIPE DRAFT",
                title = if (L.isTr) "Seçtiğin malzemeler" else "Selected ingredients"
            )
        }
        if (chips.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyIngredientCollection() }
        } else {
            itemsIndexed(chips, key = { _, ingredient -> ingredient.lowercase() }) { index, ingredient ->
                CompactDraftIngredientCard(
                    ingredient = ingredient,
                    entranceDelay = index.coerceAtMost(8) * 35,
                    onRemove = { onRemoveChip(ingredient) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            EditorialSectionHeading(
                eyebrow = if (L.isTr) "MUTFAĞIM" else "MY KITCHEN",
                title = if (L.isTr) "Stoktakiler" else "Pantry inventory",
                action = if (L.isTr) "Malzeme ekle" else "Add item",
                onAction = {
                    editingInventoryItem = null
                    showInventoryEditor = true
                }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showShoppingImport = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, colors.divider),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Alışveriş ekle" else "Add shopping", color = colors.primary)
                }
                Button(
                    onClick = { showInventoryRecipe = true },
                    enabled = inventory.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Elimdekilerle pişir" else "Cook with what I have", color = colors.onPrimary)
                }
            }
        }
        if (inventory.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (L.isTr) "Henüz miktarlı stok eklenmedi." else "No quantified stock yet.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            itemsIndexed(inventory, key = { _, item -> item.id }) { _, item ->
                InventoryIngredientCard(item, Modifier.animateItem()) {
                    editingInventoryItem = item
                    showInventoryEditor = true
                }
            }
        }
        if (chips.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { CompactKitchenSummary(pantryIntel) }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
            if (chips.isNotEmpty()) {
                EditorialTextAction(
                    modifier = Modifier.weight(1f),
                    title = if (L.isTr) "Temizle" else "Clear",
                    icon = Icons.Filled.DeleteSweep,
                    destructive = true,
                    onClick = onClearAll
                )
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

    if (showInventoryEditor) {
        InventoryItemDialog(
            item = editingInventoryItem,
            adjustments = editingInventoryItem?.let { inventoryAdjustments[it.id] }.orEmpty(),
            onDismiss = { showInventoryEditor = false },
            onSave = { name, quantity, unit, packageLabel ->
                onSaveInventoryItem(editingInventoryItem, name, quantity, unit, packageLabel)
                showInventoryEditor = false
            },
            onDelete = editingInventoryItem?.let { item ->
                {
                    onDeleteInventoryItem(item)
                    showInventoryEditor = false
                }
            }
        )
    }

    if (showShoppingImport) {
        ShoppingImportDialog(
            state = shoppingImportState,
            onDismiss = {
                showShoppingImport = false
                onClearShoppingImport()
            },
            onParseText = onImportShoppingText,
            onParsePhoto = onImportShoppingPhoto,
            onConfirm = onConfirmShoppingImport
        )
    }

    if (showInventoryRecipe) {
        InventoryRecipeDialog(
            onDismiss = { showInventoryRecipe = false },
            onStart = {
                showInventoryRecipe = false
                onStartInventorySession(it)
            }
        )
    }
}

@Composable
private fun EditorialSectionHeading(
    eyebrow: String,
    title: String,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(eyebrow, color = colors.primary, style = MaterialTheme.typography.overline)
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.h5)
            }
            if (action != null) {
                TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(action, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun CompactDraftIngredientCard(
    ingredient: String,
    entranceDelay: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var visible by remember(ingredient) { mutableStateOf(false) }
    LaunchedEffect(ingredient) {
        delay(entranceDelay.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 8 }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(.82f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceAlt)
                .clickable(onClick = onRemove)
                .padding(8.dp)
                .semantics {
                    contentDescription = if (L.isTr) "$ingredient kaldır" else "Remove $ingredient"
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                IngredientArtwork(ingredient, Modifier.fillMaxSize().padding(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(13.dp))
                }
            }
            Text(
                ingredient,
                color = colors.onSurface,
                style = MaterialTheme.typography.subtitle2,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InventoryIngredientCard(item: PantryStockItem, modifier: Modifier = Modifier, onEdit: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(.82f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceAlt)
            .clickable(onClick = onEdit)
            .padding(8.dp)
            .semantics {
                contentDescription = if (L.isTr) {
                    "${item.originalName}, ${formatInventoryQuantity(item)}, düzenle"
                } else {
                    "${item.originalName}, ${formatInventoryQuantity(item)}, edit"
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IngredientArtwork(item.originalName, Modifier.weight(1f).fillMaxWidth().padding(4.dp))
        Text(
            item.originalName,
            color = colors.onSurface,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        Text(
            formatInventoryQuantity(item),
            color = colors.primary,
            style = MaterialTheme.typography.caption,
            maxLines = 1
        )
    }
}

private fun formatInventoryQuantity(item: PantryStockItem): String =
    "${BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()} ${item.unit}"

@Composable
private fun InventoryItemDialog(
    item: PantryStockItem?,
    adjustments: List<InventoryAdjustmentRecord>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String?) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = LocalAppColors.current
    var name by remember(item?.id) { mutableStateOf(item?.originalName.orEmpty()) }
    var quantityText by remember(item?.id) {
        mutableStateOf(item?.quantity?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty())
    }
    var unit by remember(item?.id) { mutableStateOf(item?.unit ?: "adet") }
    var packageLabel by remember(item?.id) { mutableStateOf(item?.packageLabel.orEmpty()) }
    var error by remember(item?.id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(item?.id) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = colors.surface,
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (item == null) {
                        if (L.isTr) "Mutfağına ekle" else "Add to your kitchen"
                    } else {
                        if (L.isTr) "Stok miktarını düzenle" else "Edit pantry amount"
                    },
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h5
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (L.isTr) "Malzeme" else "Ingredient") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (adjustments.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Divider(color = colors.divider)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (L.isTr) "Son stok değişiklikleri" else "Recent stock changes",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.subtitle2
                    )
                    adjustments.take(3).forEach { adjustment ->
                        Text(
                            "${adjustmentLabel(adjustment.reason)} · ${BigDecimal.valueOf(adjustment.amount).stripTrailingZeros().toPlainString()} ${item?.unit.orEmpty()}",
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (L.isTr) "Miktar" else "Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("g", "kg", "ml", "L", "adet", "paket", "demet").forEach { choice ->
                        TextButton(onClick = { unit = choice }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(
                                choice,
                                color = if (unit == choice) colors.primary else colors.onSurfaceSub,
                                fontWeight = if (unit == choice) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = packageLabel,
                    onValueChange = { packageLabel = it },
                    label = { Text(if (L.isTr) "Paket notu (isteğe bağlı)" else "Package note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text(if (L.isTr) "Stoktan sil" else "Delete stock", color = MaterialTheme.colors.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(if (L.isTr) "İptal" else "Cancel") }
                    Button(
                        onClick = {
                            val quantity = quantityText.replace(',', '.').toDoubleOrNull()
                            if (name.isBlank() || quantity == null || !quantity.isFinite() || quantity <= 0) {
                                error = if (L.isTr) "Ad ve sıfırdan büyük geçerli bir miktar gir." else "Enter a name and a valid amount above zero."
                            } else {
                                onSave(name.trim(), quantity, unit, packageLabel)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text(if (L.isTr) "Kaydet" else "Save", color = colors.onPrimary)
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        Dialog(onDismissRequest = { confirmDelete = false }) {
            Card(
                backgroundColor = colors.surface,
                elevation = 0.dp,
                border = BorderStroke(1.dp, colors.divider),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (L.isTr) "Bu stok kaydı silinsin mi?" else "Delete this pantry item?",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.align(Alignment.End)) {
                        TextButton(onClick = { confirmDelete = false }) {
                            Text(if (L.isTr) "Vazgeç" else "Keep")
                        }
                        TextButton(onClick = onDelete) {
                            Text(if (L.isTr) "Sil" else "Delete", color = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}

private fun adjustmentLabel(reason: AdjustmentReason): String = when (reason) {
    AdjustmentReason.MANUAL_ADD -> if (L.isTr) "Elle eklendi" else "Added manually"
    AdjustmentReason.SHOPPING_ADD -> if (L.isTr) "Alışveriş eklendi" else "Shopping added"
    AdjustmentReason.RECOUNT -> if (L.isTr) "Yeniden sayıldı" else "Recounted"
    AdjustmentReason.RECIPE_RESERVATION -> if (L.isTr) "Tarif için ayrıldı" else "Reserved for recipe"
    AdjustmentReason.RECIPE_RESERVATION_RELEASE -> if (L.isTr) "Rezervasyon serbest bırakıldı" else "Reservation released"
    AdjustmentReason.RECIPE_CONSUMPTION -> if (L.isTr) "Tarifte kullanıldı" else "Used in recipe"
    AdjustmentReason.CORRECTION -> if (L.isTr) "Düzeltildi" else "Corrected"
    AdjustmentReason.DELETION -> if (L.isTr) "Silindi" else "Deleted"
}

@Composable
private fun EditorialHomeHeader(chips: List<String>, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { -it / 5 }
    ) {
        Column(
            modifier = modifier.padding(vertical = 32.dp)
        ) {
            EditorialBrandLockup()
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
                    L.isTr -> "${chips.size} malzeme"
                    else -> "${chips.size} ingredients"
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
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(colors.surfaceAlt, RoundedCornerShape(14.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            if (L.isTr) "Mutfağında ne var?" else "What is in your kitchen?",
            color = colors.onSurface,
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, if (isFocused) colors.primary else colors.divider, RoundedCornerShape(10.dp))
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused }.padding(vertical = 12.dp),
                textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                decorationBox = { inner -> Box { if (input.isEmpty()) Text(if (L.isTr) "Örn: tavuk, pirinç, sarımsak" else "E.g. chicken, rice, garlic", color = colors.onSurfaceSub, fontSize = 15.sp); inner() } }
            )
            IconButton(onClick = onOpenCamera) { Icon(Icons.Filled.CameraAlt, contentDescription = if (L.isTr) "Kamera" else "Camera", tint = colors.onSurfaceSub) }
            TextButton(onClick = onDone) { Text(if (L.isTr) "Ekle" else "Add", color = colors.primary) }
        }
        AnimatedVisibility(visible = expandedAuto) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 144.dp)
                    .padding(top = 4.dp)
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                filteredIngredients.forEach { selection ->
                    TextButton(onClick = {
                        onAddSelection(selection)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(selection, color = colors.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStart,
            enabled = canGenerate,
            modifier = Modifier.fillMaxWidth().height(48.dp),
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
private fun CompactKitchenSummary(pantryIntel: PantryIntelReport) {
    val colors = LocalAppColors.current
    val observation = pantryIntel.tactics.firstOrNull()?.let(::pantrySignalText)
        ?: pantryIntel.warnings.firstOrNull()?.let(::pantrySignalText)
        ?: return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(12.dp))
        Text(if (L.isTr) "MUTFAK ÖZETİ" else "KITCHEN SUMMARY", color = colors.success, style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(4.dp))
        Text(if (L.isTr) "Mutfak özeti" else "Kitchen summary", color = colors.onSurface, style = MaterialTheme.typography.h6)
        Spacer(Modifier.height(6.dp))
        Text(observation, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun PantryIntelOverviewCard(pantryIntel: PantryIntelReport, onEditSetup: () -> Unit) {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (L.isTr) "MUTFAK NOTLARI" else "KITCHEN NOTES",
                    color = colors.primary,
                    style = MaterialTheme.typography.overline,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (L.isTr) "Bugünkü mutfak görünümü" else "Today’s kitchen view",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h2
                )
            }
            TextButton(
                onClick = onEditSetup,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = if (L.isTr) "Kurulumu düzenle" else "Edit setup"
                    }
            ) {
                Text(
                    text = if (L.isTr) "Kurulumu düzenle" else "Edit setup",
                    color = colors.primary,
                    style = MaterialTheme.typography.button
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        val focusCategory = pantryCategoryLabel(pantryIntel.focusCategoryId)
        val readinessDescription = if (L.isTr) {
            "Hazırlık düzeyi ${pantryIntel.readinessScore}/100, $focusCategory"
        } else {
            "Readiness ${pantryIntel.readinessScore}/100, $focusCategory"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = readinessDescription },
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = pantryIntel.readinessScore.toString(),
                color = colors.onSurface,
                style = MaterialTheme.typography.h1
            )
            Text(
                text = "/100",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp, bottom = 6.dp)) {
                Text(
                    text = if (L.isTr) "Hazırlık düzeyi" else "Readiness",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption
                )
                Text(focusCategory, color = colors.onSurface, style = MaterialTheme.typography.body1)
            }
        }
        Spacer(Modifier.height(18.dp))
        Divider(color = colors.divider)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (L.isTr) "01 EKİPMAN" else "01 EQUIPMENT",
            color = colors.success,
            style = MaterialTheme.typography.overline,
            letterSpacing = 1.1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = equipmentLaneLabel(pantryIntel.equipmentLane),
            color = colors.onSurface,
            style = MaterialTheme.typography.body1
        )

        pantryIntel.warnings.take(1).forEach { warning ->
            Spacer(Modifier.height(16.dp))
            Divider(color = colors.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "${if (L.isTr) "Uyarı" else "Warning"}: ${pantrySignalText(warning)}"
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colors.error)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = pantrySignalText(warning),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body1
                )
            }
        }

        pantryIntel.tactics.take(2).forEachIndexed { index, tactic ->
            Spacer(Modifier.height(12.dp))
            Divider(color = colors.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "${if (L.isTr) "Not" else "Note"} ${index + 1}: ${pantrySignalText(tactic)}"
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "%02d".format(index + 1),
                    color = colors.success,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.width(34.dp)
                )
                Text(
                    text = pantrySignalText(tactic),
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Divider(color = colors.divider)
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

@Composable
private fun EditorialIngredientLibrarySection(onOpen: () -> Unit) {
    val colors = LocalAppColors.current
    val openLabel = if (L.isTr) "Malzeme kütüphanesini aç" else "Open ingredient library"

    Column(modifier = Modifier.padding(top = 28.dp, start = 24.dp, end = 24.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (L.isTr) "MALZEME KÜTÜPHANESİ" else "INGREDIENT LIBRARY",
            color = colors.primary,
            style = MaterialTheme.typography.overline,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (L.isTr) "Kategorilere göz at" else "Browse by category",
            color = colors.onSurface,
            style = MaterialTheme.typography.h2
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (L.isTr) {
                "Mutfağındaki malzemeleri kolayca bul ve ekle."
            } else {
                "Find what you have and add it to your kitchen."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(14.dp))

        INGREDIENT_CATEGORIES.forEachIndexed { index, category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClick = onOpen)
                    .semantics { contentDescription = "$openLabel: ${category.label}" }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%02d".format(index + 1),
                    color = colors.primary,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.width(38.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.label, color = colors.onSurface, style = MaterialTheme.typography.body1)
                    Text(
                        text = if (L.isTr) "${category.items.size} malzeme" else "${category.items.size} ingredients",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.caption
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(18.dp)
                )
            }
            Divider(color = colors.divider)
        }

        TextButton(
            onClick = onOpen,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                text = if (L.isTr) "Tümünü gör" else "View all",
                color = colors.primary,
                style = MaterialTheme.typography.button
            )
        }
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
            inventory = emptyList(),
            inventoryAdjustments = emptyMap(),
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
            onSaveInventoryItem = { _, _, _, _, _ -> },
            onDeleteInventoryItem = {},
            onClearAll = {},
            onStart = {},
            onEditSetup = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialKitchenSummaryPreview() {
    AgenticTheme("editorial") {
        PantryIntelOverviewCard(
            pantryIntel = PantryIntelReport(
                readinessScore = 72,
                focusCategoryId = "vegetation",
                focusCategoryLabel = "Vegetation",
                categoryBreakdown = emptyList(),
                warnings = listOf(PantryIntelSignal("needs_liquid", "")),
                tactics = listOf(
                    PantryIntelSignal("add_protein_anchor", ""),
                    PantryIntelSignal("rapid_pan_lane", "")
                ),
                equipmentLane = "rapid_pan"
            ),
            onEditSetup = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialKitchenSummaryQuietPreview() {
    AgenticTheme("editorial") {
        PantryIntelOverviewCard(
            pantryIntel = PantryIntelReport(
                readinessScore = 88,
                focusCategoryId = "carb_matrix",
                focusCategoryLabel = "Carb Matrix",
                categoryBreakdown = emptyList(),
                warnings = emptyList(),
                tactics = emptyList(),
                equipmentLane = "controlled_roast"
            ),
            onEditSetup = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialIngredientLibrarySectionPreview() {
    AgenticTheme("editorial") {
        EditorialIngredientLibrarySection(onOpen = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialIngredientPickerPreview() {
    AgenticTheme("editorial") {
        SideTabCategoryPicker(
            alreadyAdded = listOf("Domates", "Soğan"),
            colors = LocalAppColors.current,
            onAdd = {},
            onDismiss = {}
        )
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
    val closeLabel = if (L.isTr) "Kapat" else "Close"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            backgroundColor = colors.surface,
            shape = RoundedCornerShape(18.dp),
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 24.dp, end = 12.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (L.isTr) "MALZEME KÜTÜPHANESİ" else "INGREDIENT LIBRARY",
                            color = colors.primary,
                            style = MaterialTheme.typography.overline,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (L.isTr) "Mutfağına ekle" else "Add to your kitchen",
                            color = colors.onSurface,
                            style = MaterialTheme.typography.h2
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (L.isTr) {
                                "Kategoriyi seç, sonra eksik malzemeleri ekle."
                            } else {
                                "Choose a category, then add what is missing."
                            },
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.body1
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = closeLabel }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
                    }
                }
                Divider(color = colors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    INGREDIENT_CATEGORIES.forEachIndexed { index, category ->
                        val selected = selectedTabIndex == index
                        Column(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { selectedTabIndex = index }
                                .semantics {
                                    contentDescription = "${category.label}, ${if (selected) if (L.isTr) "seçili" else "selected" else ""}"
                                }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "%02d".format(index + 1),
                                color = if (selected) colors.primary else colors.onSurfaceSub,
                                style = MaterialTheme.typography.caption
                            )
                            Text(
                                text = category.label,
                                color = if (selected) colors.primary else colors.onSurface,
                                style = MaterialTheme.typography.caption,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.height(4.dp))
                            Divider(
                                color = if (selected) colors.primary else Color.Transparent,
                                thickness = 2.dp,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }
                }
                Divider(color = colors.divider)
                AnimatedContent(
                    targetState = selectedTabIndex,
                    modifier = Modifier.weight(1f),
                    label = "ingredient-category"
                ) { index ->
                    val category = INGREDIENT_CATEGORIES[index]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        category.items.map { if (L.isTr) it.first else it.second }.forEach { item ->
                            val added = alreadyAdded.any { it.equals(item, ignoreCase = true) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clickable(enabled = !added) { onAdd(item) }
                                    .semantics {
                                        contentDescription = if (added) {
                                            "$item, ${if (L.isTr) "eklendi" else "added"}"
                                        } else {
                                            "$item, ${if (L.isTr) "ekle" else "add"}"
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
                                if (added) {
                                    Text(
                                        text = if (L.isTr) "Eklendi" else "Added",
                                        color = colors.success,
                                        style = MaterialTheme.typography.caption
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(16.dp))
                                } else {
                                    Text(
                                        text = if (L.isTr) "Ekle" else "Add",
                                        color = colors.primary,
                                        style = MaterialTheme.typography.button
                                    )
                                }
                            }
                            Divider(color = colors.divider)
                        }
                    }
                }
                Divider(color = colors.divider)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = if (L.isTr) "Malzeme kütüphanesini tamamla" else "Finish ingredient library" }
                ) {
                    Text(
                        text = if (L.isTr) "Tamam" else "Done",
                        color = colors.primary,
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }
}

private data class EditableShoppingItem(
    val source: ShoppingCandidate,
    val included: Boolean = true,
    val name: String = source.displayName,
    val quantity: String = source.quantity?.let {
        if (it % 1.0 == 0.0) it.toLong().toString() else BigDecimal.valueOf(it).stripTrailingZeros().toPlainString()
    }.orEmpty(),
    val unit: String = source.unit.orEmpty()
) {
    fun candidateOrNull(): ShoppingCandidate? {
        val parsedQuantity = quantity.replace(',', '.').toDoubleOrNull()
        if (!included || name.isBlank() || parsedQuantity == null || parsedQuantity <= 0.0 || unit.isBlank()) return null
        return source.copy(displayName = name.trim(), quantity = parsedQuantity, unit = unit.trim())
    }
}

@Composable
private fun ShoppingImportDialog(
    state: ShoppingImportState,
    onDismiss: () -> Unit,
    onParseText: (String, ShoppingImportMode) -> Unit,
    onParsePhoto: (Bitmap, ShoppingImportMode) -> Unit,
    onConfirm: (List<ShoppingCandidate>, ShoppingImportMode) -> Boolean
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var textMode by remember { mutableStateOf(false) }
    var inventoryMode by remember { mutableStateOf(ShoppingImportMode.ADD) }
    var input by remember { mutableStateOf("") }
    val editable = remember { mutableStateListOf<EditableShoppingItem>() }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { onParsePhoto(it, inventoryMode) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bitmap = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
        bitmap?.let { onParsePhoto(it, inventoryMode) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
    }
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(state) {
        if (state is ShoppingImportState.Review) {
            inventoryMode = state.mode
            editable.clear()
            editable.addAll(state.candidates.map(::EditableShoppingItem))
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(.94f)
                .fillMaxHeight(.94f)
                .background(colors.surface, RoundedCornerShape(22.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(if (L.isTr) "MUTFAK STOĞU" else "KITCHEN INVENTORY", color = colors.primary, style = MaterialTheme.typography.overline)
                    Text(if (L.isTr) "Alışveriş ekle" else "Add shopping", color = colors.onSurface, style = MaterialTheme.typography.h3)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Kapat" else "Close", tint = colors.onSurfaceSub)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShoppingModeButton(
                    selected = inventoryMode == ShoppingImportMode.ADD,
                    label = if (L.isTr) "Alışveriş ekle" else "Add shopping"
                ) { inventoryMode = ShoppingImportMode.ADD }
                ShoppingModeButton(
                    selected = inventoryMode == ShoppingImportMode.RECOUNT,
                    label = if (L.isTr) "Mutfağı say" else "Recount kitchen"
                ) { inventoryMode = ShoppingImportMode.RECOUNT }
            }
            Text(
                if (inventoryMode == ShoppingImportMode.ADD) {
                    if (L.isTr) "Onaylanan miktarlar mevcut stoğa eklenir." else "Confirmed amounts are added to existing stock."
                } else {
                    if (L.isTr) "Yalnızca onayladığın ürünlerin miktarı değiştirilir; görünmeyenler silinmez." else "Only reviewed items are replaced; unseen stock is kept."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            Divider(color = colors.divider)
            when (state) {
                ShoppingImportState.Idle, is ShoppingImportState.Error -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                        ShoppingModeButton(!textMode, if (L.isTr) "Fotoğraf" else "Photo") { textMode = false }
                        ShoppingModeButton(textMode, if (L.isTr) "Metin" else "Text") { textMode = true }
                    }
                    if (state is ShoppingImportState.Error) {
                        Text(state.message, color = Color(0xFF9B3F32), style = MaterialTheme.typography.body2)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (textMode) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (L.isTr) "Ne aldın?" else "What did you buy?") },
                            placeholder = {
                                Text(
                                    if (L.isTr) "2 paket makarna, 1 kilo tavuk ve 12 yumurta"
                                    else "2 litres of milk, six tomatoes and a loaf of bread"
                                )
                            },
                            minLines = 4,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.divider,
                                textColor = colors.onSurface
                            )
                        )
                        Button(
                            onClick = { onParseText(input, inventoryMode) },
                            enabled = input.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "İncele" else "Review", color = colors.onPrimary)
                        }
                    } else {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IngredientArtwork("shopping", Modifier.size(120.dp))
                            TextButton(onClick = ::openCamera, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (L.isTr) "Fotoğraf çek" else "Take a photo", color = colors.primary)
                            }
                            TextButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (L.isTr) "Galeriden seç" else "Choose from gallery", color = colors.primary)
                            }
                        }
                    }
                }
                ShoppingImportState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (L.isTr) "Ürünler inceleniyor…" else "Reviewing products…", color = colors.onSurfaceSub)
                }
                is ShoppingImportState.Review -> {
                    Text(
                        if (L.isTr) "Yazmadan önce adları, miktarları ve birimleri doğrula." else "Confirm names, amounts, and units before saving.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    if (state.conflicts.isNotEmpty()) {
                        Text(
                            if (L.isTr) {
                                "Birimleri uyuşmayan ürünleri düzenle: ${state.conflicts.joinToString()}"
                            } else {
                                "Edit items with incompatible units: ${state.conflicts.joinToString()}"
                            },
                            color = Color(0xFF9B3F32),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        editable.forEachIndexed { index, item ->
                            ShoppingReviewRow(item) { editable[index] = it }
                        }
                    }
                    val selected = editable.mapNotNull(EditableShoppingItem::candidateOrNull)
                    Button(
                        onClick = {
                            if (onConfirm(selected, inventoryMode)) onDismiss()
                        },
                        enabled = selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(if (L.isTr) "Stoğa uygula" else "Apply to inventory", color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingModeButton(selected: Boolean, label: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .border(1.dp, if (selected) colors.primary else colors.divider, RoundedCornerShape(999.dp))
    ) {
        Text(label, color = if (selected) colors.primary else colors.onSurfaceSub)
    }
}

@Composable
private fun ShoppingReviewRow(item: EditableShoppingItem, onChange: (EditableShoppingItem) -> Unit) {
    val colors = LocalAppColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = item.included,
                onCheckedChange = { onChange(item.copy(included = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
            )
            OutlinedTextField(
                value = item.name,
                onValueChange = { onChange(item.copy(name = it)) },
                modifier = Modifier.weight(1f),
                label = { Text(if (L.isTr) "Ürün" else "Item") },
                singleLine = true
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.quantity,
                onValueChange = { onChange(item.copy(quantity = it)) },
                modifier = Modifier.weight(1f),
                label = { Text(if (L.isTr) "Miktar" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                value = item.unit,
                onValueChange = { onChange(item.copy(unit = it)) },
                modifier = Modifier.weight(1f),
                label = { Text(if (L.isTr) "Birim" else "Unit") },
                singleLine = true
            )
        }
        item.source.packageLabel?.let {
            Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        }
        if (item.source.estimated || item.source.confidence < .75 || item.source.uncertaintyReason != null) {
            Text(
                buildString {
                    append(if (L.isTr) "Kontrol et" else "Check")
                    append(" · ${(item.source.confidence * 100).toInt()}%")
                    item.source.uncertaintyReason?.let { append(" · $it") }
                },
                color = colors.primary,
                style = MaterialTheme.typography.caption
            )
        }
        Divider(color = colors.divider, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun InventoryRecipeDialog(
    onDismiss: () -> Unit,
    onStart: (InventoryRecipeRequest) -> Unit
) {
    val colors = LocalAppColors.current
    var servings by remember { mutableStateOf(2) }
    var strictStock by remember { mutableStateOf(true) }
    var missingStaples by remember { mutableStateOf(0) }
    var priority by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(22.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Text(if (L.isTr) "ELİNDEKİLERLE" else "FROM YOUR PANTRY", color = colors.primary, style = MaterialTheme.typography.overline)
            Text(if (L.isTr) "Ne pişirelim?" else "What should we cook?", color = colors.onSurface, style = MaterialTheme.typography.h3)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (L.isTr) "Kişi sayısı" else "Servings", color = colors.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = { servings = (servings - 1).coerceAtLeast(1) }) { Text("−", color = colors.primary) }
                Text(servings.toString(), color = colors.onSurface, style = MaterialTheme.typography.h5)
                TextButton(onClick = { servings = (servings + 1).coerceAtMost(12) }) { Text("+", color = colors.primary) }
            }
            Divider(color = colors.divider)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 56.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (L.isTr) "Yalnızca mevcut stok" else "Use existing stock only", color = colors.onSurface)
                    Text(if (L.isTr) "Eksik ürünle tarif başlatılmaz." else "Recipes with shortages are blocked.", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
                }
                Switch(checked = strictStock, onCheckedChange = { strictStock = it })
            }
            if (!strictStock) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (L.isTr) "Eksik temel ürün" else "Missing staples", modifier = Modifier.weight(1f), color = colors.onSurface)
                    (0..2).forEach { value ->
                        TextButton(onClick = { missingStaples = value }) {
                            Text(value.toString(), color = if (missingStaples == value) colors.primary else colors.onSurfaceSub)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = priority,
                onValueChange = { priority = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (L.isTr) "Öncelik ver (isteğe bağlı)" else "Prioritize (optional)") },
                placeholder = { Text(if (L.isTr) "Örn. tavuk, ıspanak" else "E.g. chicken, spinach") }
            )
            Text(
                if (L.isTr) "Hazır olma saatini tarif seçerken belirleyeceksin." else "You will choose the ready time after selecting a recipe.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Button(
                onClick = {
                    onStart(
                        InventoryRecipeRequest(
                            servings,
                            strictStock,
                            if (strictStock) 0 else missingStaples,
                            priority.split(',').map(String::trim).filter(String::isNotEmpty)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Tarifleri bul" else "Find recipes", color = colors.onPrimary)
            }
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
