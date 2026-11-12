package com.example.oweowe.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.oweowe.AppContainer
import com.example.oweowe.ui.activity.ActivityScreen
import com.example.oweowe.ui.auth.RegisterScreen
import com.example.oweowe.ui.auth.SignInScreen
import com.example.oweowe.ui.expense.AddExpenseScreen
import com.example.oweowe.ui.groups.CreateGroupScreen
import com.example.oweowe.ui.groups.EditGroupScreen
import com.example.oweowe.ui.groups.GroupOverviewScreen
import com.example.oweowe.ui.groups.GroupsScreen
import com.example.oweowe.ui.groups.ManageMembersScreen
import com.example.oweowe.ui.home.HomeScreen
import com.example.oweowe.ui.profile.EditProfileScreen
import com.example.oweowe.ui.profile.NotificationSettingsScreen
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
        // ---- before authentication ----
        composable(NavRoutes.SignIn.ROUTE) {
            SignInScreen(
                container = container,
                onNavigateToRegister = { navController.navigate(NavRoutes.Register.ROUTE) },
            )
        }
        composable(NavRoutes.Register.ROUTE) {
            RegisterScreen(
                container = container,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ---- primary navigation (bottom bar) ----
        composable(NavRoutes.Home.ROUTE) {
            HomeScreen(
                container = container,
                onAddExpense = { groupId ->
                    navController.navigate(NavRoutes.AddExpense.route(groupId))
                },
                onGoToGroups = { navController.navigateToTab(NavRoutes.Groups.ROUTE) },
            )
        }
        composable(NavRoutes.Groups.ROUTE) {
            GroupsScreen(
                container = container,
                onCreateGroup = { navController.navigate(NavRoutes.CreateGroup.ROUTE) },
                onOpenGroup = { groupId ->
                    navController.navigate(NavRoutes.GroupOverview.route(groupId))
                },
            )
        }
        composable(
            route = NavRoutes.AddExpense.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(NavRoutes.AddExpense.GROUP_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = ""   // the bottom bar opens the form without a preselected group
                },
            ),
        ) { entry ->
            AddExpenseScreen(
                container = container,
                initialGroupId = entry.arguments
                    ?.getString(NavRoutes.AddExpense.GROUP_ID_ARG)
                    ?.takeIf { it.isNotEmpty() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.Activity.ROUTE) {
            ActivityScreen(container = container)
        }
        composable(NavRoutes.Profile.ROUTE) {
            ProfileScreen(
                container = container,
                onEditProfile = { navController.navigate(NavRoutes.EditProfile.ROUTE) },
                onNotificationSettings = {
                    navController.navigate(NavRoutes.NotificationSettings.ROUTE)
                },
            )
        }

        // ---- groups ----
        composable(NavRoutes.CreateGroup.ROUTE) {
            CreateGroupScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    navController.navigate(NavRoutes.GroupOverview.route(groupId)) {
                        // The form must not sit on the back stack behind the new group.
                        popUpTo(NavRoutes.Groups.ROUTE)
                    }
                },
            )
        }
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
            // follows the top bar's back arrow.
            BackHandler { navController.navigateToTab(NavRoutes.Groups.ROUTE) }
            GroupOverviewScreen(
                container = container,
                groupId = groupId,
                onBack = { navController.navigateToTab(NavRoutes.Groups.ROUTE) },
                onEditGroup = { navController.navigate(NavRoutes.EditGroup.route(groupId)) },
                onManageMembers = { navController.navigate(NavRoutes.ManageMembers.route(groupId)) },
                onAddExpense = { navController.navigate(NavRoutes.AddExpense.route(groupId)) },
            )
        }
        composable(
            route = NavRoutes.EditGroup.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(NavRoutes.EditGroup.GROUP_ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            EditGroupScreen(
                container = container,
                groupId = entry.arguments?.getString(NavRoutes.EditGroup.GROUP_ID_ARG).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = NavRoutes.ManageMembers.ROUTE_WITH_ARGS,
            arguments = listOf(
                navArgument(NavRoutes.ManageMembers.GROUP_ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            ManageMembersScreen(
                container = container,
                groupId = entry.arguments?.getString(NavRoutes.ManageMembers.GROUP_ID_ARG).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }

        // ---- profile ----
        composable(NavRoutes.EditProfile.ROUTE) {
            EditProfileScreen(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.NotificationSettings.ROUTE) {
            NotificationSettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
            )
        }
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
