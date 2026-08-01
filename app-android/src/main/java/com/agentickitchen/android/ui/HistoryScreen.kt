package com.agentickitchen.android.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentickitchen.android.L
import com.agentickitchen.shared.db.RecipeHistory
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HistoryScreen(history: List<RecipeHistory>) {
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
                HistoryEntryCard(entry)
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
private fun HistoryEntryCard(entry: RecipeHistory) {
    val colors = LocalAppColors.current
    val status = historyStatusLabel(entry.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                    text = entry.name,
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
            if (entry.ingredients.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = colors.divider)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = entry.ingredients,
                    color = colors.onSurfaceSub,
                    style = MaterialTheme.typography.body2,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
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
