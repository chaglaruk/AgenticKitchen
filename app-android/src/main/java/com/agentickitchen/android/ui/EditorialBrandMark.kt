package com.agentickitchen.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
fun EditorialBrandLockup(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(22.dp).clearAndSetSemantics { }) {
            val stroke = Stroke(width = size.minDimension * .08f)
            drawOval(colors.primary, Offset(size.width * .16f, size.height * .46f), androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .30f), style = stroke)
            drawLine(colors.primary, Offset(size.width * .20f, size.height * .60f), Offset(size.width * .80f, size.height * .60f), strokeWidth = size.width * .08f)
            drawLine(colors.accent, Offset(size.width * .37f, size.height * .36f), Offset(size.width * .34f, size.height * .14f), strokeWidth = size.width * .06f)
            drawLine(colors.accent, Offset(size.width * .61f, size.height * .36f), Offset(size.width * .65f, size.height * .14f), strokeWidth = size.width * .06f)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(7.dp))
        Text("Agentic Kitchen", color = colors.onSurfaceSub, style = MaterialTheme.typography.caption)
    }
}
