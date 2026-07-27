package com.agentickitchen.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agentickitchen.android.L

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
    var contentVisible by remember { mutableStateOf(false) }
    val isHuggingFace = aiProvider == "HUGGINGFACE"
    val providerName = if (isHuggingFace) "Hugging Face" else "Gemini"
    val title = if (isHuggingFace) {
        if (L.isTr) "Hugging Face tokenı" else "Hugging Face token"
    } else {
        if (L.isTr) "Gemini API anahtarı" else "Gemini API key"
    }
    val description = if (isHuggingFace) {
        if (L.isTr) {
            "Tarif önerileri, malzeme analizi ve mutfak asistanı için bir Hugging Face tokenı gerekiyor."
        } else {
            "A Hugging Face token enables recipe ideas, ingredient analysis, and the kitchen assistant."
        }
    } else {
        if (L.isTr) {
            "Tarif önerileri, malzeme analizi ve mutfak asistanı için bir Gemini API anahtarı gerekiyor."
        } else {
            "A Gemini API key enables recipe ideas, ingredient analysis, and the kitchen assistant."
        }
    }
    val linkUrl = if (isHuggingFace) "https://huggingface.co/settings/tokens" else "https://aistudio.google.com/apikey"
    val placeholderText = if (isHuggingFace) "hf_..." else "AIza..."

    LaunchedEffect(Unit) { contentVisible = true }

    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, colors.divider, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = colors.surface,
            elevation = 0.dp
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 }
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.Start) {
                    Text(
                        if (L.isTr) "YAPAY ZEKÂ BAĞLANTISI" else "AI CONNECTION",
                        color = colors.primary,
                        style = MaterialTheme.typography.caption
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(title, color = colors.onSurface, style = MaterialTheme.typography.h2)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        description,
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(Modifier.height(20.dp))
                    Divider(color = colors.divider, thickness = 1.dp)
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = if (L.isTr) "$providerName anahtarı" else "$providerName credential" },
                        label = { Text(title) },
                        placeholder = { Text(placeholderText, color = colors.onSurfaceSub.copy(alpha = .55f)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { clipboardManager.getText()?.text?.let { apiKey = it } },
                                modifier = Modifier.semantics { contentDescription = if (L.isTr) "Panodan yapıştır" else "Paste from clipboard" }
                            ) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = colors.primary)
                            }
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = colors.onSurface,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            cursorColor = colors.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)))
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (L.isTr) "$providerName anahtarı alma sayfasını aç" else "Open $providerName credential help"
                        }
                    ) {
                        Text(
                            if (L.isTr) "$providerName anahtarı al" else "Get a $providerName credential",
                            color = colors.primary,
                            textAlign = TextAlign.Start
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (apiKey.isNotBlank()) onSave(apiKey.trim()) },
                        enabled = apiKey.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics { contentDescription = if (L.isTr) "Kaydet ve devam et" else "Save and continue" },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colors.primary,
                            disabledBackgroundColor = colors.divider
                        ),
                        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text(if (L.isTr) "Kaydet ve devam et" else "Save and continue", color = colors.onPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.semantics { contentDescription = if (L.isTr) "Şimdilik geç" else "Skip for now" }
                    ) {
                        Text(if (L.isTr) "Şimdilik geç" else "Skip for now", color = colors.onSurfaceSub)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "tr")
@Composable
private fun GeminiOnboardingPreview() {
    AgenticTheme("editorial") {
        ApiKeyOnboardingDialog(aiProvider = "GEMINI", onSave = {}, onSkip = {})
    }
}

@Preview(showBackground = true, locale = "en")
@Composable
private fun HuggingFaceOnboardingPreview() {
    AgenticTheme("editorial") {
        ApiKeyOnboardingDialog(aiProvider = "HUGGINGFACE", onSave = {}, onSkip = {})
    }
}
