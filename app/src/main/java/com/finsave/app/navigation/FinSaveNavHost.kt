package com.finsave.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.Constants.Routes
import com.finsave.feature.accounts.AccountScreen
import com.finsave.feature.budget.BudgetScreen
import com.finsave.feature.categories.CategoryScreen
import com.finsave.feature.dashboard.DashboardScreen
import com.finsave.feature.insights.InsightsScreen
import com.finsave.feature.onboarding.OnboardingScreen
import com.finsave.feature.settings.SettingsScreen
import com.finsave.feature.splitter.GroupDetailScreen
import com.finsave.feature.splitter.SplitterScreen
import com.finsave.feature.transactions.AddTransactionBottomSheet
import com.finsave.feature.transactions.AddTransactionViewModel
import com.finsave.feature.transactions.TransactionsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Main Navigation Host for FinSave.
 *
 * Connects all feature modules together.
 * The starting destination will depend on whether onboarding is complete (checked in MainActivity).
 */
@Composable
fun FinSaveNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ONBOARDING
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ── Onboarding ────────────────────────────────────────────────────────
        composable(route = Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────────
        composable(route = Routes.DASHBOARD) {
            var showAddTransaction by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            val addTxViewModel: AddTransactionViewModel = hiltViewModel()

            DashboardScreen(
                onAddTransactionClick = { showAddTransaction = true },
                onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onNavigateToSplitter = { navController.navigate(Routes.SPLITTER) }
            )

            if (showAddTransaction) {
                AddTransactionBottomSheet(
                    viewModel = addTxViewModel,
                    onDismissRequest = { showAddTransaction = false }
                )
            }
        }

        // ── Transactions ──────────────────────────────────────────────────────
        composable(route = Routes.TRANSACTIONS) {
            TransactionsScreen()
        }

        // ── Accounts ──────────────────────────────────────────────────────────
        composable(route = Routes.ACCOUNTS) {
            AccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Categories ────────────────────────────────────────────────────────
        composable(route = Routes.CATEGORIES) {
            CategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Budget ────────────────────────────────────────────────────────────
        composable(route = Routes.BUDGET) {
            BudgetScreen()
        }

        // ── Insights ──────────────────────────────────────────────────────────
        composable(route = Routes.INSIGHTS) {
            InsightsScreen()
        }

        // ── Splitter ──────────────────────────────────────────────────────────
        composable(route = Routes.SPLITTER) {
            SplitterScreen(
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate("splitter_group/$groupId")
                }
            )
        }

        // ── Splitter Group Detail ─────────────────────────────────────────────
        composable(
            route = Routes.SPLITTER_GROUP,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) {
            GroupDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(route = Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) }
            )
        }
    }
}
