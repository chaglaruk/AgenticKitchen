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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun EditorialBrandMark(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    val colors = LocalAppColors.current
    Canvas(modifier = modifier.size(size).clearAndSetSemantics { }) {
        val stroke = Stroke(width = this.size.minDimension * .08f)
        drawOval(colors.primary, Offset(this.size.width * .16f, this.size.height * .46f), androidx.compose.ui.geometry.Size(this.size.width * .68f, this.size.height * .30f), style = stroke)
        drawLine(colors.primary, Offset(this.size.width * .20f, this.size.height * .60f), Offset(this.size.width * .80f, this.size.height * .60f), strokeWidth = this.size.width * .08f)
        drawLine(colors.accent, Offset(this.size.width * .37f, this.size.height * .36f), Offset(this.size.width * .34f, this.size.height * .14f), strokeWidth = this.size.width * .06f)
        drawLine(colors.accent, Offset(this.size.width * .61f, this.size.height * .36f), Offset(this.size.width * .65f, this.size.height * .14f), strokeWidth = this.size.width * .06f)
    }
}

@Composable
fun EditorialBrandLockup(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        EditorialBrandMark(size = 29.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.width(9.dp))
        Text(
            "Agentic Kitchen",
            color = colors.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = .1.sp
        )
    }
}
