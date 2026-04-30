package com.finsave.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * FinSave Shape System
 *
 * Consistent corner radii across the entire app.
 * Follows Material 3 shape scale with FinSave customizations.
 */
val FinSaveShapes = Shapes(
    // Chips, small buttons, tags
    extraSmall = RoundedCornerShape(4.dp),

    // Text fields, list items
    small = RoundedCornerShape(8.dp),

    // Cards, dialogs, bottom sheets
    medium = RoundedCornerShape(12.dp),

    // FAB, large cards, budget rings container
    large = RoundedCornerShape(16.dp),

    // Full-screen sheets, onboarding cards
    extraLarge = RoundedCornerShape(24.dp)
)
