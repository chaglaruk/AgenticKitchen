package com.agentickitchen.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.KitchenScanState
import com.agentickitchen.android.L
import com.agentickitchen.shared.ai.ShoppingCandidate
import com.agentickitchen.shared.inventory.InventoryUnits
import com.agentickitchen.shared.inventory.LocatedShoppingCandidate
import com.agentickitchen.shared.inventory.PantryLocation
import java.math.BigDecimal

@Composable
fun KitchenScanDialog(
    state: KitchenScanState,
    onDismiss: () -> Unit,
    onScanPhoto: (Bitmap, PantryLocation) -> Unit,
    onUpdateDraft: (List<LocatedShoppingCandidate>) -> Unit,
    onConfirm: (List<LocatedShoppingCandidate>) -> Boolean
) {
    val context = LocalContext.current
    var candidates by remember { mutableStateOf<List<LocatedShoppingCandidate>>(emptyList()) }
    var pendingLocation by remember { mutableStateOf<PantryLocation?>(null) }

    val stateCandidates = when (state) {
        KitchenScanState.Idle -> emptyList()
        is KitchenScanState.Loading -> state.candidates
        is KitchenScanState.Review -> state.candidates
        is KitchenScanState.Error -> state.candidates
    }
    val scannedLocations = when (state) {
        KitchenScanState.Idle -> emptySet()
        is KitchenScanState.Loading -> state.scannedLocations
        is KitchenScanState.Review -> state.scannedLocations
        is KitchenScanState.Error -> state.scannedLocations
    }
    val conflicts = (state as? KitchenScanState.Review)?.conflicts.orEmpty()

    LaunchedEffect(state) {
        candidates = stateCandidates
    }

    fun update(next: List<LocatedShoppingCandidate>) {
        candidates = next
        onUpdateDraft(next)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val location = pendingLocation
        if (bitmap != null && location != null) onScanPhoto(bitmap, location)
        pendingLocation = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val location = pendingLocation
        if (uri != null && location != null) {
            runCatching { loadKitchenBitmap(context, uri) }
                .onSuccess { onScanPhoto(it, location) }
                .onFailure {
                    Toast.makeText(context, if (L.isTr) "Görsel yüklenemedi." else "The image could not be loaded.", Toast.LENGTH_SHORT).show()
                }
        }
        pendingLocation = null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else {
            pendingLocation = null
            Toast.makeText(context, if (L.isTr) "Kamera izni reddedildi." else "Camera permission was denied.", Toast.LENGTH_SHORT).show()
        }
    }

    fun capture(location: PantryLocation) {
        if (state is KitchenScanState.Loading) return
        pendingLocation = location
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    fun gallery(location: PantryLocation) {
        if (state is KitchenScanState.Loading) return
        pendingLocation = location
        galleryLauncher.launch("image/*")
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val colors = LocalAppColors.current
        Card(
            modifier = Modifier.fillMaxWidth(.94f).heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .92f),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = colors.surface,
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider)
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (L.isTr) "MUTFAK TARAMASI" else "KITCHEN SCAN", color = colors.primary, style = MaterialTheme.typography.overline)
                        Text(if (L.isTr) "Dört görünüm, tek kontrollü stok" else "Four views, one reviewed inventory", color = colors.onSurface, style = MaterialTheme.typography.h5)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Kapat" else "Close", tint = colors.onSurfaceSub) }
                }
                Text(
                    if (L.isTr) "Buzdolabı, dondurucu, kiler ve tezgâhı ayrı tara. Bulunan hiçbir ürün onaylamadan stoğa yazılmaz."
                    else "Scan fridge, freezer, pantry and counter separately. Nothing is written to inventory until you confirm it.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2
                )
                Spacer(Modifier.height(14.dp))

                listOf(PantryLocation.FRIDGE, PantryLocation.FREEZER, PantryLocation.PANTRY, PantryLocation.COUNTER).forEach { location ->
                    KitchenLocationCaptureRow(
                        location = location,
                        scanned = location in scannedLocations,
                        itemCount = candidates.count { it.location == location },
                        busy = state is KitchenScanState.Loading,
                        onCamera = { capture(location) },
                        onGallery = { gallery(location) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (state is KitchenScanState.Loading) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.width(22.dp), strokeWidth = 2.dp, color = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (L.isTr) "${locationLabel(state.location)} görüntüsü inceleniyor…" else "Scanning ${locationLabel(state.location)}…",
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
                if (state is KitchenScanState.Error) {
                    Text(state.message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.body2, modifier = Modifier.padding(vertical = 8.dp))
                }
                if (conflicts.isNotEmpty()) {
                    Text(
                        if (L.isTr) "Önce konum/miktar çakışmalarını düzelt: ${conflicts.joinToString()}"
                        else "Resolve location/quantity conflicts first: ${conflicts.joinToString()}",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Divider(color = colors.divider, modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (L.isTr) "İNCELE" else "REVIEW", color = colors.primary, style = MaterialTheme.typography.overline)
                        Text(if (L.isTr) "Bulunan ürünler" else "Detected items", color = colors.onSurface, style = MaterialTheme.typography.h6)
                    }
                    TextButton(
                        onClick = {
                            update(
                                candidates + LocatedShoppingCandidate(
                                    ShoppingCandidate(
                                        displayName = "",
                                        quantity = 1.0,
                                        unit = "adet",
                                        unitDimension = "count",
                                        confidence = 1.0,
                                        estimated = false
                                    ),
                                    PantryLocation.PANTRY
                                )
                            )
                        },
                        enabled = state !is KitchenScanState.Loading
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary)
                        Text(if (L.isTr) "Ürün ekle" else "Add item", color = colors.primary)
                    }
                }

                if (candidates.isEmpty()) {
                    Text(
                        if (L.isTr) "Henüz ürün bulunmadı. Bir veya daha fazla görünüm tara."
                        else "No items yet. Scan one or more kitchen views.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    candidates.forEachIndexed { index, located ->
                        KitchenCandidateEditor(
                            located = located,
                            onChange = { changed -> update(candidates.toMutableList().also { it[index] = changed }) },
                            onRemove = { update(candidates.filterIndexed { candidateIndex, _ -> candidateIndex != index }) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                val ready = candidates.isNotEmpty() && candidates.all { located ->
                    val c = located.candidate
                    c.displayName.isNotBlank() && c.quantity?.let { it.isFinite() && it > 0.0 } == true && !c.unit.isNullOrBlank()
                } && state !is KitchenScanState.Loading
                Button(
                    onClick = { onConfirm(candidates) },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(if (L.isTr) "Onayla ve stoğa ekle" else "Confirm and add to inventory", color = colors.onPrimary)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
                }
            }
        }
    }
}

@Composable
private fun KitchenLocationCaptureRow(
    location: PantryLocation,
    scanned: Boolean,
    itemCount: Int,
    busy: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(locationLabel(location), color = colors.onSurface, style = MaterialTheme.typography.subtitle1)
            Text(
                when {
                    scanned && itemCount > 0 -> if (L.isTr) "$itemCount ürün bulundu" else "$itemCount items detected"
                    scanned -> if (L.isTr) "Tarandı · ürün bulunmadı" else "Scanned · no items detected"
                    else -> if (L.isTr) "Henüz taranmadı" else "Not scanned yet"
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
        }
        IconButton(onClick = onCamera, enabled = !busy) { Icon(Icons.Filled.PhotoCamera, contentDescription = if (L.isTr) "Fotoğraf çek" else "Take photo", tint = colors.primary) }
        IconButton(onClick = onGallery, enabled = !busy) { Icon(Icons.Filled.Image, contentDescription = if (L.isTr) "Galeriden seç" else "Choose from gallery", tint = colors.primary) }
    }
}

@Composable
private fun KitchenCandidateEditor(
    located: LocatedShoppingCandidate,
    onChange: (LocatedShoppingCandidate) -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalAppColors.current
    val candidate = located.candidate
    var locationMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = colors.surfaceAlt,
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (L.isTr) "Güven %${(candidate.confidence.coerceIn(0.0, 1.0) * 100).toInt()}" else "Confidence ${(candidate.confidence.coerceIn(0.0, 1.0) * 100).toInt()}%",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Ürünü kaldır" else "Remove item", tint = colors.onSurfaceSub) }
            }
            OutlinedTextField(
                value = candidate.displayName,
                onValueChange = { onChange(located.copy(candidate = candidate.copy(displayName = it))) },
                label = { Text(if (L.isTr) "Ürün" else "Item") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = candidate.quantity?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
                    onValueChange = { value ->
                        onChange(located.copy(candidate = candidate.copy(quantity = value.replace(',', '.').toDoubleOrNull())))
                    },
                    label = { Text(if (L.isTr) "Miktar" else "Qty") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = candidate.unit.orEmpty(),
                    onValueChange = { unit ->
                        val dimension = runCatching { InventoryUnits.normalize(1.0, unit).dimension.name.lowercase() }.getOrDefault("unknown")
                        onChange(located.copy(candidate = candidate.copy(unit = unit, unitDimension = dimension)))
                    },
                    label = { Text(if (L.isTr) "Birim" else "Unit") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Box {
                TextButton(onClick = { locationMenu = true }) {
                    Text((if (L.isTr) "Konum: " else "Location: ") + locationLabel(located.location), color = colors.primary)
                }
                DropdownMenu(expanded = locationMenu, onDismissRequest = { locationMenu = false }) {
                    PantryLocation.values().forEach { location ->
                        DropdownMenuItem(onClick = {
                            locationMenu = false
                            onChange(located.copy(location = location))
                        }) { Text(locationLabel(location)) }
                    }
                }
            }
            candidate.uncertaintyReason?.takeIf(String::isNotBlank)?.let { reason ->
                Text(
                    (if (L.isTr) "Belirsizlik: " else "Uncertainty: ") + reason,
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

private fun locationLabel(location: PantryLocation): String = when (location) {
    PantryLocation.FRIDGE -> if (L.isTr) "Buzdolabı" else "Fridge"
    PantryLocation.FREEZER -> if (L.isTr) "Dondurucu" else "Freezer"
    PantryLocation.PANTRY -> if (L.isTr) "Kiler" else "Pantry"
    PantryLocation.COUNTER -> if (L.isTr) "Tezgâh" else "Counter"
    PantryLocation.OTHER -> if (L.isTr) "Diğer" else "Other"
}

@Suppress("DEPRECATION")
private fun loadKitchenBitmap(context: android.content.Context, uri: Uri): Bitmap =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
