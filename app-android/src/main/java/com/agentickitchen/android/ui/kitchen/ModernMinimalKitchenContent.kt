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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FavoriteBorder
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
fun ModernMinimalKitchenContent(
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
        // 1. Top Bar
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
            IconButton(onClick = { actions.onEditSetup() }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Greeting / Headline (Calm, modern sans)
        Text(
            text = if (L.isTr) "Bugün ne pişiriyoruz?" else "What are we cooking?",
            style = MaterialTheme.typography.h1,
            color = colors.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (L.isTr) "Güzel yemekler elindekilerle başlar." else "Good food starts with what you have.",
            style = MaterialTheme.typography.body1,
            color = colors.onSurfaceSub
        )

        Spacer(Modifier.height(16.dp))

        // 3. Search / Composer Bar
        KitchenSearchBar(
            input = input,
            onInputChange = onInputChange,
            onAddChip = actions.onAddChip,
            onOpenVoiceOrCamera = actions.onOpenIngredientCamera,
            filteredSuggestions = filteredSuggestions
        )

        Spacer(Modifier.height(16.dp))

        // 4. Quick Action Cards (4 horizontal)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTileA(
                title = if (L.isTr) "Kiler" else "Pantry",
                subtitle = if (L.isTr) "Göz at" else "See what you have",
                icon = Icons.Filled.Kitchen,
                iconColor = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = { actions.onOpenCookWithPantry() }
            )
            ActionTileA(
                title = if (L.isTr) "Tara" else "Scan",
                subtitle = if (L.isTr) "Hemen ekle" else "Add items instantly",
                icon = Icons.Filled.PhotoCamera,
                iconColor = colors.primary,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenIngredientCamera
            )
            ActionTileA(
                title = "AI",
                subtitle = if (L.isTr) "Öneriler" else "Get suggestions",
                icon = Icons.Filled.AutoAwesome,
                iconColor = colors.ai,
                modifier = Modifier.weight(1f),
                onClick = actions.onOpenCookWithPantry
            )
            ActionTileA(
                title = if (L.isTr) "Tarifler" else "Recipes",
                subtitle = if (L.isTr) "Tüm tarifler" else "Browse all recipes",
                icon = Icons.Filled.RestaurantMenu,
                iconColor = colors.primary,
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
                    text = if (L.isTr) "Seçilen malzemeler" else "Selected ingredients",
                    style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = colors.onBackground
                )
                Text(
                    text = "${state.chips.size} items",
                    fontSize = 12.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            SelectedChipsCollection(
                chips = state.chips,
                onRemoveChip = actions.onRemoveChip
            )
            Spacer(Modifier.height(18.dp))
        }

        // 6. "Your pantry at a glance" section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Kilerine bir bakış" else "Your pantry at a glance",
                style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = colors.onBackground
            )
            Row(
                modifier = Modifier.clickable { actions.onOpenCookWithPantry() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Pantry horizontal scroll items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.inventory.isEmpty()) {
                val demoItems = listOf("Tomato" to "2", "Onion" to "1", "Rice" to "200g", "Mushroom" to "340g")
                demoItems.forEach { (name, qty) ->
                    PantryItemPillA(
                        name = name,
                        qty = qty,
                        onClick = { actions.onAddChip(name) }
                    )
                }
            } else {
                state.inventory.take(6).forEach { item ->
                    PantryItemPillA(
                        name = item.originalName,
                        qty = "${item.quantity.toInt()} ${item.unit}",
                        onClick = { actions.onAddChip(item.originalName) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 7. "Recipes for you" section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (L.isTr) "Sana özel tarifler" else "Recipes for you",
                style = MaterialTheme.typography.h6.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
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
            RecipeCardA(
                title = if (L.isTr) "Mantarlı Pirinç Pilavı" else "Savory Mushroom Rice Pilaf",
                time = "35 min • Easy",
                match = "100% match",
                matchColor = colors.success,
                modifier = Modifier.weight(1f),
                onClick = actions.onStart
            )
            RecipeCardA(
                title = if (L.isTr) "Fırında Dolgulu Mantar" else "Baked Stuffed Mushrooms",
                time = "40 min • Medium",
                match = "1 missing",
                matchColor = colors.warn,
                modifier = Modifier.weight(1f),
                onClick = actions.onStart
            )
        }

        Spacer(Modifier.height(18.dp))

        // 8. AI Insight Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onStart() },
            shape = RoundedCornerShape(16.dp),
            backgroundColor = colors.surface2,
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
                        .background(colors.ai.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = colors.ai,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Insight",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = if (L.isTr) "Mantarların süresi dolmak üzere" else "Mushrooms expire soon",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )
                    Text(
                        text = if (L.isTr) "Onları bu tariflerden birinde kullan." else "Use them in one of these recipes.",
                        fontSize = 12.sp,
                        color = colors.onSurfaceSub
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 9. Secondary Actions Toolbar (Library, Kitchen scan, Shopping, Import, Setup, Clear)
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

        // 10. Primary CTA Button (Green pill)
        val buttonText = when {
            state.chips.isNotEmpty() -> if (L.isTr) "Tarif bul • ${state.chips.size} malzeme" else "Find recipes • ${state.chips.size} ingredients"
            state.inventory.isNotEmpty() -> if (L.isTr) "Elimdekilerle pişir" else "Cook with what I have"
            else -> if (L.isTr) "Tarif bul" else "Find recipes"
        }
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
                text = buttonText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActionTileA(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
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
private fun PantryItemPillA(
    name: String,
    qty: String,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onBackground
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = qty,
            fontSize = 12.sp,
            color = colors.onSurfaceSub
        )
    }
}

@Composable
private fun RecipeCardA(
    title: String,
    time: String,
    match: String,
    matchColor: Color,
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
                    .height(90.dp)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                )
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
                    text = time,
                    fontSize = 11.sp,
                    color = colors.onSurfaceSub
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = match,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = matchColor
                )
            }
        }
    }
}
