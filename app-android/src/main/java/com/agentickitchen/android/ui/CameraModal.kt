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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.L

private enum class CameraModalPhase { Consent, Idle, Scanning, Results, Error }

@Composable
fun CameraModal(
    scannedIngredients: List<String>?,
    onDismiss: () -> Unit,
    onAcceptScan: (List<String>) -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    var scanState by remember { mutableStateOf(CameraModalPhase.Consent) }
    var localScanned by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current

    fun showSafeError(messageTr: String, messageEn: String) {
        Toast.makeText(context, if (L.isTr) messageTr else messageEn, Toast.LENGTH_SHORT).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scanState = CameraModalPhase.Scanning
            onImageCaptured(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = loadBitmap(context, uri)
                scanState = CameraModalPhase.Scanning
                onImageCaptured(bitmap)
            } catch (_: Exception) {
                scanState = CameraModalPhase.Error
                showSafeError("Görsel güvenli biçimde yüklenemedi.", "The image could not be loaded safely.")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure {
                    scanState = CameraModalPhase.Error
                    showSafeError("Kamera başlatılamadı.", "The camera could not be opened.")
                }
        } else {
            showSafeError("Kamera izni reddedildi.", "Camera permission was denied.")
        }
    }

    LaunchedEffect(scannedIngredients) {
        if (scannedIngredients != null && scanState == CameraModalPhase.Scanning) {
            if (scannedIngredients.firstOrNull() == "__ERROR__") {
                scanState = CameraModalPhase.Error
            } else {
                localScanned = scannedIngredients
                scanState = CameraModalPhase.Results
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CameraModalContent(
            phase = scanState,
            localScanned = localScanned,
            onDismiss = onDismiss,
            onConsent = { scanState = CameraModalPhase.Idle },
            onTakePhoto = {
                if (scanState != CameraModalPhase.Idle && scanState != CameraModalPhase.Error) return@CameraModalContent
                runCatching {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }.onFailure {
                    scanState = CameraModalPhase.Error
                    showSafeError("Kamera başlatılamadı.", "The camera could not be opened.")
                }
            },
            onChooseGallery = {
                if (scanState != CameraModalPhase.Idle && scanState != CameraModalPhase.Error) return@CameraModalContent
                runCatching { galleryLauncher.launch("image/*") }
                    .onFailure {
                        scanState = CameraModalPhase.Error
                        showSafeError("Galeri başlatılamadı.", "The gallery could not be opened.")
                    }
            },
            onRemoveIngredient = { ingredient ->
                localScanned = localScanned.filterNot { it == ingredient }
            },
            onAccept = {
                if (localScanned.isNotEmpty()) onAcceptScan(localScanned)
            }
        )
    }
}

@Suppress("DEPRECATION")
private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
        android.graphics.ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

@Composable
private fun CameraModalContent(
    phase: CameraModalPhase,
    localScanned: List<String>,
    onDismiss: () -> Unit,
    onConsent: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onRemoveIngredient: (String) -> Unit,
    onAccept: () -> Unit
) {
    val colors = LocalAppColors.current
    val maxDialogHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f

    Card(
        modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = maxDialogHeight).wrapContentHeight(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = colors.surface,
        elevation = 0.dp,
        border = BorderStroke(1.dp, colors.divider)
    ) {
        Column(modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)) {
            CameraModalHeader(onDismiss)
            Divider(color = colors.divider)
            when (phase) {
                CameraModalPhase.Consent -> CameraConsentContent(onConsent, onDismiss)
                CameraModalPhase.Idle -> CameraIdleContent(onTakePhoto, onChooseGallery)
                CameraModalPhase.Scanning -> CameraScanningContent()
                CameraModalPhase.Results -> CameraResultsContent(
                    localScanned = localScanned,
                    onDismiss = onDismiss,
                    onRemoveIngredient = onRemoveIngredient,
                    onAccept = onAccept
                )
                CameraModalPhase.Error -> CameraErrorContent(onTakePhoto, onChooseGallery, onDismiss)
            }
        }
    }
}

@Composable
private fun CameraModalHeader(onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 10.dp),
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
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = if (L.isTr) "Kapat" else "Close"
            }
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
        }
    }
}

@Composable
private fun CameraConsentContent(onConsent: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(
            text = if (L.isTr) "Fotoğrafını göndermeden önce" else "Before sending your photo",
            color = colors.onSurface,
            style = MaterialTheme.typography.h2
        )
        Spacer(Modifier.height(12.dp))
        ConsentLine(if (L.isTr) "Seçtiğin fotoğraf, analiz için etkin AI sağlayıcısına bir kez gönderilir." else "The selected photo is sent once to the active AI provider for analysis.")
        ConsentLine(if (L.isTr) "Uygulama fotoğrafı dosyaya veya veritabanına kaydetmez." else "The app does not save the photo to a file or database.")
        ConsentLine(if (L.isTr) "AI yanlış malzeme tanıyabilir; sonuçları eklemeden önce sen kontrol etmelisin." else "AI can identify ingredients incorrectly; you must review the results before adding them.")
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
            }
            TextButton(
                onClick = onConsent,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(colors.primary, RoundedCornerShape(12.dp))
                    .semantics {
                        contentDescription = if (L.isTr) "Anladım, devam et" else "I understand, continue"
                    }
            ) {
                Text(if (L.isTr) "Anladım" else "I understand", color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun ConsentLine(text: String) {
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("•", color = colors.primary, modifier = Modifier.width(20.dp))
        Text(text, color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
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
                "Bir fotoğraf çek veya galerinden seç. Bulunan malzemeleri eklemeden önce gözden geçireceksin."
            } else {
                "Take a photo or choose one from your gallery. You will review detected ingredients before adding them."
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(18.dp))
        Text(if (L.isTr) "Fotoğraf inceleniyor" else "Reviewing the photo", color = colors.onSurface, style = MaterialTheme.typography.h2)
        Spacer(Modifier.height(6.dp))
        Text(if (L.isTr) "Malzemeler belirleniyor." else "Identifying the ingredients.", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
    }
}

@Composable
private fun CameraErrorContent(onTakePhoto: () -> Unit, onChooseGallery: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(if (L.isTr) "Fotoğraf incelenemedi" else "Could not review the photo", color = colors.onSurface, style = MaterialTheme.typography.h2)
        Spacer(Modifier.height(8.dp))
        Text(if (L.isTr) "Başka bir fotoğrafla tekrar deneyebilirsin." else "You can try again with another photo.", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
        Spacer(Modifier.height(20.dp))
        EditorialCaptureAction(Icons.Filled.PhotoCamera, if (L.isTr) "Fotoğraf çek" else "Take a photo", if (L.isTr) "Yeni fotoğrafla dene." else "Try a new photo.", onTakePhoto)
        Divider(color = colors.divider)
        EditorialCaptureAction(Icons.Filled.Image, if (L.isTr) "Galeriden seç" else "Choose from gallery", if (L.isTr) "Başka bir görsel seç." else "Choose another image.", onChooseGallery)
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
        }
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
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
        Text(if (L.isTr) "Bulunan malzemeler" else "Detected ingredients", color = colors.onSurface, style = MaterialTheme.typography.h2)
        Spacer(Modifier.height(8.dp))
        Text(
            if (L.isTr) "Yanlış veya istemediğin sonuçları çıkar; yalnızca doğruladıklarını ekle." else "Remove incorrect or unwanted results and add only what you verified.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(scrollState)) {
            if (localScanned.isEmpty()) {
                Text(if (L.isTr) "Doğrulanacak malzeme kalmadı." else "No ingredients remain to verify.", color = colors.onSurfaceSub)
            } else {
                localScanned.forEachIndexed { index, ingredient ->
                    IngredientResultRow(index, ingredient) { onRemoveIngredient(ingredient) }
                    Divider(color = colors.divider)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(if (L.isTr) "İptal" else "Cancel", color = colors.onSurfaceSub)
            }
            TextButton(
                onClick = onAccept,
                enabled = localScanned.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(colors.primary, RoundedCornerShape(12.dp))
            ) {
                Text(if (L.isTr) "Doğruladıklarımı ekle" else "Add verified items", color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun IngredientResultRow(index: Int, ingredient: String, onRemove: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("%02d".format(index + 1), color = colors.success, style = MaterialTheme.typography.h6, modifier = Modifier.width(34.dp))
        Text(ingredient, color = colors.onSurface, style = MaterialTheme.typography.body1, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = if (L.isTr) "$ingredient malzemesini kaldır" else "Remove $ingredient"
            }
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub, modifier = Modifier.size(18.dp))
        }
    }
}
