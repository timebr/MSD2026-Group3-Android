package com.example.oweowe.ui.navigation

/**
 * The routing stack from the navigation model:
 *
 * ```
 * SignIn <-> Register                     (before authentication)
 * Home  Groups  AddExpense  Activity  Profile   (primary / bottom bar)
 * Groups -> CreateGroup -> GroupOverview
 * Groups -> GroupOverview -> EditGroup | ManageMembers | AddExpense | Home
 * Profile -> EditProfile | NotificationSettings
 * ```
 */
sealed interface NavRoutes {

    /** ---- before authentication ---- */
    data object SignIn : NavRoutes {
        const val ROUTE = "signin"
    }

    data object Register : NavRoutes {
        const val ROUTE = "register"
    }

    /** ---- primary navigation (bottom bar) ---- */
    data object Home : NavRoutes {
        const val ROUTE = "home"
    }

    data object Groups : NavRoutes {
        const val ROUTE = "groups"
    }

    data object AddExpense : NavRoutes {
        const val ROUTE = "add_expense"
        const val GROUP_ID_ARG = "groupId"
        const val ROUTE_WITH_ARGS = "$ROUTE?$GROUP_ID_ARG={$GROUP_ID_ARG}"

        /** Preselects a group, e.g. from Home or a Group overview. */
        fun route(groupId: String? = null): String =
            if (groupId == null) ROUTE else "$ROUTE?$GROUP_ID_ARG=$groupId"
    }

    data object Activity : NavRoutes {
        const val ROUTE = "activity"
    }

    data object Profile : NavRoutes {
        const val ROUTE = "profile"
    }

    /** ---- groups ---- */
    data object CreateGroup : NavRoutes {
        const val ROUTE = "create_group"
    }

    data object GroupOverview : NavRoutes {
        const val ROUTE = "group_overview"
        const val GROUP_ID_ARG = "groupId"
        const val ROUTE_WITH_ARGS = "$ROUTE/{$GROUP_ID_ARG}"

        fun route(groupId: String): String = "$ROUTE/$groupId"
    }

    data object EditGroup : NavRoutes {
        const val ROUTE = "edit_group"
        const val GROUP_ID_ARG = "groupId"
        const val ROUTE_WITH_ARGS = "$ROUTE/{$GROUP_ID_ARG}"

        fun route(groupId: String): String = "$ROUTE/$groupId"
    }

    data object ManageMembers : NavRoutes {
        const val ROUTE = "manage_members"
        const val GROUP_ID_ARG = "groupId"
        const val ROUTE_WITH_ARGS = "$ROUTE/{$GROUP_ID_ARG}"

        fun route(groupId: String): String = "$ROUTE/$groupId"
    }

    /** ---- profile ---- */
    data object EditProfile : NavRoutes {
        const val ROUTE = "edit_profile"
    }

    data object NotificationSettings : NavRoutes {
        const val ROUTE = "notification_settings"
    }
}

/** Destinations that belong to the bottom bar (the "primary navigation"). */
val bottomBarRoutes = listOf(
    NavRoutes.Home.ROUTE,
    NavRoutes.Groups.ROUTE,
    NavRoutes.AddExpense.ROUTE,
    NavRoutes.Activity.ROUTE,
    NavRoutes.Profile.ROUTE,
)
