package com.agentickitchen.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun EquipmentArtwork(
    equipmentId: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val tint = if (selected) colors.primary else colors.onSurfaceSub

    Canvas(modifier = modifier.size(32.dp)) {
        val stroke = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val smallStroke = Stroke(
            width = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val left = 4.dp.toPx()
        val top = 4.dp.toPx()
        val width = size.width - 8.dp.toPx()
        val height = size.height - 8.dp.toPx()
        val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        when (equipmentId) {
            "oven" -> {
                drawRoundRect(tint, Offset(left, top), Size(width, height), corner, style = stroke)
                drawLine(tint, Offset(left, 10.dp.toPx()), Offset(left + width, 10.dp.toPx()), strokeWidth = smallStroke.width)
                drawCircle(tint, 1.1.dp.toPx(), Offset(11.dp.toPx(), 7.dp.toPx()))
                drawCircle(tint, 1.1.dp.toPx(), Offset(16.dp.toPx(), 7.dp.toPx()))
                drawRoundRect(
                    tint,
                    Offset(8.dp.toPx(), 14.dp.toPx()),
                    Size(16.dp.toPx(), 10.dp.toPx()),
                    corner,
                    style = smallStroke
                )
            }

            "elec" -> {
                drawRoundRect(tint, Offset(left, 6.dp.toPx()), Size(width, 20.dp.toPx()), corner, style = stroke)
                drawCircle(tint, 6.dp.toPx(), Offset(16.dp.toPx(), 16.dp.toPx()), style = stroke)
                drawCircle(tint, 2.5.dp.toPx(), Offset(16.dp.toPx(), 16.dp.toPx()), style = smallStroke)
            }

            "gas" -> {
                drawCircle(tint, 5.dp.toPx(), Offset(16.dp.toPx(), 17.dp.toPx()), style = stroke)
                drawLine(tint, Offset(7.dp.toPx(), 17.dp.toPx()), Offset(25.dp.toPx(), 17.dp.toPx()), strokeWidth = smallStroke.width)
                drawLine(tint, Offset(16.dp.toPx(), 8.dp.toPx()), Offset(16.dp.toPx(), 26.dp.toPx()), strokeWidth = smallStroke.width)
                drawPath(
                    Path().apply {
                        moveTo(16.dp.toPx(), 5.dp.toPx())
                        lineTo(13.5.dp.toPx(), 10.dp.toPx())
                        lineTo(16.dp.toPx(), 12.dp.toPx())
                        lineTo(18.5.dp.toPx(), 10.dp.toPx())
                        close()
                    },
                    tint,
                    style = stroke
                )
            }

            "grill" -> {
                drawRoundRect(tint, Offset(6.dp.toPx(), 10.dp.toPx()), Size(20.dp.toPx(), 10.dp.toPx()), corner, style = stroke)
                listOf(10.dp, 14.dp, 18.dp, 22.dp).forEach { x ->
                    drawLine(tint, Offset(x.toPx(), 11.dp.toPx()), Offset(x.toPx(), 19.dp.toPx()), strokeWidth = smallStroke.width)
                }
                drawLine(tint, Offset(10.dp.toPx(), 20.dp.toPx()), Offset(8.dp.toPx(), 26.dp.toPx()), strokeWidth = smallStroke.width)
                drawLine(tint, Offset(22.dp.toPx(), 20.dp.toPx()), Offset(24.dp.toPx(), 26.dp.toPx()), strokeWidth = smallStroke.width)
            }

            "camping" -> {
                drawRoundRect(tint, Offset(10.dp.toPx(), 15.dp.toPx()), Size(12.dp.toPx(), 11.dp.toPx()), corner, style = stroke)
                drawCircle(tint, 4.dp.toPx(), Offset(16.dp.toPx(), 11.dp.toPx()), style = stroke)
                drawLine(tint, Offset(8.dp.toPx(), 11.dp.toPx()), Offset(24.dp.toPx(), 11.dp.toPx()), strokeWidth = smallStroke.width)
                drawLine(tint, Offset(10.dp.toPx(), 8.dp.toPx()), Offset(22.dp.toPx(), 8.dp.toPx()), strokeWidth = smallStroke.width)
            }

            "airfryer" -> {
                drawRoundRect(tint, Offset(7.dp.toPx(), 5.dp.toPx()), Size(18.dp.toPx(), 22.dp.toPx()), corner, style = stroke)
                drawCircle(tint, 2.dp.toPx(), Offset(16.dp.toPx(), 10.dp.toPx()), style = smallStroke)
                drawLine(tint, Offset(11.dp.toPx(), 18.dp.toPx()), Offset(21.dp.toPx(), 18.dp.toPx()), strokeWidth = smallStroke.width)
                drawLine(tint, Offset(13.dp.toPx(), 22.dp.toPx()), Offset(19.dp.toPx(), 22.dp.toPx()), strokeWidth = stroke.width)
            }

            "microwave" -> {
                drawRoundRect(tint, Offset(4.dp.toPx(), 7.dp.toPx()), Size(24.dp.toPx(), 18.dp.toPx()), corner, style = stroke)
                drawRoundRect(tint, Offset(7.dp.toPx(), 10.dp.toPx()), Size(12.dp.toPx(), 12.dp.toPx()), corner, style = smallStroke)
                drawCircle(tint, 1.dp.toPx(), Offset(23.dp.toPx(), 12.dp.toPx()))
                drawCircle(tint, 1.dp.toPx(), Offset(23.dp.toPx(), 17.dp.toPx()))
                drawCircle(tint, 1.dp.toPx(), Offset(23.dp.toPx(), 22.dp.toPx()))
            }

            "pan" -> {
                drawCircle(tint, 8.dp.toPx(), Offset(13.dp.toPx(), 16.dp.toPx()), style = stroke)
                drawLine(tint, Offset(19.dp.toPx(), 12.dp.toPx()), Offset(27.dp.toPx(), 7.dp.toPx()), strokeWidth = stroke.width)
                drawLine(tint, Offset(20.dp.toPx(), 15.dp.toPx()), Offset(28.dp.toPx(), 10.dp.toPx()), strokeWidth = smallStroke.width)
            }

            else -> drawCircle(tint, 8.dp.toPx(), Offset(16.dp.toPx(), 16.dp.toPx()), style = stroke)
        }
    }
}
