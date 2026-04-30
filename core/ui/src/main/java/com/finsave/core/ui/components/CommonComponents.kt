package com.finsave.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.theme.CreditGreen
import com.finsave.core.ui.theme.CreditGreenDark
import com.finsave.core.ui.theme.CreditGreenLight
import com.finsave.core.ui.theme.DebitRed
import com.finsave.core.ui.theme.DebitRedDark
import com.finsave.core.ui.theme.DebitRedLight
import com.finsave.core.ui.theme.LocalSpacing

/**
 * FinSave Card — Elevated card with consistent styling.
 *
 * Uses Material 3 card with FinSave-specific elevation and shape.
 * Supports optional gradient background for premium cards.
 */
@Composable
fun FinSaveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 4.dp
            )
        ) {
            Box(modifier = Modifier.padding(spacing.medium)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(spacing.medium)) {
                content()
            }
        }
    }
}

/**
 * Amount Display — Renders INR amounts with color coding.
 *
 * - Debit amounts shown in red with "-" prefix
 * - Credit amounts shown in green with "+" prefix
 * - Uses Indian number formatting by default
 * - Includes TalkBack accessibility description
 */
@Composable
fun AmountDisplay(
    amountPaise: Long,
    isDebit: Boolean,
    modifier: Modifier = Modifier,
    useIndianSystem: Boolean = true,
    showPaise: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    val color by animateColorAsState(
        targetValue = when {
            isDebit && isDark -> DebitRedDark
            isDebit -> DebitRed
            isDark -> CreditGreenDark
            else -> CreditGreen
        },
        animationSpec = tween(300),
        label = "amountColor"
    )

    val prefix = if (isDebit) "- " else "+ "
    val formattedAmount = IndianNumberFormatter.format(
        paiseAmount = kotlin.math.abs(amountPaise),
        useIndianSystem = useIndianSystem,
        showPaise = showPaise,
        showSymbol = true
    )
    val displayText = "$prefix$formattedAmount"
    val accessibilityText = IndianNumberFormatter.formatForAccessibility(
        if (isDebit) -kotlin.math.abs(amountPaise) else kotlin.math.abs(amountPaise)
    )

    Text(
        text = displayText,
        style = style.copy(
            fontWeight = FontWeight.SemiBold,
            color = color
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics {
            contentDescription = if (isDebit) "Spent $accessibilityText" else "Received $accessibilityText"
        }
    )
}

/**
 * Category Chip — Emoji + label chip for expense categories.
 */
@Composable
fun CategoryChip(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryColor: Color = MaterialTheme.colorScheme.primary
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            categoryColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chipText"
    )

    val spacing = LocalSpacing.current

    androidx.compose.material3.SuggestionChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "$label category${if (isSelected) ", selected" else ""}"
        },
        shape = MaterialTheme.shapes.small
    )
}

/**
 * Transaction Type Indicator — Small colored dot showing debit/credit.
 */
@Composable
fun TransactionTypeIndicator(
    isDebit: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val bgColor = when {
        isDebit && isDark -> DebitRedDark
        isDebit -> DebitRed
        isDark -> CreditGreenDark
        else -> CreditGreen
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bgColor)
    )
}

/**
 * Gradient Header Card — Used for dashboard header with spend summary.
 */
@Composable
fun GradientHeaderCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    ),
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .padding(spacing.large)
    ) {
        content()
    }
}

// Helper: Get luminance of a Color for dark mode detection
private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}
