package com.example.oweowe.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.oweowe.AppContainer
import com.example.oweowe.ui.activity.ActivityScreen
import com.example.oweowe.ui.auth.AuthScreen
import com.example.oweowe.ui.components.SheetScoped
import com.example.oweowe.ui.expense.AddExpenseSheet
import com.example.oweowe.ui.expense.EditExpenseScreen
import com.example.oweowe.ui.groups.CreateGroupSheet
import com.example.oweowe.ui.groups.EditGroupSheet
import com.example.oweowe.ui.groups.GroupOverviewScreen
import com.example.oweowe.ui.groups.GroupsScreen
import com.example.oweowe.ui.groups.ManageMembersSheet
import com.example.oweowe.ui.home.HomeScreen
import com.example.oweowe.ui.profile.ProfileScreen

/**
 * The navigation stack from the navigation model, one composable per route.
 * Screens only get navigation lambdas; only this file and [com.example.oweowe.MainActivity]
 * ever touch the [NavHostController]. Swapping the Fake repositories for Firebase
 * ones changes nothing here.
 */
@Composable
fun OweOweNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val startDestination = remember(container) {
        if (container.initialUserId != null) NavRoutes.Home.ROUTE else NavRoutes.SignIn.ROUTE
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        // ---- before authentication (S1 / S2 toggle) ----
        composable(NavRoutes.SignIn.ROUTE) {
            AuthScreen(container = container)
        }

        // ---- primary navigation (bottom bar) ----
        composable(NavRoutes.Home.ROUTE) {
            var showAddExpense by rememberSaveable { mutableStateOf(false) }
            var addExpenseGroupId by rememberSaveable { mutableStateOf<String?>(null) }
            HomeScreen(
                container = container,
                onAddExpense = { groupId ->
                    addExpenseGroupId = groupId
                    showAddExpense = true
                },
                onGoToGroups = { navController.navigateToTab(NavRoutes.Groups.ROUTE) },
                onOpenSettings = { navController.navigateToTab(NavRoutes.Profile.ROUTE) },
            )
            // S11: adding an expense opens as a bottom sheet over the dashboard.
            if (showAddExpense) {
                SheetScoped {
                    AddExpenseSheet(
                        container = container,
                        initialGroupId = addExpenseGroupId,
                        onDismiss = { showAddExpense = false },
                    )
                }
            }
        }
        composable(NavRoutes.Groups.ROUTE) {
            var showCreateGroup by rememberSaveable { mutableStateOf(false) }
            GroupsScreen(
                container = container,
                onCreateGroup = { showCreateGroup = true },
                onOpenGroup = { groupId ->
                    navController.navigate(NavRoutes.GroupOverview.route(groupId))
                },
            )
            // S9: the create-group form is a bottom sheet, not a screen.
            if (showCreateGroup) {
                SheetScoped {
                    CreateGroupSheet(
                        container = container,
                        onDismiss = { showCreateGroup = false },
                        onGroupCreated = { groupId ->
                            showCreateGroup = false
                            navController.navigate(NavRoutes.GroupOverview.route(groupId)) {
                                popUpTo(NavRoutes.Groups.ROUTE)
                            }
                        },
                    )
                }
            }
        }
        composable(NavRoutes.Activity.ROUTE) {
            ActivityScreen(container = container)
        }
        composable(NavRoutes.Profile.ROUTE) {
            ProfileScreen(container = container)
        }

        // ---- groups ----
        composable(
            route = NavRoutes.GroupOverview.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(NavRoutes.GroupOverview.GROUP_ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            val groupId =
                entry.arguments?.getString(NavRoutes.GroupOverview.GROUP_ID_ARG).orEmpty()
            // The navigation model: leaving a group overview goes back to the
            // Groups tab (where the group was opened), so the system back gesture
            // follows the top bar's back arrow. We pop to the existing Groups
            // entry instead of switching tabs, otherwise the saved Groups stack
            // would restore with this overview still on top.
            BackHandler { navController.backToGroups() }
            var showEditGroup by rememberSaveable { mutableStateOf(false) }
            var showManageMembers by rememberSaveable { mutableStateOf(false) }
            var showAddExpense by rememberSaveable { mutableStateOf(false) }
            GroupOverviewScreen(
                container = container,
                groupId = groupId,
                onBack = { navController.backToGroups() },
                onEditGroup = { showEditGroup = true },
                onManageMembers = { showManageMembers = true },
                onAddExpense = { showAddExpense = true },
                onEditExpense = { expenseId ->
                    navController.navigate(NavRoutes.EditExpense.route(groupId, expenseId))
                },
            )
            // S7 / S8 / S11: group actions open as bottom sheets over the overview.
            if (showEditGroup) {
                SheetScoped {
                    EditGroupSheet(
                        container = container,
                        groupId = groupId,
                        onDismiss = { showEditGroup = false },
                    )
                }
            }
            if (showManageMembers) {
                SheetScoped {
                    ManageMembersSheet(
                        container = container,
                        groupId = groupId,
                        onDismiss = { showManageMembers = false },
                    )
                }
            }
            if (showAddExpense) {
                SheetScoped {
                    AddExpenseSheet(
                        container = container,
                        initialGroupId = groupId,
                        onDismiss = { showAddExpense = false },
                    )
                }
            }
        }
        composable(
            route = NavRoutes.EditExpense.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(NavRoutes.EditExpense.GROUP_ID_ARG) { type = NavType.StringType },
                navArgument(NavRoutes.EditExpense.EXPENSE_ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            EditExpenseScreen(
                container = container,
                groupId = entry.arguments?.getString(NavRoutes.EditExpense.GROUP_ID_ARG).orEmpty(),
                expenseId = entry.arguments?.getString(NavRoutes.EditExpense.EXPENSE_ID_ARG).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Leaving a group overview returns to the Groups tab without losing its
 * position (FR3, S4). Falls back to a tab switch when Groups is not in the
 * stack, e.g. after the process was restored.
 */
private fun NavHostController.backToGroups() {
    if (!popBackStack(NavRoutes.Groups.ROUTE, inclusive = false)) {
        navigateToTab(NavRoutes.Groups.ROUTE)
    }
}

/** Tab semantics: switch primary destination, but keep each tab's own back stack. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
