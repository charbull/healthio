package com.healthio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthio.ui.dashboard.TimerState

@Composable
fun FluxTimer(
    state: TimerState,
    elapsedMillis: Long,
    timeDisplay: String,
    modifier: Modifier = Modifier
) {
    val dayMillis = 24 * 60 * 60 * 1000L
    
    val fullDays = if (state == TimerState.FASTING) (elapsedMillis / dayMillis).toInt() else 0
    val remainderMillis = if (state == TimerState.FASTING) elapsedMillis % dayMillis else 0L
    val currentDayProgress = if (state == TimerState.FASTING) remainderMillis.toFloat() / dayMillis else 0f

    val animatedCurrentProgress by animateFloatAsState(
        targetValue = currentDayProgress,
        animationSpec = tween(durationMillis = 500),
        label = "CurrentRingProgress"
    )

    val primaryColor = if (state == TimerState.FASTING) Color(0xFF4CAF50) else Color(0xFFFF9800).copy(alpha = 0.5f)
    val trackColor = primaryColor.copy(alpha = 0.1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 40f
            val gap = 10f
            val baseRadius = (size.minDimension - strokeWidth) / 2
            
            val ringsToDraw = (fullDays + 1).coerceAtMost(5)
            
            for (i in 0 until ringsToDraw) {
                val radius = baseRadius - (i * (strokeWidth + gap))
                if (radius <= 0) break
                
                // Draw Track for this ring
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                if (state == TimerState.FASTING) {
                    if (i < fullDays) {
                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            style = Stroke(width = strokeWidth)
                        )
                    } else if (i == fullDays) {
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedCurrentProgress,
                            useCenter = false,
                            topLeft = center.minus(androidx.compose.ui.geometry.Offset(radius, radius)),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
        
        // Time Display Text
        Text(
            text = timeDisplay,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Light,
                fontFeatureSettings = "tnum",
                letterSpacing = (-1).sp
            )
        )
    }
}