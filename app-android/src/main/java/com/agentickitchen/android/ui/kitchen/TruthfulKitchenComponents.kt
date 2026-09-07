package com.agentickitchen.android.ui.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import com.agentickitchen.android.ui.LocalAppColors
import com.agentickitchen.shared.inventory.LocalIngredientResolver
import java.math.BigDecimal

@Composable
fun KitchenActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LocalAppColors.current.surface,
    iconColor: Color = LocalAppColors.current.primary,
    radius: Dp = 16.dp,
    compact: Boolean = false
) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(radius),
        backgroundColor = background,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 10.dp else 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(if (compact) 20.dp else 23.dp))
            Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
            Text(title, color = colors.onBackground, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                subtitle,
                color = colors.onSurfaceSub,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KitchenMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalAppColors.current.primary,
    background: Color = LocalAppColors.current.surface,
    radius: Dp = 12.dp
) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        backgroundColor = background,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = colors.onSurfaceSub, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TruthfulPantryPreview(
    state: KitchenUiState,
    actions: KitchenUiActions,
    modifier: Modifier = Modifier,
    title: String = if (L.isTr) "Kiler" else "Pantry",
    background: Color = LocalAppColors.current.surface,
    radius: Dp = 18.dp,
    compact: Boolean = false
) {
    val colors = LocalAppColors.current
    val facts = state.dashboardFacts()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius),
        backgroundColor = background,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(Modifier.padding(if (compact) 12.dp else 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onOpenPantry, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(
                        if (L.isTr) "${facts.pantryCount} ürün · Yönet" else "${facts.pantryCount} items · Manage",
                        color = colors.primary,
                        fontSize = 11.sp
                    )
                }
            }

            if (state.inventory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface2, RoundedCornerShape(radius.coerceAtMost(14.dp)))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            if (L.isTr) "Kiler henüz boş" else "Your pantry is empty",
                            color = colors.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            if (L.isTr) "Bir ürün ekle, mutfağı tara veya alışveriş listesini içe aktar." else "Add an item, scan your kitchen, or import shopping.",
                            color = colors.onSurfaceSub,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { actions.onOpenPantryEditor(null) }) { Text(if (L.isTr) "+ Ürün ekle" else "+ Add item", color = colors.primary) }
                            TextButton(onClick = actions.onOpenKitchenScan) { Text(if (L.isTr) "Mutfağı tara" else "Scan kitchen", color = colors.primary) }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.inventory.take(if (compact) 4 else 6).forEach { item ->
                        val quantity = "${BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()} ${LocalIngredientResolver.localizeUnit(item.unit, L.isTr)}"
                        Card(
                            modifier = Modifier.width(if (compact) 106.dp else 120.dp).clickable { actions.onOpenPantryEditor(item) },
                            shape = RoundedCornerShape(radius.coerceAtMost(14.dp)),
                            backgroundColor = colors.surface2,
                            border = BorderStroke(1.dp, colors.border),
                            elevation = 0.dp
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(item.originalName, color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                Text(quantity, color = colors.onSurfaceSub, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.width(82.dp).clickable { actions.onOpenPantryEditor(null) },
                        shape = RoundedCornerShape(radius.coerceAtMost(14.dp)),
                        backgroundColor = colors.surface2,
                        border = BorderStroke(1.dp, colors.border),
                        elevation = 0.dp
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Add, contentDescription = if (L.isTr) "Ürün ekle" else "Add item", tint = colors.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(3.dp))
                            Text(if (L.isTr) "Ekle" else "Add", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TruthfulKitchenInsight(
    state: KitchenUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LocalAppColors.current.aiBg,
    accent: Color = LocalAppColors.current.ai,
    radius: Dp = 16.dp,
    label: String = if (L.isTr) "Mutfak içgörüsü" else "Kitchen insight"
) {
    val colors = LocalAppColors.current
    val facts = state.dashboardFacts()
    val expiringNames = facts.expiringItems.take(3).joinToString(" • ") { it.originalName }
    val title: String
    val detail: String
    val icon: ImageVector

    if (facts.expiringItems.isNotEmpty()) {
        icon = Icons.Filled.Warning
        title = if (L.isTr) "${facts.expiringItems.size} ürün yakında kullanılmalı" else "${facts.expiringItems.size} items need attention"
        detail = expiringNames
    } else if (!facts.intelligenceMessage.isNullOrBlank()) {
        icon = Icons.Filled.AutoAwesome
        title = label
        detail = facts.intelligenceMessage
    } else {
        icon = Icons.Filled.AutoAwesome
        title = if (L.isTr) "Mutfak hazırlığı ${facts.readinessScore}/100" else "Kitchen readiness ${facts.readinessScore}/100"
        detail = if (facts.pantryCount > 0) {
            if (L.isTr) "Mevcut kilerini kullanarak kişiselleştirilmiş tarif fikirleri oluştur." else "Generate tailored recipe ideas from your current pantry."
        } else {
            if (L.isTr) "Daha iyi öneriler için kilerine birkaç ürün ekle." else "Add a few pantry items for more useful suggestions."
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(radius),
        backgroundColor = background,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, color = colors.onSurfaceSub, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TruthfulShoppingListSection(state: KitchenUiState, actions: KitchenUiActions, modifier: Modifier = Modifier) {
    if (state.shoppingList.isEmpty()) return
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (L.isTr) "Alışveriş listesi" else "Shopping list", color = colors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (state.shoppingList.any { it.checked }) {
                    TextButton(onClick = actions.onClearCheckedShoppingItems) {
                        Text(if (L.isTr) "Tamamlananları temizle" else "Clear completed", color = colors.primary, fontSize = 10.sp)
                    }
                }
            }
            state.shoppingList.take(5).forEach { item ->
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { actions.onToggleShoppingItem(item.id, !item.checked) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = if (item.checked) colors.success else colors.onSurfaceSub, modifier = Modifier.size(19.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.originalName, color = if (item.checked) colors.onSurfaceSub else colors.onBackground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${BigDecimal.valueOf(item.quantity).stripTrailingZeros().toPlainString()} ${LocalIngredientResolver.localizeUnit(item.unit, L.isTr)}",
                            color = colors.onSurfaceSub,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = { actions.onDeleteShoppingItem(item.id) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = if (L.isTr) "Listeden kaldır" else "Remove from list", tint = colors.onSurfaceSub, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun KitchenPrimaryAction(state: KitchenUiState, actions: KitchenUiActions, modifier: Modifier = Modifier, radius: Dp = 999.dp) {
    val colors = LocalAppColors.current
    val label: String
    val onClick: () -> Unit
    when {
        state.chips.isNotEmpty() -> {
            label = if (L.isTr) "Tarif bul • ${state.chips.size} malzeme" else "Find recipes • ${state.chips.size} ingredients"
            onClick = actions.onStart
        }
        state.inventory.isNotEmpty() -> {
            label = if (L.isTr) "Elimdekilerle pişir" else "Cook with what I have"
            onClick = actions.onOpenCookWithPantry
        }
        else -> {
            label = if (L.isTr) "Malzeme ekle" else "Add ingredients"
            onClick = actions.onOpenIngredientLibrary
        }
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(radius),
        colors = ButtonDefaults.buttonColors(backgroundColor = colors.primary, contentColor = colors.onPrimary),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
