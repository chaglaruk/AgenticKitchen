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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestaurantMenu
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
fun WarmEditorialKitchenContent(
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
        // 1. Top Bar with Botanical Badge
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (L.isTr) "Doğal malzemeler ♡" else "Real ingredients ♡",
                        fontSize = 10.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { actions.onEditSetup() }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Warm Editorial Serif Display Headline
        Text(
            text = if (L.isTr) "Günaydın!" else "Good morning!",
            style = MaterialTheme.typography.h1,
            color = colors.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (L.isTr) "Bugün ne pişirmek istersiniz?" else "What would you like to cook today?",
            style = MaterialTheme.typography.body1,
            color = colors.onSurfaceSub
        )

        Spacer(Modifier.height(16.dp))

        // 3. Search Bar
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Malzeme ekle (tavuk, ıspanak, pirinç)..." else "Add ingredients (e.g. chicken, spinach, rice)..."
        )

        Spacer(Modifier.height(16.dp))

        // 4. Pastel Tinted Action Cards (Pantry in sage, Scan in terracotta, Ask AI in lavender, Recipes in warm amber)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PastelActionCardL(
                title = if (L.isTr) "Kiler" else "Pantry",
                subtitle = if (L.isTr) "Göz at" else "See what you have",
                icon = Icons.Filled.Kitchen,
                iconColor = colors.primary,
                bgColor = Color(0xFFEDF6F0),
                modifier = Modifier.weight(1f),
                onClick = { actions.onOpenCookWithPantry() }
            )
            PastelActionCardL(
                title = if (L.isTr) "Tara" else "Scan",
                subtitle = if (L.isTr) "Malzeme ekle" else "Add ingredients",
                icon = Icons.Filled.PhotoCamera,
                iconColor = colors.accent,
                bgColor = Color(0xFFFDF0EC),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenIngredientCamera
            )
            PastelActionCardL(
                title = "Ask AI",
                subtitle = if (L.isTr) "Fikir al" else "Get ideas",
                icon = Icons.Filled.AutoAwesome,
                iconColor = colors.ai,
                bgColor = Color(0xFFF1EEFF),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenCookWithPantry
            )
            PastelActionCardL(
                title = if (L.isTr) "Tarifler" else "Recipes",
                subtitle = if (L.isTr) "Keşfet" else "Browse & explore",
                icon = Icons.Filled.RestaurantMenu,
                iconColor = Color(0xFFC97F3A),
                bgColor = Color(0xFFFDF6E9),
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
                chipTextColor = colors.primary,
                chipBorderColor = colors.primary.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(18.dp))
        }

        // 6. "Your pantry" Container Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (L.isTr) "Kileriniz" else "Your pantry",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
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

                // Square rounded item cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.inventory.isEmpty()) {
                        val demoItems = listOf(
                            "Tomatoes" to "4",
                            "Mushrooms" to "340 g",
                            "Onion" to "1",
                            "Rice" to "200 g"
                        )
                        demoItems.forEach { (name, qty) ->
                            PantrySquareItemL(name = name, qty = qty, onClick = { actions.onAddChip(name) })
                        }
                    } else {
                        state.inventory.take(6).forEach { item ->
                            PantrySquareItemL(
                                name = item.originalName,
                                qty = "${item.quantity.toInt()} ${item.unit}",
                                onClick = { actions.onAddChip(item.originalName) }
                            )
                        }
                    }

                    // "+" See all green pill tile
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface2)
                            .clickable { actions.onOpenCookWithPantry() }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "See all",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (L.isTr) "Tümü" else "See all",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Terracotta subcard inside: "3 items expire soon"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFDF1ED))
                        .clickable { actions.onOpenCookWithPantry() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (L.isTr) "3 ürünün süresi dolmak üzere" else "3 items expire soon",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                            Text(
                                text = "Mushrooms • Spinach • Milk",
                                fontSize = 11.sp,
                                color = colors.onSurfaceSub
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 7. "Ideas with what you have" Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Elindekilerle tarif fikirleri" else "Ideas with what you have",
                style = MaterialTheme.typography.h6.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = colors.onBackground
            )
            Text(
                text = if (L.isTr) "Tümünü gör" else "See all",
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
            CookbookRecipeCardL(
                title = if (L.isTr) "Kremalı Mantarlı Makarna" else "Creamy Mushroom Pasta",
                time = "25 min • Easy",
                onClick = actions.onStart,
                onQuickAdd = { actions.onAddChip("Pasta") }
            )
            CookbookRecipeCardL(
                title = if (L.isTr) "Sarımsaklı Tavuk Kasesi" else "Garlic Chicken Rice Bowl",
                time = "30 min • Easy",
                onClick = actions.onStart,
                onQuickAdd = { actions.onAddChip("Chicken") }
            )
            CookbookRecipeCardL(
                title = if (L.isTr) "Domatesli Makarna" else "Tomato Pasta",
                time = "20 min • Easy",
                onClick = actions.onStart,
                onQuickAdd = { actions.onAddChip("Tomato") }
            )
        }

        Spacer(Modifier.height(18.dp))

        // 8. AI Insight Card (Soft lavender banner with lightbulb icon)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF1EDFC))
                .border(1.dp, Color(0xFFDFD8F7), RoundedCornerShape(16.dp))
                .clickable { actions.onStart() }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.ai.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = colors.ai,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI insight",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ai
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (L.isTr) "Süresi dolmak üzere olan mantarların var. Hızlı ve lezzetli kremalı bir makarna yapmayı dene!" else "You have mushrooms that will expire soon. Try a creamy mushroom pasta — a quick, delicious way to use them up!",
                        fontSize = 12.sp,
                        color = colors.onBackground,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.ai,
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
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
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
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PastelActionCardL(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = bgColor,
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
private fun PantrySquareItemL(
    name: String,
    qty: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
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
private fun CookbookRecipeCardL(
    title: String,
    time: String,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .width(145.dp)
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
                    .height(90.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(34.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                        text = time,
                        fontSize = 10.sp,
                        color = colors.onSurfaceSub
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable(onClick = onQuickAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Quick add",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
