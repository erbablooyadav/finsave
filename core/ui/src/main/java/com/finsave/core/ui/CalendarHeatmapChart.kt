package com.finsave.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun CalendarHeatmapChart(
    dailySpend: Map<LocalDate, Long>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = 14.dp
    val cellSpacing = 3.dp
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp
    )

    // Calculate dates
    val today = remember { LocalDate.now() }
    val daysCount = 84 // 12 weeks * 7 days
    val startDate = remember(today) { today.minusDays(daysCount - 1L) }
    
    // We want to align the grid such that each column is a week.
    // The top row (row 0) is Monday, bottom row (row 6) is Sunday.
    // However, the 84-day period ends exactly on 'today'.
    // If today is not Sunday, the last column will not be full.
    // To keep it simple and standard like GitHub, columns are standard weeks.
    // So if today is Wednesday (DayOfWeek.WEDNESDAY = 3), row 0,1,2 of the last column are filled.
    // The start date's day of week determines the first column's start.
    // Wait, 84 days is exactly 12 weeks, so if we just go back 83 days, it might not start on a Monday.
    // For a strict 12 column x 7 row grid, it's 84 cells.
    // We will map cell(col, row) to a date:
    // col in 0..11, row in 0..6
    // cellIndex = col * 7 + row
    // date = startDate.plusDays(cellIndex)

    val maxSpend = remember(dailySpend) {
        dailySpend.values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    // To ensure month labels are drawn correctly, we find the first occurrence of a month
    // in the first row (or roughly the start of the week).
    val monthLabels = remember(startDate) {
        val labels = mutableListOf<Pair<Int, String>>()
        var currentMonth: YearMonth? = null
        for (col in 0 until 12) {
            val colStartDate = startDate.plusDays((col * 7).toLong())
            val month = YearMonth.from(colStartDate)
            if (currentMonth != month) {
                currentMonth = month
                labels.add(col to month.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()))
            }
        }
        labels
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp) // Approximate height: labels + 7 rows + padding
            .padding(vertical = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(startDate) {
                    detectTapGestures { offset ->
                        // Calculate cell tap
                        // Cell area starts after day labels. Let's assume 24.dp for day labels.
                        val startX = 24.dp.toPx()
                        val startY = 20.dp.toPx() // Space for month labels
                        val cellTotal = cellSize.toPx() + cellSpacing.toPx()

                        if (offset.x >= startX && offset.y >= startY) {
                            val col = ((offset.x - startX) / cellTotal).toInt()
                            val row = ((offset.y - startY) / cellTotal).toInt()
                            if (col in 0..11 && row in 0..6) {
                                val clickedDate = startDate.plusDays((col * 7 + row).toLong())
                                onDayClick(clickedDate)
                            }
                        }
                    }
                }
        ) {
            val startX = 24.dp.toPx()
            val startY = 20.dp.toPx()
            val cellPx = cellSize.toPx()
            val spacingPx = cellSpacing.toPx()
            val cornerRadius = CornerRadius(2.dp.toPx())

            // 1. Draw Month Labels
            monthLabels.forEach { (col, text) ->
                val x = startX + col * (cellPx + spacingPx)
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    style = labelStyle,
                    topLeft = Offset(x, 0f)
                )
            }

            // 2. Draw Day Labels (Mon, Wed, Fri)
            val dayLabelIndices = listOf(1 to "M", 3 to "W", 5 to "F") // Rows 1, 3, 5 (0-indexed)
            dayLabelIndices.forEach { (row, text) ->
                val y = startY + row * (cellPx + spacingPx)
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    style = labelStyle,
                    topLeft = Offset(0f, y)
                )
            }

            // 3. Draw Grid
            for (col in 0 until 12) {
                for (row in 0 until 7) {
                    val date = startDate.plusDays((col * 7 + row).toLong())
                    val spend = dailySpend[date] ?: 0L

                    // Determine color intensity
                    val color = if (spend == 0L) {
                        surfaceVariant
                    } else {
                        val fraction = (spend.toFloat() / maxSpend).coerceIn(0f, 1f)
                        val alpha = when {
                            fraction <= 0.25f -> 0.2f
                            fraction <= 0.5f -> 0.4f
                            fraction <= 0.75f -> 0.7f
                            else -> 1.0f
                        }
                        // Blend primary with surfaceVariant based on alpha
                        Color(
                            red = primaryColor.red * alpha + surfaceVariant.red * (1 - alpha),
                            green = primaryColor.green * alpha + surfaceVariant.green * (1 - alpha),
                            blue = primaryColor.blue * alpha + surfaceVariant.blue * (1 - alpha),
                            alpha = 1f
                        )
                    }

                    val x = startX + col * (cellPx + spacingPx)
                    val y = startY + row * (cellPx + spacingPx)

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(cellPx, cellPx),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
    }
}
