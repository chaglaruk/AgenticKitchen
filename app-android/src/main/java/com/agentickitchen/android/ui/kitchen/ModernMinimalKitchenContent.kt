package com.agentickitchen.android.ui.kitchen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import com.agentickitchen.android.ui.EditorialBrandMark
import com.agentickitchen.android.ui.LocalAppColors

@Composable
fun ModernMinimalKitchenContent(
    state: KitchenUiState,
    actions: KitchenUiActions,
    input: String,
    onInputChange: (String) -> Unit,
    filteredSuggestions: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val recipeAction = { if (state.chips.isNotEmpty()) actions.onStart() else actions.onOpenCookWithPantry() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EditorialBrandMark(size = 24.dp)
            Spacer(Modifier.width(8.dp))
            Text("AgenticKitchen", style = MaterialTheme.typography.h6, color = colors.onBackground, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = actions.onEditSetup) {
                Icon(Icons.Filled.Tune, contentDescription = if (L.isTr) "Kurulum" else "Setup", tint = colors.onSurfaceSub, modifier = Modifier.size(21.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(if (L.isTr) "Bugün ne pişiriyoruz?" else "What are we cooking?", style = MaterialTheme.typography.h1, color = colors.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(if (L.isTr) "Güzel yemekler elindekilerle başlar." else "Good food starts with what you have.", style = MaterialTheme.typography.body1, color = colors.onSurfaceSub)
        Spacer(Modifier.height(16.dp))

        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Malzeme ekle..." else "Add an ingredient..."
        )

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitchenActionTile(if (L.isTr) "Kiler" else "Pantry", if (L.isTr) "Stoku yönet" else "Manage stock", Icons.Filled.Kitchen, actions.onOpenPantry, Modifier.weight(1f), compact = true)
            KitchenActionTile(if (L.isTr) "Fotoğraf" else "Photo", if (L.isTr) "Malzeme ekle" else "Add ingredients", Icons.Filled.PhotoCamera, actions.onOpenIngredientCamera, Modifier.weight(1f), compact = true)
            KitchenActionTile(if (L.isTr) "Hızlı pişir" else "Quick cook", if (L.isTr) "Kileri kullan" else "Use pantry", Icons.Filled.AutoAwesome, actions.onOpenCookWithPantry, Modifier.weight(1f), iconColor = colors.ai, compact = true)
            KitchenActionTile(if (L.isTr) "Tarifler" else "Recipes", if (L.isTr) "Fikir oluştur" else "Generate ideas", Icons.Filled.RestaurantMenu, recipeAction, Modifier.weight(1f), compact = true)
        }

        if (state.chips.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (L.isTr) "Seçilen malzemeler" else "Selected ingredients", color = colors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${state.chips.size}", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            SelectedChipsCollection(state.chips, actions.onRemoveChip)
        }

        Spacer(Modifier.height(18.dp))
        TruthfulPantryPreview(state, actions, radius = 18.dp)
        Spacer(Modifier.height(12.dp))
        TruthfulKitchenInsight(state, recipeAction, background = colors.surface2, accent = colors.ai)
        if (state.shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            TruthfulShoppingListSection(state, actions)
        }
        Spacer(Modifier.height(14.dp))
        SecondaryKitchenToolbar(
            chipsCount = state.chips.size,
            onOpenLibrary = actions.onOpenIngredientLibrary,
            onOpenKitchenScan = actions.onOpenKitchenScan,
            onOpenShopping = actions.onOpenShoppingImport,
            onOpenRecipeImport = actions.onOpenRecipeImport,
            onOpenSetup = actions.onEditSetup,
            onClearAll = actions.onClearAll
        )
        Spacer(Modifier.height(18.dp))
        KitchenPrimaryAction(state, actions)
        Spacer(Modifier.height(16.dp))
    }
}
