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
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun LuxeApplianceKitchenContent(
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
        // 1. Top Bar with Appliance Dark Controls
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
                        .border(1.dp, colors.border, CircleShape)
                        .clickable { actions.onEditSetup() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = colors.onBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Tracked Uppercase Subhead + Serif Headline + Herb Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (L.isTr) "GÜNAYDIN" else "GOOD MORNING",
                    style = MaterialTheme.typography.overline,
                    color = colors.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (L.isTr) "Bugün ne pişirmek istersiniz?" else "What would you like to cook today?",
                    style = MaterialTheme.typography.h1,
                    color = colors.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (L.isTr) "Harika yemekler\nparlak günler" else "Great food\nbrighter days",
                    fontSize = 10.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Search / Control Input Field
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Her şeyi sor..." else "Ask anything..."
        )

        Spacer(Modifier.height(16.dp))

        // 4. Modular Control Blocks (Pantry highlighted green, Scan, AI Chef in amber, Recipes)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlBlockK(
                title = if (L.isTr) "Kiler" else "Pantry",
                subtitle = "${state.inventory.size.coerceAtLeast(24)} items",
                icon = Icons.Filled.Kitchen,
                iconColor = colors.accent,
                bgColor = colors.surface2,
                borderColor = colors.accent.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
                onClick = { actions.onOpenCookWithPantry() }
            )
            ControlBlockK(
                title = if (L.isTr) "Tara" else "Scan",
                subtitle = if (L.isTr) "Öğe ekle" else "Add items",
                icon = Icons.Filled.PhotoCamera,
                iconColor = Color(0xFF6DA4F0),
                bgColor = colors.surface,
                borderColor = colors.border,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenIngredientCamera
            )
            ControlBlockK(
                title = "AI Chef",
                subtitle = if (L.isTr) "Fikir al" else "Get ideas",
                icon = Icons.Filled.AutoAwesome,
                iconColor = colors.primary,
                bgColor = Color(0xFF201A12),
                borderColor = colors.primary.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenCookWithPantry
            )
            ControlBlockK(
                title = if (L.isTr) "Tarifler" else "Recipes",
                subtitle = if (L.isTr) "Kayıtlı 12" else "Saved 12",
                icon = Icons.Filled.RestaurantMenu,
                iconColor = Color(0xFFAC7DF2),
                bgColor = colors.surface,
                borderColor = colors.border,
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
                    text = if (L.isTr) "Seçili malzemeler" else "Selected ingredients",
                    style = MaterialTheme.typography.h6.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
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
                chipBgColor = colors.surface2,
                chipTextColor = colors.accent,
                chipBorderColor = colors.accent.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(18.dp))
        }

        // 6. "Your pantry" Container Panel (Modular appliance container with ingredients + status strip)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header inside container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Kitchen,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (L.isTr) "Kileriniz" else "Your pantry",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    }
                    Row(
                        modifier = Modifier.clickable { actions.onOpenCookWithPantry() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.inventory.size.coerceAtLeast(24)} items",
                            fontSize = 12.sp,
                            color = colors.onSurfaceSub
                        )
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = colors.onSurfaceSub,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Ingredient circular badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.inventory.isEmpty()) {
                        val demoItems = listOf(
                            "Tomatoes" to "6 left",
                            "Mushrooms" to "340 g",
                            "Onion" to "1 left",
                            "Rice" to "200g"
                        )
                        demoItems.forEach { (name, qty) ->
                            IngredientBadgeK(name = name, qty = qty, onClick = { actions.onAddChip(name) })
                        }
                    } else {
                        state.inventory.take(6).forEach { item ->
                            IngredientBadgeK(
                                name = item.originalName,
                                qty = "${item.quantity.toInt()} ${item.unit}",
                                onClick = { actions.onAddChip(item.originalName) }
                            )
                        }
                    }

                    // "+" Add item dashed circle
                    Column(
                        modifier = Modifier
                            .clickable { actions.onOpenPantryEditor(null) }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(colors.surface2)
                                .border(1.dp, colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add item",
                                tint = colors.onSurfaceSub,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (L.isTr) "Ekle" else "Add item",
                            fontSize = 10.sp,
                            color = colors.onSurfaceSub
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Integrated status strip inside panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (L.isTr) "3 malzemenin süresi dolmak üzere" else "3 ingredients expire soon",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onBackground
                                )
                                Text(
                                    text = "Mushrooms • Tomatoes • Creme Fraiche",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceSub
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { actions.onOpenCookWithPantry() },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, colors.accent),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (L.isTr) "Gör >" else "View >",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 7. "Recommended for you" Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (L.isTr) "Sana özel öneriler" else "Recommended for you",
                    style = MaterialTheme.typography.h6.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = colors.onBackground
                )
            }
            Text(
                text = if (L.isTr) "Tümünü gör >" else "See all >",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                modifier = Modifier.clickable { actions.onStart() }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecipeCardK(
                title = if (L.isTr) "Mantarlı Pirinç Pilavı" else "Savory Mushroom Rice Pilaf",
                meta = "35 min • Easy",
                tag = "Uses 3 pantry items",
                tagColor = colors.accent,
                onClick = actions.onStart
            )
            RecipeCardK(
                title = if (L.isTr) "Sarımsaklı Otlu Tavuk" else "Garlic Herb Chicken Pasta",
                meta = "25 min • Easy",
                tag = "Popular this week",
                tagColor = colors.primary,
                onClick = actions.onStart
            )
            RecipeCardK(
                title = if (L.isTr) "Akdeniz Kasesi" else "Mediterranean Bowl",
                meta = "20 min • Medium",
                tag = "Healthy choice",
                tagColor = colors.accent,
                onClick = actions.onStart
            )
        }

        Spacer(Modifier.height(18.dp))

        // 8. "AI insight" Card (Amber/gold gradient banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF281C10), Color(0xFF1E281E))
                    )
                )
                .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { actions.onStart() }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI insight",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (L.isTr) "7 akşam yemeği tarifi için yeterli malzemen var. Domateslerini yakında kullanmayı dene!" else "You have enough ingredients for 7 dinner recipes with what you have. Try using your tomatoes soon!",
                        fontSize = 12.sp,
                        color = colors.onBackground,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 9. Secondary Actions Toolbar
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

        // 10. Primary CTA
        Button(
            onClick = {
                if (state.chips.isNotEmpty()) actions.onStart() else actions.onOpenCookWithPantry()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
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
private fun ControlBlockK(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = bgColor,
        border = BorderStroke(1.dp, borderColor),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
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
private fun IngredientBadgeK(
    name: String,
    qty: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(colors.surface2)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground,
            maxLines = 1
        )
        Text(
            text = qty,
            fontSize = 10.sp,
            color = colors.onSurfaceSub,
            maxLines = 1
        )
    }
}

@Composable
private fun RecipeCardK(
    title: String,
    meta: String,
    tag: String,
    tagColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    fontSize = 10.sp,
                    color = colors.onSurfaceSub,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
