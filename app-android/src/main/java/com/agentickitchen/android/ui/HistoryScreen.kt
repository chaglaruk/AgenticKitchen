package com.agentickitchen.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentickitchen.android.L
import com.agentickitchen.android.canonicalIngredientName
import com.agentickitchen.android.catalogIngredientForName
import com.agentickitchen.shared.db.RecipeHistory
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HistoryScreen(history: List<RecipeHistory>, onReuseIngredients: (List<String>) -> Unit = {}) {
    val colors = LocalAppColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EditorialBrandLockup()
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (L.isTr) "Pişirme geçmişi" else "Cooking history",
                color = colors.onSurface,
                style = MaterialTheme.typography.h1
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (L.isTr) {
                    "Başlattığın tarifler bu telefonda saklanır."
                } else {
                    "Recipes you start are stored on this phone."
                },
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.body1
            )
            Spacer(Modifier.height(18.dp))
            Divider(color = colors.divider)
        }

        if (history.isEmpty()) {
            item { EmptyHistoryState() }
        } else {
            items(history, key = RecipeHistory::id) { entry ->
                HistoryEntryCard(entry, onReuseIngredients)
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp)) {
        Text(
            text = if (L.isTr) "Henüz geçmiş yok" else "No history yet",
            color = colors.onSurface,
            style = MaterialTheme.typography.h5
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (L.isTr) {
                "Bir tarif planı başlattığında burada görünecek."
            } else {
                "A recipe will appear here after you start its plan."
            },
            color = colors.onSurfaceSub,
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun HistoryEntryCard(entry: RecipeHistory, onReuseIngredients: (List<String>) -> Unit) {
    val colors = LocalAppColors.current
    val status = historyStatusLabel(entry.status)
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val reusableIngredients = historyIngredientsForReuse(entry.ingredients)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .semantics {
                contentDescription = "${entry.name}, $status, ${historyDateLabel(entry.timestamp)}"
            },
        shape = RoundedCornerShape(14.dp),
        backgroundColor = colors.surface,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = normalizeLegacyRecipeName(entry.name, L.isTr),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    color = colors.primary,
                    style = MaterialTheme.typography.overline,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = historyDateLabel(entry.timestamp),
                color = colors.onSurfaceSub,
                style = MaterialTheme.typography.caption
            )
            if (expanded && entry.ingredients.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = colors.divider)
                Spacer(Modifier.height(12.dp))
                Text(if (L.isTr) "Malzemeler" else "Ingredients", color = colors.onSurface, style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = localizeHistoryIngredients(entry.ingredients, L.isTr),
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2,
                    maxLines = Int.MAX_VALUE
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onReuseIngredients(reusableIngredients) },
                    enabled = reusableIngredients.isNotEmpty()
                ) {
                    Text(
                        if (L.isTr) "Bu malzemelerle yeniden bak" else "Use these ingredients again",
                        color = colors.primary
                    )
                }
            } else if (!expanded && entry.ingredients.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(if (L.isTr) "Ayrıntılar için dokun" else "Tap for details", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
            }
        }
    }
}

internal fun historyStatusLabel(status: String): String = when (status.lowercase(Locale.US)) {
    "completed" -> if (L.isTr) "Tamamlandı" else "Completed"
    "cancelled", "canceled" -> if (L.isTr) "İptal edildi" else "Cancelled"
    "ended" -> if (L.isTr) "Sonlandırıldı" else "Ended"
    else -> if (L.isTr) "Başlatıldı" else "Started"
}

internal fun historyDateLabel(timestamp: String): String = runCatching {
    OffsetDateTime.parse(timestamp).format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(if (L.isTr) Locale.forLanguageTag("tr-TR") else Locale.UK)
    )
}.getOrElse { timestamp }

internal fun normalizeLegacyRecipeName(name: String, isTurkish: Boolean): String {
    val patterns = listOf(
        Regex("""^(.+?) ve (.+?) Tavası$""", RegexOption.IGNORE_CASE),
        Regex("""^(.+?) and (.+?) Sauté$""", RegexOption.IGNORE_CASE)
    )
    val match = patterns.firstNotNullOfOrNull { it.matchEntire(name.trim()) } ?: return name
    val first = match.groupValues[1].trim()
    val second = match.groupValues[2].trim()
    if (catalogIngredientForName(first) == null || catalogIngredientForName(second) == null) return name
    val firstLocalized = canonicalIngredientName(first, isTurkish)
    val secondLocalized = canonicalIngredientName(second, isTurkish)
    return if (isTurkish) "$firstLocalized ve $secondLocalized Tavası" else "$firstLocalized and $secondLocalized Sauté"
}

internal fun localizeHistoryIngredients(value: String, isTurkish: Boolean): String = value.split(',')
    .joinToString(", ") { rawItem ->
        val item = rawItem.trim()
        val match = HISTORY_INGREDIENT_LINE.matchEntire(item) ?: return@joinToString item
        val quantity = match.groupValues[1]
        val unit = localizedHistoryUnit(match.groupValues[2], isTurkish)
        val name = match.groupValues[3].trim()
        "$quantity $unit $name"
    }

private fun localizedHistoryUnit(unit: String, isTurkish: Boolean): String {
    val normalized = unit.trim().lowercase(Locale.ROOT).removeSuffix(".")
    return if (isTurkish) {
        when (normalized) {
            "count", "adet", "piece", "pieces", "pcs" -> "adet"
            "clove", "cloves", "diş", "dis" -> "diş"
            "slice", "slices", "dilim" -> "dilim"
            "pinch", "pinches", "tutam" -> "tutam"
            "package", "packages", "pack", "packs", "paket" -> "paket"
            "bunch", "bunches", "demet" -> "demet"
            "tsp", "teaspoon", "teaspoons", "çay kaşığı", "cay kasigi" -> "çay kaşığı"
            "tbsp", "tablespoon", "tablespoons", "yemek kaşığı", "yemek kasigi" -> "yemek kaşığı"
            "cup", "cups", "bardak", "su bardağı", "su bardagi" -> "su bardağı"
            "unit", "units", "birim" -> "birim"
            else -> unit.trim()
        }
    } else {
        when (normalized) {
            "count", "adet", "piece", "pieces", "pcs" -> "piece"
            "clove", "cloves", "diş", "dis" -> "clove"
            "slice", "slices", "dilim" -> "slice"
            "pinch", "pinches", "tutam" -> "pinch"
            "package", "packages", "pack", "packs", "paket" -> "package"
            "bunch", "bunches", "demet" -> "bunch"
            "tsp", "teaspoon", "teaspoons", "çay kaşığı", "cay kasigi" -> "tsp"
            "tbsp", "tablespoon", "tablespoons", "yemek kaşığı", "yemek kasigi" -> "tbsp"
            "cup", "cups", "bardak", "su bardağı", "su bardagi" -> "cup"
            "unit", "units", "birim" -> "unit"
            else -> unit.trim()
        }
    }
}

internal fun historyIngredientsForReuse(value: String): List<String> = value.split(',')
    .map { item ->
        item.trim().replace(
            Regex("""^\d+(?:[.,]\d+)?\s+(?:g|kg|ml|l|tsp|tbsp|cup|piece|pieces|pcs|count|slice|slices|clove|cloves|pinch|pinches|unit|units|package|packages|pack|packs|bunch|bunches|adet|diş|dis|dilim|tutam|paket|demet)\s+""", RegexOption.IGNORE_CASE),
            ""
        ).trim()
    }
    .filter(String::isNotBlank)

private val HISTORY_INGREDIENT_LINE = Regex("""^(\d+(?:[.,]\d+)?)\s+(\S+)\s+(.+)$""")
