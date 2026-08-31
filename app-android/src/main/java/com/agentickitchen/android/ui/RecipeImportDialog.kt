package com.agentickitchen.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.agentickitchen.android.L
import com.agentickitchen.android.RecipeImportState
import com.agentickitchen.shared.ai.ImportedRecipe
import com.agentickitchen.shared.inventory.PantryStockItem
import com.agentickitchen.shared.inventory.LocalIngredientResolver
import com.agentickitchen.shared.inventory.RecipeImportAvailability
import com.agentickitchen.shared.inventory.RecipeImportDraftPolicy
import com.agentickitchen.shared.inventory.RecipeImportPantryPlanner
import java.math.BigDecimal

@Composable
fun RecipeImportDialog(
    state: RecipeImportState,
    inventory: List<PantryStockItem>,
    onDismiss: () -> Unit,
    onImportUrl: (String) -> Unit,
    onImportText: (String) -> Unit,
    onImportPhoto: (Bitmap) -> Unit,
    onPrepare: (ImportedRecipe) -> Unit,
    onConfigureGemini: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var pastedText by remember { mutableStateOf("") }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let(onImportPhoto)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
    }
    val importing = state is RecipeImportState.Loading

    Dialog(
        onDismissRequest = { if (!importing) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !importing,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            backgroundColor = colors.surface,
            elevation = 0.dp,
            border = BorderStroke(1.dp, colors.divider),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    if (L.isTr) "Tarif içe aktar" else "Import a recipe",
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h5
                )
                Text(
                    if (L.isTr) "Bağlantı, metin, ekran görüntüsü veya Android Paylaş menüsünden tarif al." else "Bring in a recipe from a link, text, screenshot, or Android Share menu.",
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2
                )
                Spacer(Modifier.height(16.dp))

                when (state) {
                    RecipeImportState.Idle, is RecipeImportState.Error -> {
                        if (state is RecipeImportState.Error) {
                            Text(state.message, color = MaterialTheme.colors.error, style = MaterialTheme.typography.body2)
                            Spacer(Modifier.height(10.dp))
                        }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text(if (L.isTr) "Tarif bağlantısı" else "Recipe URL") },
                            placeholder = { Text("https://…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onImportUrl(url) },
                            enabled = url.trim().startsWith("http://") || url.trim().startsWith("https://"),
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text(if (L.isTr) "Bağlantıdan oku" else "Read from URL", color = colors.onPrimary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Divider(color = colors.divider)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            label = { Text(if (L.isTr) "Tarif metni" else "Recipe text") },
                            placeholder = { Text(if (L.isTr) "Başlık, malzemeler ve yapılışı yapıştır…" else "Paste title, ingredients, and instructions…") },
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onImportText(pastedText) },
                            enabled = pastedText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, colors.primary),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "Metni çözümle" else "Parse text", color = colors.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    camera.launch(null)
                                } else {
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, colors.divider),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(if (L.isTr) "Tarif fotoğrafını tara" else "Scan recipe photo", color = colors.primary)
                        }
                        if (state is RecipeImportState.Error) {
                            TextButton(onClick = onConfigureGemini, modifier = Modifier.align(Alignment.End)) {
                                Text(if (L.isTr) "AI ayarları" else "AI settings", color = colors.primary)
                            }
                        }
                    }

                    is RecipeImportState.Loading -> {
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = colors.primary)
                            Text(
                                when (state.source) {
                                    "prepare" -> if (L.isTr) "Tarifi güvenli pişirme planına dönüştürüyorum…" else "Turning the recipe into a validated cooking plan…"
                                    "url" -> if (L.isTr) "Tarif sayfasını okuyorum…" else "Reading the recipe page…"
                                    "photo" -> if (L.isTr) "Tarif fotoğrafını okuyorum…" else "Reading the recipe photo…"
                                    else -> if (L.isTr) "Tarifi okuyorum…" else "Reading the recipe…"
                                },
                                color = colors.onSurface
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    is RecipeImportState.Review -> RecipeImportReview(
                        state = state,
                        inventory = inventory,
                        onPrepare = onPrepare
                    )
                }

                if (!importing) {
                    Spacer(Modifier.height(14.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text(if (L.isTr) "Kapat" else "Close", color = colors.onSurfaceSub)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeImportReview(
    state: RecipeImportState.Review,
    inventory: List<PantryStockItem>,
    onPrepare: (ImportedRecipe) -> Unit
) {
    val colors = LocalAppColors.current
    val source = state.response.recipe
    var name by remember(source) { mutableStateOf(source.name) }
    var servingsText by remember(source) { mutableStateOf(source.servings?.toString().orEmpty()) }
    var ingredients by remember(source) { mutableStateOf(source.ingredients) }
    var instructions by remember(source) { mutableStateOf(source.instructions) }

    val draft = source.copy(
        name = name.trim(),
        servings = servingsText.toIntOrNull()?.takeIf { it > 0 },
        ingredients = ingredients,
        instructions = instructions
    )
    val pantry = RecipeImportPantryPlanner.compare(draft, inventory)
    val issues = RecipeImportDraftPolicy.issues(draft)

    Text(
        if (L.isTr) "Önizleme ve kontrol" else "Preview and review",
        color = colors.primary,
        style = MaterialTheme.typography.overline
    )
    source.sourceLabel?.takeIf(String::isNotBlank)?.let {
        Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
    }
    state.response.uncertainty?.let {
        Text(it, color = colors.accent, style = MaterialTheme.typography.caption)
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(if (L.isTr) "Tarif adı" else "Recipe name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = servingsText,
        onValueChange = { servingsText = it.filter(Char::isDigit).take(3) },
        label = { Text(if (L.isTr) "Porsiyon" else "Servings") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(14.dp))
    Divider(color = colors.divider)
    Spacer(Modifier.height(12.dp))
    Text(if (L.isTr) "Malzemeler" else "Ingredients", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    ingredients.forEachIndexed { index, ingredient ->
        val match = pantry.matches.getOrNull(index)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ingredient.displayName,
            onValueChange = { value ->
                ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(displayName = value, canonicalIngredientId = LocalIngredientResolver.resolveCanonicalId(value)) else item }
            },
            label = { Text(if (L.isTr) "Malzeme ${index + 1}" else "Ingredient ${index + 1}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ingredient.quantity?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
                onValueChange = { raw ->
                    val parsed = raw.replace(',', '.').toDoubleOrNull()
                    ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(quantity = parsed) else item }
                },
                label = { Text(if (L.isTr) "Miktar" else "Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ingredient.unit.orEmpty(),
                onValueChange = { value ->
                    ingredients = ingredients.mapIndexed { i, item -> if (i == index) item.copy(unit = value.trim().takeIf(String::isNotEmpty)) else item }
                },
                label = { Text(if (L.isTr) "Birim" else "Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            importAvailabilityLabel(match?.availability ?: RecipeImportAvailability.NEEDS_REVIEW),
            color = when (match?.availability) {
                RecipeImportAvailability.AVAILABLE -> colors.success
                RecipeImportAvailability.PARTIAL -> colors.accent
                RecipeImportAvailability.MISSING -> MaterialTheme.colors.error
                else -> colors.onSurfaceSub
            },
            style = MaterialTheme.typography.caption
        )
    }
    Spacer(Modifier.height(14.dp))
    Divider(color = colors.divider)
    Spacer(Modifier.height(12.dp))
    Text(if (L.isTr) "Adımlar" else "Instructions", color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    instructions.forEachIndexed { index, instruction ->
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = instruction,
            onValueChange = { value ->
                instructions = instructions.mapIndexed { i, item -> if (i == index) value else item }
            },
            label = { Text(if (L.isTr) "Adım ${index + 1}" else "Step ${index + 1}") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(14.dp))
    Text(
        if (L.isTr) {
            "Stok: ${pantry.availableCount} hazır · ${pantry.partialCount} kısmi · ${pantry.missingCount} eksik · ${pantry.needsReviewCount} kontrol"
        } else {
            "Pantry: ${pantry.availableCount} ready · ${pantry.partialCount} partial · ${pantry.missingCount} missing · ${pantry.needsReviewCount} review"
        },
        color = colors.onSurfaceSub,
        style = MaterialTheme.typography.body2
    )
    if (issues.isNotEmpty()) {
        Text(
            if (L.isTr) "Eksik veya anlaşılmayan alanları düzeltmeden pişirme planı oluşturulmaz." else "Complete unclear or missing fields before creating a cooking plan.",
            color = colors.accent,
            style = MaterialTheme.typography.caption
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onPrepare(draft) },
        enabled = issues.isEmpty(),
        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
    ) {
        Text(if (L.isTr) "Pişirme planını hazırla" else "Prepare cooking plan", color = colors.onPrimary)
    }
}

private fun importAvailabilityLabel(value: RecipeImportAvailability): String = when (value) {
    RecipeImportAvailability.AVAILABLE -> if (L.isTr) "Stokta yeterli" else "Enough in pantry"
    RecipeImportAvailability.PARTIAL -> if (L.isTr) "Stokta kısmen var" else "Partially available"
    RecipeImportAvailability.MISSING -> if (L.isTr) "Eksik · alışveriş/değişiklik gerekebilir" else "Missing · shopping/substitution may be needed"
    RecipeImportAvailability.NEEDS_REVIEW -> if (L.isTr) "Miktar veya birim kontrol edilmeli" else "Amount or unit needs review"
}
