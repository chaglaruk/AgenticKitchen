package com.agentickitchen.android.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext

@Composable
fun CameraModal(
    scannedIngredients: List<String>?,
    onDismiss: () -> Unit, 
    onAcceptScan: (List<String>) -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    var scanState by remember { mutableStateOf(0) } // 0: Idle, 1: Scanning, 2: Done
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scanState = 1 // start scanning
            onImageCaptured(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                scanState = 1 // start scanning
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                    android.graphics.ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onImageCaptured(bitmap)
            } catch (e: Exception) {
                scanState = 0
                Toast.makeText(context, "Görsel yüklenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Kamera başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Kamera izni reddedildi.", Toast.LENGTH_SHORT).show()
        }
    }

    var localScanned by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(scannedIngredients) {
        if (scannedIngredients != null && scanState == 1) {
            if (scannedIngredients.firstOrNull() == "__ERROR__") {
                onDismiss()
            } else {
                localScanned = scannedIngredients
                scanState = 2
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = colors.background,
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = colors.onSurfaceSub)
                    }
                }
                
                if (scanState == 0) {
                    // Idle state
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(64.dp), tint = colors.primaryLight)
                    Spacer(Modifier.height(16.dp))
                    Text("Görsel Analizi (Vision Agent)", color = colors.onSurface, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kameranızı kullanarak tezgâhtaki malzemeleri çekin veya galeriden fotoğraf yükleyin. Yapay zeka sizin için onları tanısın.",
                        color = colors.onSurfaceSub,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(32.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { 
                                try {
                                    val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = colors.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Fotoğraf Çek", color = colors.onPrimary, style = MaterialTheme.typography.button)
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                try {
                                    galleryLauncher.launch("image/*") 
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.background),
                            border = BorderStroke(1.dp, colors.primary)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Galeriden Yükle", color = colors.primary, style = MaterialTheme.typography.button)
                        }
                    }
                } else if (scanState == 1) {
                    // Scanning state
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulse by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse)
                    )
                    
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 4.dp, modifier = Modifier.fillMaxSize())
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size((40 * pulse).dp), tint = colors.primaryLight)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Görsel İşleniyor...", color = colors.onSurface, style = MaterialTheme.typography.h6)
                    Text("Yapay zeka malzemeleri tanımlıyor", color = colors.onSurfaceSub, style = MaterialTheme.typography.body1)
                    Spacer(Modifier.height(32.dp))
                } else {
                    // Done state
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = colors.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Malzemeler Tespit Edildi", color = colors.onSurface, style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(16.dp))
                    
                    // Found ingredients box
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface).padding(16.dp)) {
                        Column {
                            Text("Görselde Bulunanlar:", color = colors.onSurfaceSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            // Chips
                            FlowRow(chips = localScanned, colors = colors, onRemove = { toRemove -> localScanned = localScanned.filter { it != toRemove } })
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colors.background)
                        ) {
                            Text("İptal", color = colors.onSurfaceSub)
                        }
                        Button(
                            onClick = { onAcceptScan(localScanned) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary)
                        ) {
                            Text("Listeye Ekle", color = colors.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
