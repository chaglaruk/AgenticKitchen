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
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun LuxeApplianceKitchenContent(
    state: KitchenUiState,
    actions: KitchenUiActions,
    input: String,
    onInputChange: (String) -> Unit,
    filteredSuggestions: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val facts = state.dashboardFacts()
    val recipeAction = { if (state.chips.isNotEmpty()) actions.onStart() else actions.onOpenCookWithPantry() }

    Column(
        modifier = modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EditorialBrandMark(size = 23.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("AGENTICKITCHEN", color = colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text(if (L.isTr) "MUTFAK KONTROLÜ" else "KITCHEN CONTROL", color = colors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            IconButton(onClick = actions.onEditSetup) {
                Icon(Icons.Filled.Tune, contentDescription = if (L.isTr) "Kurulum" else "Setup", tint = colors.primary, modifier = Modifier.size(21.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(if (L.isTr) "Bugün ne pişirmek istersiniz?" else "What would you like to cook today?", style = MaterialTheme.typography.h1, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            KitchenMetricTile(if (L.isTr) "Kiler" else "Pantry", facts.pantryCount.toString(), Modifier.weight(1f), accent = colors.accent, background = colors.surface2, radius = 10.dp)
            KitchenMetricTile(if (L.isTr) "Seçili" else "Selected", facts.selectedCount.toString(), Modifier.weight(1f), accent = colors.primary, background = colors.surface, radius = 10.dp)
            KitchenMetricTile(if (L.isTr) "Yakında" else "Use soon", facts.expiringItems.size.toString(), Modifier.weight(1f), accent = colors.warn, background = colors.surface, radius = 10.dp)
            KitchenMetricTile(if (L.isTr) "Hazırlık" else "Ready", "${facts.readinessScore}%", Modifier.weight(1f), accent = colors.accent, background = colors.surface, radius = 10.dp)
        }

        Spacer(Modifier.height(12.dp))
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Malzeme veya komut ekle..." else "Add an ingredient..."
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            KitchenActionTile(if (L.isTr) "Kiler" else "Pantry", if (L.isTr) "Stok" else "Inventory", Icons.Filled.Kitchen, actions.onOpenPantry, Modifier.weight(1f), background = colors.surface2, iconColor = colors.accent, radius = 10.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Fotoğraf" else "Photo", if (L.isTr) "Malzeme" else "Ingredient", Icons.Filled.PhotoCamera, actions.onOpenIngredientCamera, Modifier.weight(1f), background = colors.surface, radius = 10.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Hızlı pişir" else "Quick cook", if (L.isTr) "Kileri kullan" else "Use pantry", Icons.Filled.AutoAwesome, actions.onOpenCookWithPantry, Modifier.weight(1f), background = colors.aiBg, iconColor = colors.primary, radius = 10.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Tarif" else "Recipes", if (L.isTr) "Fikir oluştur" else "Generate", Icons.Filled.RestaurantMenu, recipeAction, Modifier.weight(1f), background = colors.surface, iconColor = colors.primary, radius = 10.dp, compact = true)
        }

        if (state.chips.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(if (L.isTr) "SEÇİLİ MALZEMELER" else "SELECTED INGREDIENTS", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(7.dp))
            SelectedChipsCollection(state.chips, actions.onRemoveChip, chipBgColor = colors.surface2, chipTextColor = colors.accent, chipBorderColor = colors.border)
        }

        Spacer(Modifier.height(14.dp))
        TruthfulPantryPreview(state, actions, background = colors.surface, radius = 12.dp, compact = true, title = if (L.isTr) "Kiler modülü" else "Pantry module")
        Spacer(Modifier.height(10.dp))
        TruthfulKitchenInsight(state, recipeAction, background = colors.aiBg, accent = colors.primary, radius = 12.dp, label = if (L.isTr) "Kontrol içgörüsü" else "Control insight")
        if (state.shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            TruthfulShoppingListSection(state, actions)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryKitchenToolbar(
            chipsCount = state.chips.size,
            onOpenLibrary = actions.onOpenIngredientLibrary,
            onOpenKitchenScan = actions.onOpenKitchenScan,
            onOpenShopping = actions.onOpenShoppingImport,
            onOpenRecipeImport = actions.onOpenRecipeImport,
            onOpenSetup = actions.onEditSetup,
            onClearAll = actions.onClearAll
        )
        Spacer(Modifier.height(16.dp))
        KitchenPrimaryAction(state, actions, radius = 10.dp)
        Spacer(Modifier.height(14.dp))
    }
}
