package com.agentickitchen.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EditorialNavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

@Composable fun EditorialBottomBar(items: List<EditorialNavItem>) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth().height(64.dp).background(colors.surface).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { item ->
            val indicator = animateFloatAsState(if (item.selected) 1f else 0f, label = "navIndicator")
            Column(Modifier.weight(1f).clickable(onClick = item.onClick).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(item.icon, item.label, Modifier.size(18.dp), tint = if (item.selected) colors.onSurface else colors.onSurfaceSub)
                Text(item.label, color = if (item.selected) colors.onSurface else colors.onSurfaceSub, fontSize = 10.sp)
                androidx.compose.foundation.layout.Spacer(Modifier.height(3.dp))
                androidx.compose.foundation.layout.Box(Modifier.size(width = 16.dp, height = 2.dp).alpha(indicator.value).background(colors.primary))
            }
        }
    }
}
