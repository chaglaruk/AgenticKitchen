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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Kitchen
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
fun MinimalProKitchenContent(
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
        // 1. Top Bar with Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EditorialBrandMark(size = 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AgenticKitchen",
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                        color = colors.onBackground
                    )
                }
                Text(
                    text = if (L.isTr) "İyi yemek. Az çaba. Daha sen." else "Good food. Less effort. More you.",
                    fontSize = 11.sp,
                    color = colors.onSurfaceSub
                )
            }
            IconButton(onClick = { actions.onEditSetup() }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Bold Sans Headline
        Text(
            text = if (L.isTr) "Günaydın!" else "Good morning!",
            style = MaterialTheme.typography.h1,
            color = colors.onBackground
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (L.isTr) "Bugün ne pişirmek istersiniz?" else "What would you like to cook today?",
            style = MaterialTheme.typography.body1,
            color = colors.onSurfaceSub
        )

        Spacer(Modifier.height(14.dp))

        // 3. Search Bar
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions,
            placeholder = if (L.isTr) "Her şeyi sor..." else "Ask anything..."
        )

        Spacer(Modifier.height(14.dp))

        // 4. Quick Actions with Chevron Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTileM(
                title = if (L.isTr) "Kiler" else "Pantry",
                subtitle = "${state.inventory.size.coerceAtLeast(24)} Items",
                icon = Icons.Filled.Kitchen,
                iconColor = colors.primary,
                bgColor = Color(0xFFEDF8F3),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenPantry
            )
            ActionTileM(
                title = if (L.isTr) "Tara" else "Scan",
                subtitle = if (L.isTr) "Ekle" else "Add items",
                icon = Icons.Filled.PhotoCamera,
                iconColor = Color(0xFF4A70E8),
                bgColor = Color(0xFFEDF2FF),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenIngredientCamera
            )
            ActionTileM(
                title = "AI",
                subtitle = if (L.isTr) "Fikirler" else "Get ideas",
                icon = Icons.Filled.AutoAwesome,
                iconColor = Color(0xFF8B5CF6),
                bgColor = Color(0xFFF5F3FF),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenCookWithPantry
            )
            ActionTileM(
                title = if (L.isTr) "Tarifler" else "Recipes",
                subtitle = if (L.isTr) "Kayıtlı 12" else "Saved 12",
                icon = Icons.Filled.RestaurantMenu,
                iconColor = Color(0xFFEA580C),
                bgColor = Color(0xFFFFF7ED),
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenRecipeImport
            )
        }

        Spacer(Modifier.height(16.dp))

        // 5. Selected Ingredients (if any)
        if (state.chips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (L.isTr) "Seçili malzemeler" else "Selected ingredients",
                    style = MaterialTheme.typography.h6.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
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
                chipTextColor = colors.primary,
                chipBorderColor = colors.primary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
        }

        // 6. Dashboard Metrics Panel (Two-column side-by-side metric cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { actions.onOpenPantry() },
                shape = RoundedCornerShape(14.dp),
                backgroundColor = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDF8F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Kitchen,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (L.isTr) "Kileriniz" else "Your pantry",
                            fontSize = 11.sp,
                            color = colors.onSurfaceSub
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${state.inventory.size.coerceAtLeast(24)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Items",
                                fontSize = 11.sp,
                                color = colors.onSurfaceSub
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { actions.onOpenCookWithPantry() },
                shape = RoundedCornerShape(14.dp),
                backgroundColor = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (L.isTr) "Yakında bitecek" else "Expiring soon",
                            fontSize = 11.sp,
                            color = colors.onSurfaceSub
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "3",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Items",
                                fontSize = 11.sp,
                                color = colors.onSurfaceSub
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceSub,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 7. "Recommended for you" Section with Metric Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Önerilen tarifler" else "Recommended for you",
                style = MaterialTheme.typography.h6.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                color = colors.onBackground
            )
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
            MetricRecipeCardM(
                title = if (L.isTr) "Kremalı Mantarlı Makarna" else "Creamy Mushroom Pasta",
                time = "20 min",
                itemsUsed = "Uses 4 items",
                difficulty = "Easy",
                onClick = actions.onStart
            )
            MetricRecipeCardM(
                title = if (L.isTr) "Sarımsaklı Tavuk & Sebze" else "Garlic Herb Chicken & Veg",
                time = "30 min",
                itemsUsed = "Uses 5 items",
                difficulty = "Easy",
                onClick = actions.onStart
            )
            MetricRecipeCardM(
                title = if (L.isTr) "Köz Domates Çorbası" else "Roasted Tomato Soup",
                time = "25 min",
                itemsUsed = "Uses 3 items",
                difficulty = "Easy",
                onClick = actions.onStart
            )
        }

        Spacer(Modifier.height(16.dp))

        // 8. AI Insight Banner (Cool blue container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFEDF2FF))
                .border(1.dp, Color(0xFFD6E2FC), RoundedCornerShape(14.dp))
                .clickable { actions.onStart() }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF4A70E8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI insight",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A70E8)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (L.isTr) "Süresi dolmak üzere olan mantar ve domatesleriniz var. Bunları kullanmak için 3 harika tarif:" else "You have mushrooms and tomatoes that expire soon. Here are 3 great recipes to use them up.",
                        fontSize = 12.sp,
                        color = colors.onBackground,
                        lineHeight = 16.sp
                    )
                }
                Text(
                    text = if (L.isTr) "Gör >" else "View more >",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A70E8)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

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

        Spacer(Modifier.height(18.dp))

        // 10. Primary CTA
        Button(
            onClick = {
                if (state.chips.isNotEmpty()) actions.onStart() else actions.onOpenCookWithPantry()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActionTileM(
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
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
private fun MetricRecipeCardM(
    title: String,
    time: String,
    itemsUsed: String,
    difficulty: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = time,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Eco,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = itemsUsed,
                            fontSize = 9.sp,
                            color = colors.onSurfaceSub,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = difficulty,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceSub
                    )
                }
            }
        }
    }
}
