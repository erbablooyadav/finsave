package com.finsave.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * FinSaveSnackbar — Custom styled SnackbarHost with consistent styling across the app.
 *
 * Features:
 * - Custom rounded shape (12dp)
 * - Uses inverseSurface/inversePrimary for dark-on-light contrast
 * - Material3 typography (bodyMedium)
 * - Supports undo actions via SnackbarResult.ActionPerformed
 */
@Composable
fun FinSaveSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
            actionContentColor = MaterialTheme.colorScheme.inversePrimary
        )
    }
}
