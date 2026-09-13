package com.agentickitchen.android.ui.kitchen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import com.agentickitchen.android.ui.LocalAppColors
import com.agentickitchen.android.ui.LocalThemeSpec

@Composable
fun KitchenSearchBar(
    input: String,
    onInputChange: (String) -> Unit,
    onAddChip: (String) -> Unit,
    onOpenVoiceOrCamera: () -> Unit,
    filteredSuggestions: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = if (L.isTr) "Ne pişirmek istersin? Malzeme yaz..." else "Tell me what you want to cook..."
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(spec.cornerRadius.dp),
            backgroundColor = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            elevation = if (spec.isLight) 1.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (input.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(color = colors.onSurfaceSub, fontSize = 14.sp)
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (input.isNotBlank()) {
                                input.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(onAddChip)
                                onInputChange("")
                                keyboard?.hide()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (input.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary)
                            .clickable {
                                input.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach(onAddChip)
                                onInputChange("")
                                keyboard?.hide()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (L.isTr) "Ekle" else "Add",
                            color = colors.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(
                    onClick = onOpenVoiceOrCamera,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Camera",
                        tint = colors.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = filteredSuggestions.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    filteredSuggestions.take(5).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddChip(suggestion)
                                    onInputChange("")
                                    keyboard?.hide()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedChipsCollection(
    chips: List<String>,
    onRemoveChip: (String) -> Unit,
    modifier: Modifier = Modifier,
    chipBgColor: Color? = null,
    chipTextColor: Color? = null,
    chipBorderColor: Color? = null
) {
    val colors = LocalAppColors.current
    val spec = LocalThemeSpec.current

    if (chips.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { chip ->
            val bg = chipBgColor ?: colors.surface2
            val text = chipTextColor ?: colors.onSurface
            val border = chipBorderColor ?: colors.border

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(999.dp))
                    .clickable { onRemoveChip(chip) }
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chip,
                    color = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove $chip",
                    tint = colors.onSurfaceSub,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SecondaryKitchenToolbar(
    chipsCount: Int,
    onOpenLibrary: () -> Unit,
    onOpenKitchenScan: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenRecipeImport: () -> Unit,
    onOpenSetup: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarPill(
            icon = Icons.Filled.GridView,
            label = if (L.isTr) "Kütüphane" else "Library",
            onClick = onOpenLibrary
        )
        ToolbarPill(
            icon = Icons.Filled.PhotoCamera,
            label = if (L.isTr) "Mutfak tara" else "Scan kitchen",
            onClick = onOpenKitchenScan
        )
        ToolbarPill(
            icon = Icons.Filled.ReceiptLong,
            label = if (L.isTr) "Alışveriş" else "Shopping",
            onClick = onOpenShopping
        )
        ToolbarPill(
            icon = Icons.Filled.RestaurantMenu,
            label = if (L.isTr) "Tarif aktar" else "Import recipe",
            onClick = onOpenRecipeImport
        )
        ToolbarPill(
            icon = Icons.Filled.Tune,
            label = if (L.isTr) "Kurulum" else "Setup",
            onClick = onOpenSetup
        )
        if (chipsCount > 0) {
            ToolbarPill(
                icon = Icons.Filled.DeleteSweep,
                label = if (L.isTr) "Temizle" else "Clear",
                destructive = true,
                onClick = onClearAll
            )
        }
    }
}

@Composable
private fun ToolbarPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val colors = LocalAppColors.current
    val borderCol = if (destructive) colors.danger.copy(alpha = 0.5f) else colors.border
    val tintCol = if (destructive) colors.danger else colors.primary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface)
            .border(1.dp, borderCol, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintCol,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (destructive) colors.danger else colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
