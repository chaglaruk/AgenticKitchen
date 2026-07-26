package com.agentickitchen.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.L

/**
 * İlk açılışta veya API key boşken gösterilecek basit onboarding dialog.
 * Kullanıcı ya key'i yapıştırır ya da "Şimdilik Geç" der (mock modda çalışır).
 */
@Composable
fun ApiKeyOnboardingDialog(
    aiProvider: String,
    onSave: (String) -> Unit,
    onSkip: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var apiKey by remember { mutableStateOf("") }

    val isGemini = aiProvider == "GEMINI"
    val providerName = when (aiProvider) {
        "HUGGINGFACE" -> if (L.isTr) "Hugging Face" else "Hugging Face"
        else -> if (L.isTr) "Gemini" else "Gemini"
    }
    val description = when (aiProvider) {
        "HUGGINGFACE" -> if (L.isTr)
            "Gerçek tarif üretimi, malzeme tarama ve şef asistanı için bir Hugging Face token gereklidir.\n\nÜcretsiz olarak huggingface.co adresinden alabilirsiniz."
        else
            "A Hugging Face token is required for real recipe generation, ingredient scanning, and chef assistant.\n\nGet one at huggingface.co."
        else -> if (L.isTr)
            "Gerçek tarif üretimi, malzeme tarama ve şef asistanı için bir Gemini API Key gereklidir.\n\nÜcretsiz olarak aistudio.google.com adresinden alabilirsiniz."
        else
            "A Gemini API Key is required for real recipe generation, ingredient scanning, and chef assistant.\n\nGet one for free at aistudio.google.com."
    }
    val linkUrl = if (aiProvider == "HUGGINGFACE") "https://huggingface.co/settings/tokens" else "https://aistudio.google.com/apikey"
    val placeholderText = if (aiProvider == "HUGGINGFACE") "hf_..." else "AIza..."

    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = colors.surface,
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    if (L.isTr) "Yapay Zeka Bağlantısı" else "AI Connection",
                    color = colors.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    description,
                    color = colors.onSurfaceSub,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(20.dp))

                // Link butonu
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.primary)
                ) {
                    Text(
                        if (L.isTr) "🔗 API Key Al (Ücretsiz)" else "🔗 Get API Key (Free)",
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Key input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(providerName + " API Key", color = colors.onSurfaceSub) },
                    placeholder = { Text(placeholderText, color = colors.onSurfaceSub.copy(alpha = 0.4f)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { apiKey = it }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Yapıştır", tint = colors.primaryLight)
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                // Kaydet butonu
                Button(
                    onClick = { if (apiKey.isNotBlank()) onSave(apiKey.trim()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (apiKey.isNotBlank()) colors.primary else colors.divider
                    ),
                    enabled = apiKey.isNotBlank()
                ) {
                    Text(
                        if (L.isTr) "Kaydet ve Başla" else "Save & Start",
                        color = if (apiKey.isNotBlank()) colors.onPrimary else colors.onSurfaceSub,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Geç butonu
                TextButton(onClick = onSkip) {
                    Text(
                        if (L.isTr) "Şimdilik Geçersiz Modda Kullan" else "Skip (Offline Mode)",
                        color = colors.onSurfaceSub,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
