package com.agentickitchen.android.ui.kitchen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import com.agentickitchen.android.ui.EditorialBrandMark
import com.agentickitchen.android.ui.LocalAppColors

@Composable
fun PremiumDarkKitchenContent(
    state: KitchenUiState,
    actions: KitchenUiActions,
    input: String,
    onInputChange: (String) -> Unit,
    filteredSuggestions: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // 1. Top Bar with Brand and Avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorialBrandMark(size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AgenticKitchen",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                    color = colors.onBackground
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { actions.onEditSetup() }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.primary, CircleShape)
                        .clickable { actions.onEditSetup() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Cinematic Serif Headline
        Text(
            text = if (L.isTr) "Bugün ne pişiriyoruz?" else "What are we cooking?",
            style = MaterialTheme.typography.h1,
            color = colors.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (L.isTr) "İyi akşamlar, harika bir şeyler yapalım." else "Good evening, let's make something great.",
            style = MaterialTheme.typography.body1,
            color = colors.onSurfaceSub
        )

        Spacer(Modifier.height(16.dp))

        // 3. Search / Composer Bar with copper mic
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Tarif ara, her şeyi sor..." else "Search recipes, ask anything..."
        )

        Spacer(Modifier.height(16.dp))

        // 4. Action Tiles (Dark rounded square tiles with copper icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTileB(
                title = if (L.isTr) "Kiler" else "Pantry",
                subtitle = if (L.isTr) "Göz at" else "See what you have",
                icon = Icons.Filled.Kitchen,
                modifier = Modifier.weight(1f),
                onClick = { actions.onOpenCookWithPantry() }
            )
            ActionTileB(
                title = if (L.isTr) "Tara" else "Scan",
                subtitle = if (L.isTr) "Malzeme ekle" else "Add ingredients",
                icon = Icons.Filled.PhotoCamera,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenIngredientCamera
            )
            ActionTileB(
                title = "AI Chef",
                subtitle = if (L.isTr) "Öneri al" else "Get suggestions",
                icon = Icons.Filled.AutoAwesome,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenCookWithPantry
            )
            ActionTileB(
                title = if (L.isTr) "Tarifler" else "Recipes",
                subtitle = if (L.isTr) "Lezzet keşfet" else "Find delicious",
                icon = Icons.Filled.RestaurantMenu,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenRecipeImport
            )
        }

        Spacer(Modifier.height(18.dp))

        // 5. Selected Ingredients (if any)
        if (state.chips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (L.isTr) "Seçtiğin malzemeler" else "Selected ingredients",
                    style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    color = colors.onBackground
                )
                Text(
                    text = "${state.chips.size} items",
                    fontSize = 12.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            SelectedChipsCollection(
                chips = state.chips,
                onRemoveChip = actions.onRemoveChip,
                chipBgColor = colors.surface,
                chipTextColor = colors.primaryLight,
                chipBorderColor = colors.primary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(18.dp))
        }

        // 6. "Your Pantry" Horizontal Section with Circular Avatar Cards + "+" Add item
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Kileriniz" else "Your Pantry",
                style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = colors.onBackground
            )
            Text(
                text = if (L.isTr) "Tümünü gör >" else "View all >",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                modifier = Modifier.clickable { actions.onOpenCookWithPantry() }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.inventory.isEmpty()) {
                val demoItems = listOf("Tomato" to "2 left", "Onion" to "1 left", "Rice" to "200g", "Mushroom" to "340g")
                demoItems.forEach { (name, qty) ->
                    PantryItemCardB(name = name, qty = qty, onClick = { actions.onAddChip(name) })
                }
            } else {
                state.inventory.take(6).forEach { item ->
                    PantryItemCardB(
                        name = item.originalName,
                        qty = "${item.quantity.toInt()} ${item.unit}",
                        onClick = { actions.onAddChip(item.originalName) }
                    )
                }
            }
            // "+" Add item tile
            Column(
                modifier = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .clickable { actions.onOpenPantryEditor(null) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .border(1.dp, colors.primary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add item",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (L.isTr) "Ekle" else "Add item",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceSub
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 7. Hero Callout Card (Emerald glow border + "Why?" link + Orange Find recipes button)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = colors.surface,
            border = BorderStroke(1.dp, colors.accent2.copy(alpha = 0.6f)),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.accent2.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            tint = colors.accent2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (L.isTr) "Mantarlar yakında bozulabilir" else "Mushrooms expire soon",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                        Text(
                            text = if (L.isTr) "Süreleri dolmadan 3 ürünü kullan" else "Use 3 items before they expire",
                            fontSize = 12.sp,
                            color = colors.onSurfaceSub
                        )
                    }
                    Text(
                        text = if (L.isTr) "Neden?" else "Why?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent2,
                        modifier = Modifier.clickable { actions.onStart() }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small ingredient badges
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        listOf("M", "T", "O").forEach { initial ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface2)
                                    .border(1.5.dp, colors.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    color = colors.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = actions.onStart,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = if (L.isTr) "Tarif bul" else "Find recipes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 8. "Recommended for you" Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Sana özel öneriler" else "Recommended for you",
                style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
                color = colors.onBackground
            )
            Text(
                text = if (L.isTr) "Tümünü gör >" else "View all >",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                modifier = Modifier.clickable { actions.onStart() }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecipeCardB(
                title = if (L.isTr) "Mantarlı Pirinç Pilavı" else "Savory Mushroom Rice Pilaf",
                meta = "35 min • Easy • 3 ingredients",
                match = "100% match",
                modifier = Modifier.weight(1f),
                onClick = actions.onStart
            )
            RecipeCardB(
                title = if (L.isTr) "Fırında Dolgulu Mantar" else "Baked Stuffed Mushrooms",
                meta = "40 min • Medium • 2 ingredients",
                match = null,
                modifier = Modifier.weight(1f),
                onClick = actions.onStart
            )
        }

        Spacer(Modifier.height(18.dp))

        // 9. AI Insight Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Insight",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = if (L.isTr) "Bu akşam için mükemmel" else "Perfect for tonight",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = if (L.isTr) "3 harika tarif için elinde tüm malzemeler var." else "You have all the ingredients for 3 great recipes.",
                        fontSize = 11.sp,
                        color = colors.onSurfaceSub
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                        .clickable { actions.onStart() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (L.isTr) "Gör" else "See all",
                        color = colors.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 10. Secondary Actions Toolbar
        SecondaryKitchenToolbar(
            chipsCount = state.chips.size,
            onOpenLibrary = actions.onOpenIngredientLibrary,
            onOpenKitchenScan = actions.onOpenKitchenScan,
            onOpenShopping = actions.onOpenShoppingImport,
            onOpenRecipeImport = actions.onOpenRecipeImport,
            onOpenSetup = actions.onEditSetup,
            onClearAll = actions.onClearAll
        )

        Spacer(Modifier.height(20.dp))

        // 11. Primary CTA
        Button(
            onClick = {
                if (state.chips.isNotEmpty()) actions.onStart() else actions.onOpenCookWithPantry()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Text(
                text = if (state.chips.isNotEmpty()) {
                    if (L.isTr) "Tarif bul • ${state.chips.size} malzeme" else "Find recipes • ${state.chips.size} ingredients"
                } else {
                    if (L.isTr) "Elimdekilerle pişir" else "Cook with what I have"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActionTileB(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = colors.onSurfaceSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PantryItemCardB(
    name: String,
    qty: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(colors.surface2)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = qty,
            fontSize = 10.sp,
            color = colors.onSurfaceSub,
            maxLines = 1
        )
    }
}

@Composable
private fun RecipeCardB(
    title: String,
    meta: String,
    match: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                )
                if (match != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surface.copy(alpha = 0.85f))
                            .border(1.dp, colors.accent2, RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = match,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent2
                        )
                    }
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = meta,
                    fontSize = 10.sp,
                    color = colors.onSurfaceSub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
