package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WhatsAppGreen

@Composable
fun WhatsAppAvatar(
    name: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    isGroup: Boolean = false,
    showOnlineBadge: Boolean = false,
    hasStatusRing: Boolean = false,
    statusViewed: Boolean = false,
    statusSegments: Int = 1
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color(0xFF008069)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Status Ring around avatar if applicable
        if (hasStatusRing) {
            val ringColor = if (statusViewed) Color(0xFF8696A0) else WhatsAppGreen
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidth = 2.5.dp.toPx()
                if (statusSegments <= 1) {
                    drawCircle(
                        color = ringColor,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    val gapDegrees = 8f
                    val sweepAngle = (360f - (gapDegrees * statusSegments)) / statusSegments
                    for (i in 0 until statusSegments) {
                        val startAngle = -90f + i * (sweepAngle + gapDegrees)
                        drawArc(
                            color = ringColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        // Inner Avatar Circle
        val avatarInnerPadding = if (hasStatusRing) 3.5.dp else 0.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(parsedColor),
            contentAlignment = Alignment.Center
        ) {
            if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = name,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            } else if (name.isBlank()) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = name,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.55f)
                )
            } else {
                val initials = name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first() }
                    .joinToString("")
                    .uppercase()

                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Online green indicator dot
        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(WhatsAppGreen)
                )
            }
        }
    }
}
