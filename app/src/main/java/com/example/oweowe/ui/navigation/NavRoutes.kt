package com.example.oweowe.ui.navigation

/**
 * The routing stack from the navigation model:
 *
 * ```
 * SignIn / Register (toggle)              (before authentication)
 * Home  Groups  AddExpense  Activity  Profile   (primary / bottom bar)
 * Groups -> GroupOverview -> Groups (back)
 * ```
 *
 * Add/edit expense, create/edit group and manage members open as bottom
 * sheets, so they have no destination of their own; [AddExpense] only names
 * the bottom-bar Plus tab that opens its sheet.
 */
sealed interface NavRoutes {

    /** ---- before authentication (S1 sign in / S2 register toggle) ---- */
    data object SignIn : NavRoutes {
        const val ROUTE = "signin"
    }

    /** ---- primary navigation (bottom bar) ---- */
    data object Home : NavRoutes {
        const val ROUTE = "home"
    }

    data object Groups : NavRoutes {
        const val ROUTE = "groups"
    }

    /** The bottom-bar Plus tab; it opens the Add expense sheet, not a destination. */
    data object AddExpense : NavRoutes {
        const val ROUTE = "add_expense"
    }

    data object Activity : NavRoutes {
        const val ROUTE = "activity"
    }

    data object Profile : NavRoutes {
        const val ROUTE = "profile"
    }

    /** ---- groups ---- */
    data object GroupOverview : NavRoutes {
        const val ROUTE = "group_overview"
        const val GROUP_ID_ARG = "groupId"
        const val ROUTE_WITH_ARGS = "$ROUTE/{$GROUP_ID_ARG}"

        fun route(groupId: String): String = "$ROUTE/$groupId"
    }
}
