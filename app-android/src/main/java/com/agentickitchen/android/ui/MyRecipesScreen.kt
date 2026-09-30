package com.agentickitchen.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentickitchen.android.L
import com.agentickitchen.shared.ai.ImportedRecipeIngredient
import com.agentickitchen.shared.recipes.SavedRecipe
import com.agentickitchen.shared.recipes.SavedRecipeSource
import java.util.Locale

@Composable
fun MyRecipesScreen(
    recipes: List<SavedRecipe>,
    preparedRecipeName: String?,
    preparedRecipeAlreadySaved: Boolean,
    onSavePreparedRecipe: () -> Unit,
    onDeleteRecipe: (String) -> Unit,
    onCookRecipe: (String) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selected = recipes.firstOrNull { it.id == selectedId }
    LaunchedEffect(recipes, selectedId) {
        if (selectedId != null && selected == null) selectedId = null
    }

    if (selected != null) {
        SavedRecipeDetail(
            saved = selected,
            onBack = { selectedId = null },
            onDelete = {
                onDeleteRecipe(selected.id)
                selectedId = null
            },
            onCook = { onCookRecipe(selected.id) }
        )
        return
    }

    val colors = LocalAppColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EditorialBrandLockup()
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (L.isTr) "Tariflerim" else "My Recipes",
                color = colors.onSurface,
                style = MaterialTheme.typography.h1
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (L.isTr) {
                    "Kaydettiğin tarifleri burada açabilir, silebilir ve güncel mutfak durumuna göre yeniden hazırlayabilirsin."
                } else {
                    "Open, delete, or re-prepare saved recipes against your current kitchen state."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
            if (preparedRecipeName != null) {
                Spacer(Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = colors.surface,
                    elevation = 0.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = preparedRecipeName,
                            color = colors.onSurface,
                            style = MaterialTheme.typography.h6,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (preparedRecipeAlreadySaved) {
                                if (L.isTr) "Bu hazırlanmış tarif zaten Tariflerim'de. Güncel planla yeniden kaydedebilirsin." else "This prepared recipe is already in My Recipes. Save again to update it with the current plan."
                            } else {
                                if (L.isTr) "Şu an hazırlanmış tarifi kalıcı olarak sakla." else "Keep the currently prepared recipe on this device."
                            },
                            color = colors.onSurfaceSub,
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSavePreparedRecipe,
                            colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                if (preparedRecipeAlreadySaved) {
                                    if (L.isTr) "Kaydı güncelle" else "Update saved recipe"
                                } else {
                                    if (L.isTr) "Tarifi kaydet" else "Save recipe"
                                },
                                color = colors.onPrimary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Divider(color = colors.divider)
        }

        if (recipes.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp)) {
                    Text(
                        if (L.isTr) "Henüz kaydedilmiş tarif yok" else "No saved recipes yet",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.h5
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (L.isTr) "Bir tarifi hazırladıktan sonra buradan kaydedebilirsin." else "Prepare a recipe, then save it here for quick reuse.",
                        color = colors.onSurfaceSub,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        } else {
            items(recipes, key = SavedRecipe::id) { saved ->
                SavedRecipeCard(saved = saved, onOpen = { selectedId = saved.id })
            }
        }
    }
}

@Composable
private fun SavedRecipeCard(saved: SavedRecipe, onOpen: () -> Unit) {
    val colors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = colors.surface,
        elevation = 0.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = saved.recipe.name,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sourceLabel(saved.source),
                    color = colors.primary,
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildString {
                    append(saved.recipe.servings?.let { if (L.isTr) "$it porsiyon" else "$it servings" } ?: if (L.isTr) "Porsiyon belirsiz" else "Servings unknown")
                    append(" · ")
                    append(if (L.isTr) "${saved.recipe.ingredients.size} malzeme" else "${saved.recipe.ingredients.size} ingredients")
                    if (saved.cookCount > 0) {
                        append(" · ")
                        append(if (L.isTr) "${saved.cookCount} kez kullanıldı" else "used ${saved.cookCount} times")
                    }
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body2
            )
            saved.recipe.sourceLabel?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun SavedRecipeDetail(
    saved: SavedRecipe,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onCook: () -> Unit
) {
    var confirmDelete by remember(saved.id) { mutableStateOf(false) }
    val colors = LocalAppColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(if (L.isTr) "← Tariflerim" else "← My Recipes", color = colors.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(saved.recipe.name, color = colors.onSurface, style = MaterialTheme.typography.h1)
            Spacer(Modifier.height(8.dp))
            Text(
                sourceLabel(saved.source) + saved.recipe.servings?.let { " · " + if (L.isTr) "$it porsiyon" else "$it servings" }.orEmpty(),
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
            saved.recipe.sourceLabel?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onCook,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Bu tarifi hazırla" else "Prepare this recipe", color = colors.onPrimary)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (L.isTr) "Tarifi sil" else "Delete recipe", color = colors.onSurface)
            }
            Spacer(Modifier.height(18.dp))
            Divider(color = colors.divider)
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Malzemeler" else "Ingredients", color = colors.onSurface, style = MaterialTheme.typography.h5)
        }

        items(saved.recipe.ingredients) { ingredient ->
            Text(ingredientLabel(ingredient), color = colors.onSurface, style = MaterialTheme.typography.body1)
        }

        item {
            Spacer(Modifier.height(8.dp))
            Divider(color = colors.divider)
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Adımlar" else "Instructions", color = colors.onSurface, style = MaterialTheme.typography.h5)
        }

        items(saved.recipe.instructions.size) { index ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}.", color = colors.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.padding(horizontal = 5.dp))
                Text(saved.recipe.instructions[index], color = colors.onSurface, style = MaterialTheme.typography.body1)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (L.isTr) "Tarifi sil?" else "Delete recipe?") },
            text = { Text(if (L.isTr) "Bu kayıt bu telefondan kalıcı olarak silinecek." else "This saved recipe will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(if (L.isTr) "Sil" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(if (L.isTr) "İptal" else "Cancel")
                }
            }
        )
    }
}

private fun sourceLabel(source: SavedRecipeSource): String = when (source) {
    SavedRecipeSource.IMPORTED -> if (L.isTr) "İçe aktarıldı" else "Imported"
    SavedRecipeSource.GENERATED_AI -> if (L.isTr) "AI tarifi" else "AI recipe"
    SavedRecipeSource.GENERATED_OFFLINE -> if (L.isTr) "Çevrimdışı" else "Offline"
    SavedRecipeSource.COOKED -> if (L.isTr) "Pişirildi" else "Cooked"
    SavedRecipeSource.MANUAL -> if (L.isTr) "Manuel" else "Manual"
}

private fun ingredientLabel(ingredient: ImportedRecipeIngredient): String {
    val quantity = ingredient.quantity?.let(::formatQuantity)
    return listOfNotNull(quantity, ingredient.unit, ingredient.displayName)
        .filter(String::isNotBlank)
        .joinToString(" ")
}

private fun formatQuantity(value: Double): String {
    if (value % 1.0 == 0.0) return value.toLong().toString()
    return String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}
