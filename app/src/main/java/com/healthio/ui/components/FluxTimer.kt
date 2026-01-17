package com.healthio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.healthio.ui.dashboard.TimerState

@Composable
fun FluxTimer(
    state: TimerState,
    elapsedMillis: Long,
    modifier: Modifier = Modifier
) {
    val dayMillis = 24 * 60 * 60 * 1000L
    
    // If not fasting, show 0 or full empty ring
    if (state != TimerState.FASTING) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(280.dp)) {
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    style = Stroke(width = 40f)
                )
            }
        }
        return
    }

    val fullDays = (elapsedMillis / dayMillis).toInt()
    val remainderMillis = elapsedMillis % dayMillis
    val currentDayProgress = remainderMillis.toFloat() / dayMillis

    val animatedCurrentProgress by animateFloatAsState(
        targetValue = currentDayProgress,
        animationSpec = tween(durationMillis = 500),
        label = "CurrentRingProgress"
    )

    val primaryColor = Color(0xFF4CAF50)
    val trackColor = primaryColor.copy(alpha = 0.1f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 40f
            val gap = 10f
            val baseRadius = (size.minDimension - strokeWidth) / 2
            
            // Draw Rings
            // We draw from Outer (Day 1) to Inner
            // Limit to max 3 rings for aesthetics, or dynamic? Let's support dynamic but it gets small fast.
            
            // Logic:
            // Loop for each completed day + 1 for current
            // Limit loop to avoid infinite small rings? Let's cap at 5.
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

                // If this is a completed day (i < fullDays), it's full.
                // If it's the current day (i == fullDays), it's partial.
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
}