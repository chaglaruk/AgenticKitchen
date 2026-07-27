package com.agentickitchen.android.ui

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.L

private enum class CameraModalPhase { Idle, Scanning, Results }

@Composable
fun CameraModal(
    scannedIngredients: List<String>?,
    onDismiss: () -> Unit,
    onAcceptScan: (List<String>) -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    var scanState by remember { mutableStateOf(CameraModalPhase.Idle) }
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scanState = CameraModalPhase.Scanning
            onImageCaptured(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                scanState = CameraModalPhase.Scanning
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onImageCaptured(bitmap)
            } catch (e: Exception) {
                scanState = CameraModalPhase.Idle
                Toast.makeText(
                    context,
                    if (L.isTr) "Görsel yüklenemedi: ${e.localizedMessage}" else "Could not load image: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (L.isTr) "Kamera başlatılamadı: ${e.localizedMessage}" else "Could not open camera: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                if (L.isTr) "Kamera izni reddedildi." else "Camera permission was denied.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var localScanned by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(scannedIngredients) {
        if (scannedIngredients != null && scanState == CameraModalPhase.Scanning) {
            if (scannedIngredients.firstOrNull() == "__ERROR__") {
                onDismiss()
            } else {
                localScanned = scannedIngredients
                scanState = CameraModalPhase.Results
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CameraModalContent(
            phase = scanState,
            localScanned = localScanned,
            onDismiss = onDismiss,
            onTakePhoto = {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (L.isTr) "Kamera veya galeri başlatılamadı: ${e.localizedMessage}" else "Could not open camera or gallery: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onChooseGallery = {
                try {
                    galleryLauncher.launch("image/*")
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (L.isTr) "Kamera veya galeri başlatılamadı: ${e.localizedMessage}" else "Could not open camera or gallery: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRemoveIngredient = { ingredient ->
                localScanned = localScanned.filter { it != ingredient }
            },
            onAccept = { onAcceptScan(localScanned) }
        )
    }
}

@Composable
private fun CameraModalContent(
    phase: CameraModalPhase,
    localScanned: List<String>,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAccept: () -> Unit
) {
    val colors = LocalAppColors.current
    val closeLabel = if (L.isTr) "Kapat" else "Close"

    Card(
        modifier = Modifier.fillMaxWidth(0.94f).wrapContentHeight(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = colors.surface,
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = if (L.isTr) "GÖRSEL MALZEME TARAMASI" else "VISUAL INGREDIENT SCAN",
                    color = colors.primary,
                    style = MaterialTheme.typography.overline,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f).padding(top = 10.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = closeLabel }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
                }
            }
            Spacer(Modifier.height(10.dp))
            Divider(color = colors.divider)

            when (phase) {
                CameraModalPhase.Idle -> CameraIdleContent(
                    onTakePhoto = onTakePhoto,
                    onChooseGallery = onChooseGallery
                )
                CameraModalPhase.Scanning -> CameraScanningContent()
                CameraModalPhase.Results -> CameraResultsContent(
                    localScanned = localScanned,
                    onDismiss = onDismiss,
                    onRemoveIngredient = onRemoveIngredient,
                    onAccept = onAccept
                )
            }
        }
    }
}

@Composable
private fun CameraIdleContent(onTakePhoto: () -> Unit, onChooseGallery: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(
            text = if (L.isTr) "Fotoğraftan malzeme ekle" else "Add ingredients from a photo",
            color = colors.onSurface,
            style = MaterialTheme.typography.h2
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (L.isTr) {
                "Bir fotoğraf çek veya galerinden seç. Bulunan malzemeleri listeye eklemeden önce gözden geçirebilirsin."
            } else {
                "Take a photo or choose one from your gallery. You can review detected ingredients before adding them."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(20.dp))
        EditorialCaptureAction(
            icon = Icons.Filled.PhotoCamera,
            title = if (L.isTr) "Fotoğraf çek" else "Take a photo",
            subtitle = if (L.isTr) "Kamera izni gerektiğinde istenir." else "Camera permission is requested when needed.",
            onClick = onTakePhoto
        )
        Divider(color = colors.divider)
        EditorialCaptureAction(
            icon = Icons.Filled.Image,
            title = if (L.isTr) "Galeriden seç" else "Choose from gallery",
            subtitle = if (L.isTr) "Mevcut bir görseli incele." else "Review an existing image.",
            onClick = onChooseGallery
        )
    }
}

@Composable
private fun EditorialCaptureAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = title }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, style = MaterialTheme.typography.body1, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun CameraScanningContent() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .semantics {
                contentDescription = if (L.isTr) "Fotoğraf inceleniyor, işlem sürüyor" else "Reviewing the photo, processing in progress"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (L.isTr) "Fotoğraf inceleniyor" else "Reviewing the photo",
            color = colors.onSurface,
            style = MaterialTheme.typography.h2
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (L.isTr) "Malzemeler belirleniyor." else "Identifying the ingredients.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun CameraResultsContent(
    localScanned: List<String>,
    onDismiss: () -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAccept: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(
            text = if (L.isTr) "Bulunan malzemeler" else "Detected ingredients",
            color = colors.onSurface,
            style = MaterialTheme.typography.h2
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (L.isTr) {
                "Listeye eklemeden önce bulunanları gözden geçir ve istemediklerini çıkar."
            } else {
                "Review the detected ingredients and remove anything you do not want before adding them."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(16.dp))

        if (localScanned.isEmpty()) {
            Text(
                text = if (L.isTr) "Bu listede malzeme kalmadı." else "There are no ingredients left in this list.",
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            localScanned.forEachIndexed { index, ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .semantics { contentDescription = "${index + 1}. $ingredient" }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d".format(index + 1),
                        color = colors.success,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.width(34.dp)
                    )
                    Text(
                        text = ingredient,
                        color = colors.onSurface,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onRemoveIngredient(ingredient) },
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = if (L.isTr) "$ingredient malzemesini kaldır" else "Remove $ingredient"
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
                    }
                }
                Divider(color = colors.divider)
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = if (L.isTr) "İptal" else "Cancel" }
            ) {
                Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
            }
            TextButton(
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(colors.primary, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = if (L.isTr) "Listeye ekle" else "Add to list" }
            ) {
                Text(
                    text = if (L.isTr) "Listeye ekle" else "Add to list",
                    color = colors.onPrimary,
                    style = MaterialTheme.typography.button
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialCameraIdlePreview() {
    AgenticTheme("editorial") {
        CameraModalContent(
            phase = CameraModalPhase.Idle,
            localScanned = emptyList(),
            onDismiss = {},
            onTakePhoto = {},
            onChooseGallery = {},
            onRemoveIngredient = {},
            onAccept = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialCameraScanningPreview() {
    AgenticTheme("editorial") {
        CameraModalContent(
            phase = CameraModalPhase.Scanning,
            localScanned = emptyList(),
            onDismiss = {},
            onTakePhoto = {},
            onChooseGallery = {},
            onRemoveIngredient = {},
            onAccept = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorialCameraResultsPreview() {
    AgenticTheme("editorial") {
        CameraModalContent(
            phase = CameraModalPhase.Results,
            localScanned = listOf("Domates", "Soğan", "Tavuk", "Fesleğen"),
            onDismiss = {},
            onTakePhoto = {},
            onChooseGallery = {},
            onRemoveIngredient = {},
            onAccept = {}
        )
    }
}
