package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChatBackgroundDark
import com.example.ui.theme.ChatBackgroundLight

@Composable
fun WhatsAppDoodleBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val baseBgColor = if (isDark) ChatBackgroundDark else ChatBackgroundLight
    val doodleTint = if (isDark) Color(0x12FFFFFF) else Color(0x14000000)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = baseBgColor)

            val stepX = 85.dp.toPx()
            val stepY = 85.dp.toPx()
            val cols = (size.width / stepX).toInt() + 1
            val rows = (size.height / stepY).toInt() + 1

            for (r in 0..rows) {
                for (c in 0..cols) {
                    val cx = c * stepX + if (r % 2 == 1) stepX / 2f else 0f
                    val cy = r * stepY
                    val doodleType = (r * 3 + c * 7) % 6

                    when (doodleType) {
                        0 -> drawDoodleHeart(cx, cy, doodleTint)
                        1 -> drawDoodleChatBubble(cx, cy, doodleTint)
                        2 -> drawDoodleStar(cx, cy, doodleTint)
                        3 -> drawDoodleCoffee(cx, cy, doodleTint)
                        4 -> drawDoodleMusicNote(cx, cy, doodleTint)
                        else -> drawDoodleSparkle(cx, cy, doodleTint)
                    }
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawDoodleHeart(cx: Float, cy: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + 4)
        cubicTo(cx - 8, cy - 8, cx - 14, cy + 2, cx, cy + 12)
        cubicTo(cx + 14, cy + 2, cx + 8, cy - 8, cx, cy + 4)
    }
    drawPath(path, color, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawDoodleChatBubble(cx: Float, cy: Float, color: Color) {
    drawOval(
        color = color,
        topLeft = Offset(cx - 8, cy - 6),
        size = Size(16f, 12f),
        style = Stroke(width = 1.2f)
    )
    val tail = Path().apply {
        moveTo(cx - 4, cy + 5)
        lineTo(cx - 7, cy + 9)
        lineTo(cx - 1, cy + 6)
    }
    drawPath(tail, color, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawDoodleStar(cx: Float, cy: Float, color: Color) {
    drawLine(color, Offset(cx - 6, cy), Offset(cx + 6, cy), strokeWidth = 1.2f)
    drawLine(color, Offset(cx, cy - 6), Offset(cx, cy + 6), strokeWidth = 1.2f)
    drawLine(color, Offset(cx - 4, cy - 4), Offset(cx + 4, cy + 4), strokeWidth = 1.0f)
    drawLine(color, Offset(cx - 4, cy + 4), Offset(cx + 4, cy - 4), strokeWidth = 1.0f)
}

private fun DrawScope.drawDoodleCoffee(cx: Float, cy: Float, color: Color) {
    val cup = Path().apply {
        moveTo(cx - 6, cy - 5)
        lineTo(cx + 6, cy - 5)
        lineTo(cx + 5, cy + 4)
        lineTo(cx - 5, cy + 4)
        close()
    }
    drawPath(cup, color, style = Stroke(width = 1.2f))
    drawArc(
        color = color,
        startAngle = -70f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx + 4, cy - 4),
        size = Size(5f, 6f),
        style = Stroke(width = 1.2f)
    )
}

private fun DrawScope.drawDoodleMusicNote(cx: Float, cy: Float, color: Color) {
    drawCircle(color, radius = 2.5f, center = Offset(cx - 3, cy + 4))
    drawCircle(color, radius = 2.5f, center = Offset(cx + 4, cy + 2))
    drawLine(color, Offset(cx - 1, cy + 4), Offset(cx - 1, cy - 5), strokeWidth = 1.2f)
    drawLine(color, Offset(cx + 6, cy + 2), Offset(cx + 6, cy - 7), strokeWidth = 1.2f)
    drawLine(color, Offset(cx - 1, cy - 5), Offset(cx + 6, cy - 7), strokeWidth = 1.5f)
}

private fun DrawScope.drawDoodleSparkle(cx: Float, cy: Float, color: Color) {
    drawCircle(color, radius = 2.5f, center = Offset(cx, cy), style = Stroke(width = 1.2f))
    drawLine(color, Offset(cx, cy - 5), Offset(cx, cy - 7), strokeWidth = 1.0f)
    drawLine(color, Offset(cx, cy + 5), Offset(cx, cy + 7), strokeWidth = 1.0f)
    drawLine(color, Offset(cx - 5, cy), Offset(cx - 7, cy), strokeWidth = 1.0f)
    drawLine(color, Offset(cx + 5, cy), Offset(cx + 7, cy), strokeWidth = 1.0f)
}
