package com.finsave.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class ChartSegment(
    val value: Float,
    val color: Color
)

@Composable
fun CustomDonutChart(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 60f,
    centerContent: @Composable () -> Unit = {}
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    val proportions = if (total == 0f) segments.map { 1f / segments.size } else segments.map { it.value / total }
    val sweepAngles = proportions.map { it * 360f }
    
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - strokeWidth) / 2
            val rectSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            if (total == 0f) {
                // Empty state ring
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = rectSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f // Start from top

                for (i in segments.indices) {
                    val sweepAngle = sweepAngles[i] * animationProgress.value
                    
                    // Draw gap
                    if (sweepAngle > 0) {
                        drawArc(
                            color = segments[i].color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = rectSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    startAngle += sweepAngles[i]
                }
            }
        }
        centerContent()
    }
}
