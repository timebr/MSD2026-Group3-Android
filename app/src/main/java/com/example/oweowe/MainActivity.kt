package com.example.oweowe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.oweowe.ui.components.SheetScoped
import com.example.oweowe.ui.expense.AddExpenseSheet
import com.example.oweowe.ui.navigation.NavRoutes
import com.example.oweowe.ui.navigation.OweOweNavHost
import com.example.oweowe.ui.theme.OweOweTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as OweOweApplication).container
        setContent {
            OweOweTheme {
                OweOweApp(container = container)
            }
        }
    }
}

/** One tab of the primary (bottom bar) navigation. */
private data class BottomBarTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomBarTabs = listOf(
    BottomBarTab(NavRoutes.Home.ROUTE, R.string.nav_home, Icons.Filled.Home),
    BottomBarTab(NavRoutes.Groups.ROUTE, R.string.nav_groups, Icons.AutoMirrored.Filled.List),
    BottomBarTab(NavRoutes.AddExpense.ROUTE, R.string.nav_add_expense, Icons.Filled.Add),
    BottomBarTab(NavRoutes.Activity.ROUTE, R.string.nav_activity, Icons.Filled.Notifications),
    BottomBarTab(NavRoutes.Profile.ROUTE, R.string.nav_profile, Icons.Filled.Person),
)

/** The bar is only shown on the primary destinations, never on auth or detail screens. */
private fun tabFor(route: String?): BottomBarTab? =
    bottomBarTabs.firstOrNull { tab -> route == tab.route || route?.startsWith("${tab.route}?") == true }

/**
 * App root: the auth gate and the primary navigation. Sign in / out is not
 * navigated from any screen — the repository's `currentUserId` flips and this
 * composable switches between the auth stack and Home, so it works the same
 * once Firebase is wired in.
 */
@Composable
fun OweOweApp(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // FR7: Android 13+ needs a runtime grant before local notifications appear.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val signedInUserId by container.authRepository.currentUserId
        .collectAsStateWithLifecycle(initialValue = container.initialUserId)
    LaunchedEffect(signedInUserId) {
        val route = navController.currentDestination?.route ?: return@LaunchedEffect
        val onAuthScreen = route == NavRoutes.SignIn.ROUTE
        when {
            signedInUserId == null && !onAuthScreen ->
                navController.navigate(NavRoutes.SignIn.ROUTE) {
                    popUpTo(0) { inclusive = true }   // leave no signed-out state behind
                }
            signedInUserId != null && onAuthScreen ->
                navController.navigate(NavRoutes.Home.ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = tabFor(backStackEntry?.destination?.route)

    // S11: the bottom-bar Plus opens the Add expense sheet over the current tab,
    // instead of switching to a separate destination.
    var showAddExpenseSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    bottomBarTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = {
                                if (tab.route == NavRoutes.AddExpense.ROUTE) {
                                    showAddExpenseSheet = true
                                } else if (tab != currentTab) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    // "Add expense" wraps onto two lines in a five-tab bar;
                                    // without this the second line starts at the item's left edge.
                                    textAlign = TextAlign.Center,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        OweOweNavHost(
            navController = navController,
            container = container,
            // Only the bar's height; each screen's own Scaffold insets its top bar.
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        )
        if (showAddExpenseSheet) {
            SheetScoped {
                AddExpenseSheet(
                    container = container,
                    initialGroupId = null,
                    onDismiss = { showAddExpenseSheet = false },
                )
            }
        }
    }
}
