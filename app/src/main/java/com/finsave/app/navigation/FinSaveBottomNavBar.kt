package com.finsave.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finsave.core.common.Constants.Routes

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : BottomNavItem(Routes.DASHBOARD, Icons.Default.Home, "Home")
    object Transactions : BottomNavItem(Routes.TRANSACTIONS, Icons.Default.Receipt, "Transactions")
    object Insights : BottomNavItem(Routes.INSIGHTS, Icons.Default.PieChart, "Insights")
    object Budget : BottomNavItem(Routes.BUDGET, Icons.Default.AccountBalanceWallet, "Budget")
    object Splitter : BottomNavItem(Routes.SPLITTER, Icons.Default.Group, "Splitter")
    object Settings : BottomNavItem(Routes.SETTINGS, Icons.Default.Settings, "Settings")
}

@Composable
fun FinSaveBottomNavBar(
    navController: NavController,
    unseenSmsCount: Int = 0,
    onTransactionsTabSelected: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Transactions,
        BottomNavItem.Insights,
        BottomNavItem.Budget,
        BottomNavItem.Splitter,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar for these main top-level routes
    val showBottomBar = items.any { it.route == currentDestination?.route }

    if (showBottomBar) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                val showBadge = item is BottomNavItem.Transactions && unseenSmsCount > 0

                NavigationBarItem(
                    icon = {
                        if (showBadge) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        val badgeText = if (unseenSmsCount > 99) "99+" else unseenSmsCount.toString()
                                        Text(
                                            text = badgeText,
                                            modifier = androidx.compose.ui.Modifier.semantics {
                                                contentDescription = "$unseenSmsCount new transactions imported"
                                            }
                                        )
                                    }
                                }
                            ) {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        } else {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    },
                    label = { Text(item.label) },
                    selected = isSelected,
                    onClick = {
                        if (item is BottomNavItem.Transactions) {
                            onTransactionsTabSelected()
                        }
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
