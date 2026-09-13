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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import com.agentickitchen.android.ui.EditorialBrandMark
import com.agentickitchen.android.ui.LocalAppColors

@Composable
fun WarmEditorialKitchenContent(
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
        modifier = modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EditorialBrandMark(size = 23.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("AgenticKitchen", style = MaterialTheme.typography.h6, color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text(if (L.isTr) "MUTFAK DEFTERİ" else "KITCHEN JOURNAL", color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }
            IconButton(onClick = actions.onEditSetup) {
                Icon(Icons.Filled.Tune, contentDescription = if (L.isTr) "Kurulum" else "Setup", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(if (L.isTr) "Bugün ne pişirmek istersiniz?" else "What would you like to cook today?", style = MaterialTheme.typography.h1, color = colors.onBackground)
        Spacer(Modifier.height(5.dp))
        Text(if (L.isTr) "Kilerindeki gerçek malzemelerden başlayalım." else "Begin with the real ingredients in your kitchen.", style = MaterialTheme.typography.body1, color = colors.onSurfaceSub)
        Spacer(Modifier.height(16.dp))

        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Malzeme ekle..." else "Add ingredients..."
        )

        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitchenActionTile(if (L.isTr) "Kiler" else "Pantry", if (L.isTr) "Stoku gör" else "See your stock", Icons.Filled.Kitchen, actions.onOpenPantry, Modifier.weight(1f), background = Color(0xFFEEF6E9), iconColor = colors.primary, radius = 18.dp)
            KitchenActionTile(if (L.isTr) "Fotoğraf" else "Photo", if (L.isTr) "Malzeme ekle" else "Add ingredients", Icons.Filled.PhotoCamera, actions.onOpenIngredientCamera, Modifier.weight(1f), background = Color(0xFFFFF1EC), iconColor = colors.accent, radius = 18.dp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitchenActionTile(if (L.isTr) "Hızlı pişir" else "Quick cook", if (L.isTr) "Kileri kullan" else "Use pantry", Icons.Filled.AutoAwesome, actions.onOpenCookWithPantry, Modifier.weight(1f), background = colors.aiBg, iconColor = colors.ai, radius = 18.dp)
            KitchenActionTile(if (L.isTr) "Tarif fikirleri" else "Recipe ideas", if (L.isTr) "Elindekilerle" else "From what you have", Icons.Filled.RestaurantMenu, recipeAction, Modifier.weight(1f), background = Color(0xFFFFF7E8), iconColor = colors.primary, radius = 18.dp)
        }

        Spacer(Modifier.height(20.dp))
        Text(if (L.isTr) "Kilerinden bugün" else "From your pantry today", style = MaterialTheme.typography.h5, color = colors.onBackground)
        Spacer(Modifier.height(9.dp))
        TruthfulPantryPreview(state, actions, background = colors.surface, radius = 22.dp, title = if (L.isTr) "Kileriniz" else "Your pantry")

        if (state.chips.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(if (L.isTr) "Seçili malzemeler" else "Selected ingredients", style = MaterialTheme.typography.h5, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            SelectedChipsCollection(state.chips, actions.onRemoveChip, chipBgColor = colors.surface2, chipTextColor = colors.primary, chipBorderColor = colors.border)
        }

        Spacer(Modifier.height(14.dp))
        TruthfulKitchenInsight(state, recipeAction, background = colors.aiBg, accent = colors.ai, radius = 18.dp, label = if (L.isTr) "Mutfak notu" else "Kitchen note")
        if (state.shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            TruthfulShoppingListSection(state, actions)
        }
        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(18.dp))
    }
}
