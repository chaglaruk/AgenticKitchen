package com.agentickitchen.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentickitchen.android.L
import kotlinx.coroutines.delay
import java.util.Locale

internal enum class IngredientVisualKind(val labelTr: String, val labelEn: String) {
    TOMATO("Sebze", "Vegetable"), PEPPER("Biber", "Pepper"), CUCUMBER("Salatalık", "Cucumber"), ROOT_VEGETABLE("Kök sebze", "Root vegetable"), POTATO("Patates", "Potato"), MUSHROOM("Mantar", "Mushroom"),
    LEAFY("Yeşillik", "Greens"), ONION("Soğan", "Onion"), GARLIC("Sarımsak", "Garlic"),
    CHICKEN("Tavuk", "Chicken"), RED_MEAT("Et", "Red meat"), FISH("Balık", "Fish"), SEAFOOD("Deniz ürünü", "Seafood"),
    EGG("Yumurta", "Egg"), MILK_CREAM("Süt ürünü", "Milk or cream"), YOGHURT("Yoğurt", "Yoghurt"), CHEESE("Peynir", "Cheese"), BUTTER("Tereyağı", "Butter"),
    MEAT("Protein", "Protein"), EGG_DAIRY("Süt ürünü", "Dairy"), PASTA_GRAINS("Tahıl", "Grain"),
    PASTA("Makarna", "Pasta"), RICE("Pirinç", "Rice"), BREAD("Ekmek", "Bread"), LEGUMES("Bakliyat", "Legumes"), FLOUR_BAKING("Unlu mamul", "Flour and baking"), HERBS("Otlar", "Herbs"), SPICES("Baharat", "Spices"), OIL("Yağ", "Oil"), SAUCE("Sos", "Sauce"), FRUIT("Meyve", "Fruit"), CITRUS("Narenciye", "Citrus"), NUTS_SEEDS("Kuruyemiş", "Nuts and seeds"), SUGAR_HONEY("Tatlandırıcı", "Sugar and honey"),
    PANTRY("Kiler", "Pantry")
}

private fun ingredientVisualFor(name: String): IngredientVisualKind {
    val value = name.lowercase(Locale.ROOT)
        .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g').replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
    fun matches(vararg terms: String) = terms.any(value::contains)
    return when {
        matches("domates", "tomato", "biber", "pepper", "havuç", "carrot", "patlican", "eggplant", "kabak", "zucchini") -> IngredientVisualKind.TOMATO
        matches("ispanak", "spinach", "marul", "lettuce", "roka", "arugula", "brokoli", "broccoli") -> IngredientVisualKind.LEAFY
        matches("sogan", "onion", "sarimsak", "garlic") -> IngredientVisualKind.ONION
        matches("tavuk", "chicken", "et", "beef", "meat", "kofte", "lamb") -> IngredientVisualKind.MEAT
        matches("balik", "fish", "somon", "salmon", "ton", "tuna") -> IngredientVisualKind.FISH
        matches("yumurta", "egg", "sut", "milk", "yogurt", "yoğurt", "cream", "krema", "tereyag", "butter") -> IngredientVisualKind.EGG_DAIRY
        matches("pirinc", "rice") -> IngredientVisualKind.RICE
        matches("makarna", "pasta", "bulgur", "yulaf", "oat", "un", "flour", "grain") -> IngredientVisualKind.PASTA_GRAINS
        matches("feslegen", "basil", "maydanoz", "parsley", "nane", "mint", "kekik", "thyme", "dill") -> IngredientVisualKind.HERBS
        matches("peynir", "cheese", "kaşar", "kasar", "feta") -> IngredientVisualKind.CHEESE
        matches("ekmek", "bread", "baget", "baguette", "toast") -> IngredientVisualKind.BREAD
        else -> IngredientVisualKind.PANTRY
    }
}

@Composable
internal fun IngredientArtwork(name: String, modifier: Modifier = Modifier) {
    val kind = ingredientVisualFor(name)
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val left = (size.width - s) / 2f
        val top = (size.height - s) / 2f
        fun point(x: Float, y: Float) = Offset(left + s * x, top + s * y)
        fun oval(color: Color, x: Float, y: Float, width: Float, height: Float) =
            drawOval(color, topLeft = point(x, y), size = Size(s * width, s * height))

        oval(Color(0x1F9A8977), .20f, .78f, .60f, .10f)
        when (kind) {
            IngredientVisualKind.TOMATO, IngredientVisualKind.PEPPER, IngredientVisualKind.CUCUMBER, IngredientVisualKind.ROOT_VEGETABLE, IngredientVisualKind.POTATO, IngredientVisualKind.MUSHROOM -> {
                drawCircle(Color(0xFFB7644C), s * .30f, point(.50f, .50f))
                drawCircle(Color(0xFFD98567), s * .10f, point(.41f, .40f))
                drawLine(Color(0xFF74806B), point(.50f, .24f), point(.52f, .37f), strokeWidth = s * .06f)
                drawLine(Color(0xFF74806B), point(.50f, .33f), point(.34f, .28f), strokeWidth = s * .05f)
                drawLine(Color(0xFF74806B), point(.50f, .33f), point(.66f, .28f), strokeWidth = s * .05f)
            }
            IngredientVisualKind.LEAFY -> {
                drawCircle(Color(0xFF74806B), s * .22f, point(.40f, .52f))
                drawCircle(Color(0xFF93A085), s * .23f, point(.58f, .45f))
                drawCircle(Color(0xFFC5CCBE), s * .18f, point(.53f, .63f))
                drawLine(Color(0xFF5E6B58), point(.47f, .72f), point(.54f, .30f), strokeWidth = s * .035f)
            }
            IngredientVisualKind.ONION, IngredientVisualKind.GARLIC -> {
                oval(Color(0xFFE4D2BF), .28f, .28f, .44f, .46f)
                drawCircle(Color(0xFFF6E8D9), s * .14f, point(.45f, .43f))
                drawLine(Color(0xFF74806B), point(.50f, .28f), point(.56f, .15f), strokeWidth = s * .04f)
            }
            IngredientVisualKind.MEAT, IngredientVisualKind.CHICKEN, IngredientVisualKind.RED_MEAT -> {
                drawRoundRect(Color(0xFFD79A82), point(.24f, .30f), Size(s * .52f, s * .40f), CornerRadius(s * .20f, s * .20f))
                drawCircle(Color(0xFFF0C4A5), s * .08f, point(.57f, .46f))
                drawCircle(Color(0xFFF0C4A5), s * .05f, point(.39f, .57f))
            }
            IngredientVisualKind.FISH, IngredientVisualKind.SEAFOOD -> {
                oval(Color(0xFF8EA39D), .23f, .35f, .48f, .30f)
                val tail = Path().apply {
                    moveTo(point(.70f, .50f).x, point(.70f, .50f).y)
                    lineTo(point(.88f, .31f).x, point(.88f, .31f).y)
                    lineTo(point(.88f, .69f).x, point(.88f, .69f).y)
                    close()
                }
                drawPath(tail, Color(0xFF74806B))
                drawCircle(Color(0xFF191714), s * .025f, point(.37f, .46f))
            }
            IngredientVisualKind.EGG_DAIRY, IngredientVisualKind.EGG, IngredientVisualKind.MILK_CREAM, IngredientVisualKind.YOGHURT, IngredientVisualKind.BUTTER -> {
                oval(Color(0xFFF8F1E5), .30f, .23f, .40f, .52f)
                drawCircle(Color(0xFFD7A18E), s * .11f, point(.50f, .53f))
                drawOval(Color(0xFFD8D0C5), topLeft = point(.30f, .23f), size = Size(s * .40f, s * .52f), style = Stroke(s * .015f))
            }
            IngredientVisualKind.PASTA_GRAINS, IngredientVisualKind.PASTA, IngredientVisualKind.LEGUMES, IngredientVisualKind.FLOUR_BAKING -> {
                repeat(4) { index ->
                    drawRoundRect(Color(0xFFD9B76E), point(.28f + index * .10f, .30f + index % 2 * .08f), Size(s * .25f, s * .13f), CornerRadius(s * .06f, s * .06f))
                }
            }
            IngredientVisualKind.RICE -> {
                repeat(9) { index ->
                    val x = .30f + (index % 3) * .14f
                    val y = .32f + (index / 3) * .14f
                    oval(Color(0xFFF0E5CC), x, y, .16f, .08f)
                }
            }
            IngredientVisualKind.HERBS, IngredientVisualKind.SPICES -> {
                drawLine(Color(0xFF5E6B58), point(.48f, .73f), point(.55f, .25f), strokeWidth = s * .035f)
                repeat(4) { index ->
                    val y = .32f + index * .10f
                    oval(Color(0xFF74806B), .34f + (index % 2) * .12f, y, .20f, .12f)
                }
            }
            IngredientVisualKind.CHEESE -> {
                val wedge = Path().apply {
                    moveTo(point(.25f, .70f).x, point(.25f, .70f).y)
                    lineTo(point(.75f, .70f).x, point(.75f, .70f).y)
                    lineTo(point(.68f, .27f).x, point(.68f, .27f).y)
                    close()
                }
                drawPath(wedge, Color(0xFFE4BE62))
                drawCircle(Color(0xFFC7983D), s * .045f, point(.57f, .55f))
                drawCircle(Color(0xFFC7983D), s * .032f, point(.48f, .64f))
            }
            IngredientVisualKind.BREAD -> {
                drawRoundRect(Color(0xFFC98A55), point(.25f, .37f), Size(s * .50f, s * .34f), CornerRadius(s * .18f, s * .18f))
                drawLine(Color(0xFFF0C18B), point(.40f, .43f), point(.36f, .60f), strokeWidth = s * .035f)
                drawLine(Color(0xFFF0C18B), point(.56f, .43f), point(.52f, .60f), strokeWidth = s * .035f)
            }
            IngredientVisualKind.PANTRY, IngredientVisualKind.OIL, IngredientVisualKind.SAUCE, IngredientVisualKind.FRUIT, IngredientVisualKind.CITRUS, IngredientVisualKind.NUTS_SEEDS, IngredientVisualKind.SUGAR_HONEY -> {
                drawRoundRect(Color(0xFFD8D0C5), point(.31f, .28f), Size(s * .38f, s * .46f), CornerRadius(s * .08f, s * .08f))
                drawRoundRect(Color(0xFFB7644C), point(.34f, .42f), Size(s * .32f, s * .18f), CornerRadius(s * .04f, s * .04f))
                drawLine(Color(0xFF5F5951), point(.34f, .24f), point(.66f, .24f), strokeWidth = s * .05f)
            }
        }
    }
}

private data class DisplayIngredient(val name: String, val exiting: Boolean = false)

@Composable
fun EditorialIngredientCollection(chips: List<String>, onRemove: (String) -> Unit) {
    var displayed by remember { mutableStateOf(chips.map(::DisplayIngredient)) }
    LaunchedEffect(chips) {
        val desired = chips.toSet()
        val updated = displayed.map { item -> item.copy(exiting = item.name !in desired) }
        val additions = chips.filterNot { name -> updated.any { it.name == name } }.map(::DisplayIngredient)
        displayed = updated + additions
        if (updated.any { it.exiting }) {
            delay(220)
            displayed = displayed.filterNot { it.exiting }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).animateContentSize(tween(260)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayed.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().animateContentSize(tween(260)),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEachIndexed { columnIndex, item ->
                    key(item.name) {
                        IngredientCollectionItem(
                            item = item,
                            entranceDelay = (rowIndex * 2 + columnIndex) * 40,
                            onRemove = { onRemove(item.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (displayed.isEmpty() && chips.isEmpty()) EmptyIngredientCollection()
    }
}

@Composable
private fun IngredientCollectionItem(
    item: DisplayIngredient,
    entranceDelay: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val visual = ingredientVisualFor(item.name)
    val state = remember { MutableTransitionState(false) }.apply { targetState = !item.exiting }

    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(260, entranceDelay)) + scaleIn(tween(260, entranceDelay), initialScale = .94f) + slideInVertically(tween(260, entranceDelay)) { -it / 8 },
        exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = .94f)
    ) {
        Column(modifier = modifier.animateContentSize(tween(220)).height(88.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                IngredientArtwork(item.name, Modifier.fillMaxWidth().height(46.dp).align(Alignment.Center))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (L.isTr) "${item.name} malzemesini kaldır" else "Remove ${item.name}",
                            tint = colors.onSurfaceSub,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Text(item.name, color = colors.onSurface, style = MaterialTheme.typography.subtitle1, maxLines = 2)
            Spacer(Modifier.height(2.dp))
            Text(if (L.isTr) visual.labelTr else visual.labelEn, color = colors.onSurfaceSub, fontSize = 11.sp)
        }
    }
}

@Composable
fun EmptyIngredientCollection() {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IngredientArtwork("", Modifier.size(64.dp))
        Text(
            if (L.isTr) "İlk malzemeni ekle." else "Add your first ingredient.",
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}
