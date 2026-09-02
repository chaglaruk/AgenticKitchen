package com.agentickitchen.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.L
import com.agentickitchen.shared.inventory.PantryFreshnessInfo
import com.agentickitchen.shared.inventory.PantryFreshnessPolicy
import com.agentickitchen.shared.inventory.PantryFreshnessStatus
import com.agentickitchen.shared.inventory.PantryInventoryView
import com.agentickitchen.shared.inventory.PantryLocation
import com.agentickitchen.shared.inventory.PantrySortOrder
import com.agentickitchen.shared.inventory.PantryStockItem
import java.math.BigDecimal

private enum class KitchenHubMode { INGREDIENTS, PANTRY }

@Composable
fun KitchenHubScreen(
    inventory: List<PantryStockItem>,
    onSaveInventoryItem: (PantryStockItem?, String, Double, String, String?) -> Unit,
    onDeleteInventoryItem: (PantryStockItem) -> Unit,
    onUpdateMetadata: (PantryStockItem) -> Boolean,
    homeContent: @Composable () -> Unit
) {
    val colors = LocalAppColors.current
    var mode by remember { mutableStateOf(KitchenHubMode.INGREDIENTS) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KitchenHubModeButton(
                selected = mode == KitchenHubMode.INGREDIENTS,
                label = if (L.isTr) "Malzemeler" else "Ingredients",
                modifier = Modifier.weight(1f)
            ) { mode = KitchenHubMode.INGREDIENTS }
            KitchenHubModeButton(
                selected = mode == KitchenHubMode.PANTRY,
                label = if (L.isTr) "Stok" else "Pantry",
                modifier = Modifier.weight(1f)
            ) { mode = KitchenHubMode.PANTRY }
        }
        Divider(color = colors.divider)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (mode == KitchenHubMode.INGREDIENTS) {
                homeContent()
            } else {
                SmartPantryScreen(
                    inventory = inventory,
                    onSaveInventoryItem = onSaveInventoryItem,
                    onDeleteInventoryItem = onDeleteInventoryItem,
                    onUpdateMetadata = onUpdateMetadata
                )
            }
        }
    }
}

@Composable
private fun KitchenHubModeButton(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 44.dp)
            .border(1.dp, if (selected) colors.primary else colors.divider, RoundedCornerShape(999.dp))
            .semantics { contentDescription = label }
    ) {
        Text(
            label,
            color = if (selected) colors.primary else colors.onSurfaceSub,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SmartPantryScreen(
    inventory: List<PantryStockItem>,
    onSaveInventoryItem: (PantryStockItem?, String, Double, String, String?) -> Unit,
    onDeleteInventoryItem: (PantryStockItem) -> Unit,
    onUpdateMetadata: (PantryStockItem) -> Boolean
) {
    val colors = LocalAppColors.current
    var selectedLocation by remember { mutableStateOf<PantryLocation?>(null) }
    var sortOrder by remember { mutableStateOf(PantrySortOrder.EXPIRY) }
    var editingItem by remember { mutableStateOf<PantryStockItem?>(null) }
    val visibleItems = PantryInventoryView.filterAndSort(inventory, selectedLocation, sortOrder)
    val useFirst = PantryInventoryView.useFirst(inventory)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (L.isTr) "AKILLI STOK" else "SMART PANTRY",
                    color = colors.primary,
                    style = MaterialTheme.typography.overline
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (L.isTr) "Önce neyi kullanalım?" else "What should we use first?",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (L.isTr) "Konum ve tarihlere göre mutfağındaki stoğu takip et." else "Track your kitchen by location and freshness dates.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body1
                )
            }
        }

        if (useFirst.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UseFirstStrip(useFirst) { editingItem = it }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            PantryLocationFilters(selectedLocation) { selectedLocation = it }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            PantrySortControls(sortOrder) { sortOrder = it }
        }

        if (visibleItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)) {
                    Text(
                        if (inventory.isEmpty()) {
                            if (L.isTr) "Henüz stok kaydı yok." else "No pantry items yet."
                        } else {
                            if (L.isTr) "Bu konumda ürün yok." else "No items in this location."
                        },
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        if (L.isTr) "Yeni ürünü Malzemeler görünümünden ekleyebilirsin." else "Add new items from the Ingredients view.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        } else {
            items(visibleItems, key = { it.id }) { item ->
                SmartPantryCard(item) { editingItem = item }
            }
        }
    }

    editingItem?.let { item ->
        SmartPantryItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, quantity, unit, packageLabel, location, customLocation, bestBefore, useBy ->
                onSaveInventoryItem(item, name, quantity, unit, packageLabel)
                val updated = onUpdateMetadata(
                    item.copy(
                        location = location,
                        customLocationLabel = customLocation,
                        bestBefore = bestBefore,
                        useBy = useBy
                    )
                )
                if (updated) editingItem = null
                updated
            },
            onRanOut = {
                onDeleteInventoryItem(item)
                editingItem = null
            }
        )
    }
}

@Composable
private fun UseFirstStrip(items: List<PantryStockItem>, onOpen: (PantryStockItem) -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Divider(color = colors.divider)
        Spacer(Modifier.height(12.dp))
        Text(if (L.isTr) "ÖNCE BUNLARI KULLAN" else "USE FIRST", color = colors.success, style = MaterialTheme.typography.overline)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val info = PantryFreshnessPolicy.evaluate(item)
                Column(
                    modifier = Modifier
                        .width(148.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceAlt)
                        .clickable { onOpen(item) }
                        .padding(12.dp)
                ) {
                    Text(item.originalName, color = colors.onSurface, style = MaterialTheme.typography.subtitle2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(freshnessLabel(info, L.isTr), color = freshnessColor(info.status), style = MaterialTheme.typography.caption)
                }
            }
        }
    }
}

@Composable
private fun PantryLocationFilters(selected: PantryLocation?, onSelect: (PantryLocation?) -> Unit) {
    val choices = listOf<PantryLocation?>(null) + PantryLocation.entries
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        choices.forEach { location ->
            PantryFilterPill(
                selected = selected == location,
                label = locationLabel(location, L.isTr)
            ) { onSelect(location) }
        }
    }
}

@Composable
private fun PantrySortControls(selected: PantrySortOrder, onSelect: (PantrySortOrder) -> Unit) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (L.isTr) "Sırala" else "Sort", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PantrySortOrder.entries.forEach { order ->
                PantryFilterPill(selected == order, sortLabel(order, L.isTr)) { onSelect(order) }
            }
        }
    }
}

@Composable
private fun PantryFilterPill(selected: Boolean, label: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 42.dp)
            .border(1.dp, if (selected) colors.primary else colors.divider, RoundedCornerShape(999.dp))
    ) {
        Text(label, color = if (selected) colors.primary else colors.onSurfaceSub, fontSize = 12.sp)
    }
}

@Composable
private fun SmartPantryCard(item: PantryStockItem, onOpen: () -> Unit) {
    val colors = LocalAppColors.current
    val freshness = PantryFreshnessPolicy.evaluate(item)
    val statusText = freshnessLabel(freshness, L.isTr)
    val locationText = displayLocation(item, L.isTr)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceAlt)
            .clickable(onClick = onOpen)
            .padding(8.dp)
            .semantics {
                contentDescription = "${item.originalName}, ${formatPantryQuantity(item)}, $locationText, $statusText"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(locationText.uppercase(), color = colors.onSurfaceSub, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        IngredientArtwork(item.originalName, Modifier.weight(1f).fillMaxWidth().padding(3.dp))
        Text(
            item.originalName,
            color = colors.onSurface,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        Text(formatPantryQuantity(item), color = colors.primary, style = MaterialTheme.typography.caption, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .border(1.dp, freshnessColor(freshness.status), RoundedCornerShape(999.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(statusText, color = freshnessColor(freshness.status), fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun freshnessColor(status: PantryFreshnessStatus) = when (status) {
    PantryFreshnessStatus.EXPIRED -> MaterialTheme.colors.error
    PantryFreshnessStatus.EXPIRES_TODAY -> LocalAppColors.current.primary
    PantryFreshnessStatus.USE_SOON -> LocalAppColors.current.success
    PantryFreshnessStatus.LOW_STOCK -> LocalAppColors.current.primary
    PantryFreshnessStatus.FRESH -> LocalAppColors.current.onSurfaceSub
}

private fun freshnessLabel(info: PantryFreshnessInfo, isTurkish: Boolean): String = when (info.status) {
    PantryFreshnessStatus.FRESH -> if (isTurkish) "Taze" else "Fresh"
    PantryFreshnessStatus.USE_SOON -> if (isTurkish) {
        info.daysUntilDate?.let { "$it gün içinde" } ?: "Yakında kullan"
    } else {
        info.daysUntilDate?.let { "Use in ${it}d" } ?: "Use soon"
    }
    PantryFreshnessStatus.EXPIRES_TODAY -> if (isTurkish) "Bugün kullan" else "Use today"
    PantryFreshnessStatus.EXPIRED -> if (isTurkish) "Süresi geçti" else "Expired"
    PantryFreshnessStatus.LOW_STOCK -> if (isTurkish) "Az kaldı" else "Low stock"
}

private fun locationLabel(location: PantryLocation?, isTurkish: Boolean): String = when (location) {
    null -> if (isTurkish) "Tümü" else "All"
    PantryLocation.FRIDGE -> if (isTurkish) "Buzdolabı" else "Fridge"
    PantryLocation.FREEZER -> if (isTurkish) "Dondurucu" else "Freezer"
    PantryLocation.PANTRY -> if (isTurkish) "Kiler" else "Pantry"
    PantryLocation.COUNTER -> if (isTurkish) "Tezgâh" else "Counter"
    PantryLocation.OTHER -> if (isTurkish) "Diğer" else "Other"
}

private fun displayLocation(item: PantryStockItem, isTurkish: Boolean): String =
    if (item.location == PantryLocation.OTHER && !item.customLocationLabel.isNullOrBlank()) {
        item.customLocationLabel.orEmpty()
    } else {
        locationLabel(item.location, isTurkish)
    }

private fun sortLabel(order: PantrySortOrder, isTurkish: Boolean): String = when (order) {
    PantrySortOrder.EXPIRY -> if (isTurkish) "Tarih" else "Expiry"
    PantrySortOrder.NAME -> if (isTurkish) "İsim" else "Name"
    PantrySortOrder.QUANTITY -> if (isTurkish) "Miktar" else "Quantity"
}

private fun formatPantryQuantity(item: PantryStockItem): String =
    "${BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()} ${item.unit}"

@Composable
private fun SmartPantryItemDialog(
    item: PantryStockItem,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        quantity: Double,
        unit: String,
        packageLabel: String?,
        location: PantryLocation,
        customLocation: String?,
        bestBefore: String?,
        useBy: String?
    ) -> Boolean,
    onRanOut: () -> Unit
) {
    val colors = LocalAppColors.current
    var name by remember(item.id) { mutableStateOf(item.originalName) }
    var quantityText by remember(item.id) { mutableStateOf(BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()) }
    var unit by remember(item.id) { mutableStateOf(item.unit) }
    var packageLabel by remember(item.id) { mutableStateOf(item.packageLabel.orEmpty()) }
    var location by remember(item.id) { mutableStateOf(item.location) }
    var customLocation by remember(item.id) { mutableStateOf(item.customLocationLabel.orEmpty()) }
    var bestBefore by remember(item.id) { mutableStateOf(item.bestBefore.orEmpty()) }
    var useBy by remember(item.id) { mutableStateOf(item.useBy.orEmpty()) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }

    val bestBeforeValid = bestBefore.isBlank() || PantryFreshnessPolicy.parseIsoDate(bestBefore) != null
    val useByValid = useBy.isBlank() || PantryFreshnessPolicy.parseIsoDate(useBy) != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.92f),
            backgroundColor = colors.surface,
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(if (L.isTr) "STOK DETAYI" else "PANTRY DETAIL", color = colors.primary, style = MaterialTheme.typography.overline)
                Text(item.originalName, color = colors.onSurface, style = MaterialTheme.typography.h3)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (L.isTr) "Malzeme" else "Ingredient") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text(if (L.isTr) "Miktar" else "Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("g", "kg", "ml", "L", "adet", "paket", "demet").forEach { choice ->
                        TextButton(onClick = { unit = choice }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(choice, color = if (unit == choice) colors.primary else colors.onSurfaceSub)
                        }
                    }
                }

                Divider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))
                Text(if (L.isTr) "Konum" else "Location", color = colors.onSurface, style = MaterialTheme.typography.subtitle2)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PantryLocation.entries.forEach { choice ->
                        PantryFilterPill(location == choice, locationLabel(choice, L.isTr)) { location = choice }
                    }
                }
                if (location == PantryLocation.OTHER) {
                    OutlinedTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        label = { Text(if (L.isTr) "Konum adı" else "Location name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Divider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))
                Text(if (L.isTr) "Tazelik tarihleri" else "Freshness dates", color = colors.onSurface, style = MaterialTheme.typography.subtitle2)
                Text(
                    if (L.isTr) "Tarihleri YYYY-AA-GG biçiminde yaz. İkisi de isteğe bağlı." else "Use YYYY-MM-DD. Both dates are optional.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = useBy,
                    onValueChange = { useBy = it },
                    isError = !useByValid,
                    label = { Text(if (L.isTr) "Son tüketim" else "Use by") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bestBefore,
                    onValueChange = { bestBefore = it },
                    isError = !bestBeforeValid,
                    label = { Text(if (L.isTr) "Tavsiye edilen tüketim" else "Best before") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageLabel,
                    onValueChange = { packageLabel = it },
                    label = { Text(if (L.isTr) "Paket notu" else "Package note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val quantity = quantityText.replace(',', '.').toDoubleOrNull()
                        when {
                            name.isBlank() || quantity == null || !quantity.isFinite() || quantity <= 0.0 -> {
                                error = if (L.isTr) "Geçerli bir ad ve miktar gir." else "Enter a valid name and quantity."
                            }
                            !bestBeforeValid || !useByValid -> {
                                error = if (L.isTr) "Tarih biçimini kontrol et." else "Check the date format."
                            }
                            location == PantryLocation.OTHER && customLocation.isBlank() -> {
                                error = if (L.isTr) "Diğer konum için bir ad yaz." else "Name the custom location."
                            }
                            else -> {
                                val saved = onSave(
                                    name.trim(),
                                    quantity,
                                    unit,
                                    packageLabel.trim().takeIf(String::isNotEmpty),
                                    location,
                                    customLocation.trim().takeIf { location == PantryLocation.OTHER && it.isNotEmpty() },
                                    bestBefore.trim().takeIf(String::isNotEmpty),
                                    useBy.trim().takeIf(String::isNotEmpty)
                                )
                                if (!saved) error = if (L.isTr) "Stok detayı kaydedilemedi." else "Pantry details could not be saved."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Kaydet" else "Save", color = colors.onPrimary)
                }
                TextButton(onClick = onRanOut, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (L.isTr) "Bitti — stoktan çıkar" else "Ran out — remove from pantry", color = MaterialTheme.colors.error)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
                }
            }
        }
    }
}
