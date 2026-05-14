package com.finsave.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finsave.core.ui.theme.LocalSpacing

/**
 * EmptyStateView — Illustrated empty state with optional CTA button.
 *
 * Reused across Dashboard, Transactions, Budget, and Splitter screens.
 * All colors are MaterialTheme tokens — fully dark-mode safe.
 *
 * @param vectorRes Drawable resource ID for the illustration
 * @param title     Primary empty state headline
 * @param body      Secondary supporting text
 * @param ctaText   Optional CTA button label (null = no button shown)
 * @param onCtaClick Optional CTA action (ignored if ctaText is null)
 */
@Composable
fun EmptyStateView(
    @DrawableRes vectorRes: Int,
    title: String,
    body: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = vectorRes),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(spacing.large))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(spacing.large))
            FilledTonalButton(onClick = onCtaClick) {
                Text(text = ctaText)
            }
        }
    }
}
