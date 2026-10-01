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
fun MinimalProKitchenContent(
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
        modifier = modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EditorialBrandMark(size = 21.dp)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text("AgenticKitchen", style = MaterialTheme.typography.h6, color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text(if (L.isTr) "MUTFAK DURUMU" else "KITCHEN STATUS", color = colors.onSurfaceSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            IconButton(onClick = actions.onEditSetup) {
                Icon(Icons.Filled.Tune, contentDescription = if (L.isTr) "Kurulum" else "Setup", tint = colors.onSurfaceSub, modifier = Modifier.size(19.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KitchenMetricTile(if (L.isTr) "Stok" else "In stock", facts.pantryCount.toString(), Modifier.weight(1f), accent = colors.primary, radius = 6.dp)
            KitchenMetricTile(if (L.isTr) "Seçili" else "Selected", facts.selectedCount.toString(), Modifier.weight(1f), accent = colors.ai, radius = 6.dp)
            KitchenMetricTile(if (L.isTr) "Yakında" else "Use soon", facts.expiringItems.size.toString(), Modifier.weight(1f), accent = colors.warn, radius = 6.dp)
            KitchenMetricTile(if (L.isTr) "Hazır" else "Ready", "${facts.readinessScore}%", Modifier.weight(1f), accent = colors.success, radius = 6.dp)
        }

        Spacer(Modifier.height(10.dp))
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Malzeme ekle..." else "Add ingredients..."
        )

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KitchenActionTile(if (L.isTr) "Kiler" else "Pantry", if (L.isTr) "Yönet" else "Manage", Icons.Filled.Kitchen, actions.onOpenPantry, Modifier.weight(1f), background = Color(0xFFEDF8F3), radius = 6.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Fotoğraf" else "Photo", if (L.isTr) "Ekle" else "Add", Icons.Filled.PhotoCamera, actions.onOpenIngredientCamera, Modifier.weight(1f), background = Color(0xFFEDF2FF), iconColor = colors.ai, radius = 6.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Hızlı" else "Quick cook", if (L.isTr) "Kileri kullan" else "Use pantry", Icons.Filled.AutoAwesome, actions.onOpenCookWithPantry, Modifier.weight(1f), background = colors.aiBg, iconColor = colors.ai, radius = 6.dp, compact = true)
            KitchenActionTile(if (L.isTr) "Tarif" else "Recipes", if (L.isTr) "Oluştur" else "Generate", Icons.Filled.RestaurantMenu, recipeAction, Modifier.weight(1f), background = colors.surface, radius = 6.dp, compact = true)
        }

        if (state.chips.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (L.isTr) "Seçili malzemeler" else "Selected ingredients", color = colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(state.chips.size.toString(), color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            SelectedChipsCollection(state.chips, actions.onRemoveChip, chipBgColor = colors.surface, chipTextColor = colors.onBackground, chipBorderColor = colors.border)
        }

        Spacer(Modifier.height(12.dp))
        TruthfulKitchenInsight(state, recipeAction, background = Color(0xFFEDF2FF), accent = colors.ai, radius = 8.dp, label = if (L.isTr) "Durum içgörüsü" else "Status insight")
        Spacer(Modifier.height(10.dp))
        TruthfulPantryPreview(state, actions, background = colors.surface, radius = 8.dp, compact = true, title = if (L.isTr) "Kiler envanteri" else "Pantry inventory")
        if (state.shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            TruthfulShoppingListSection(state, actions)
        }
        Spacer(Modifier.height(10.dp))
        SecondaryKitchenToolbar(
            chipsCount = state.chips.size,
            onOpenLibrary = actions.onOpenIngredientLibrary,
            onOpenKitchenScan = actions.onOpenKitchenScan,
            onOpenShopping = actions.onOpenShoppingImport,
            onOpenRecipeImport = actions.onOpenRecipeImport,
            onOpenSetup = actions.onEditSetup,
            onClearAll = actions.onClearAll
        )
        Spacer(Modifier.height(14.dp))
        KitchenPrimaryAction(state, actions, radius = 6.dp)
        Spacer(Modifier.height(12.dp))
    }
}
