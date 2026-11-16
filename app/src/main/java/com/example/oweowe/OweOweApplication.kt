package com.example.oweowe

import android.app.Application
import com.example.oweowe.data.connectivity.ConnectivityManagerConnectivityObserver
import com.example.oweowe.notifications.AndroidNotificationPresenter
import com.example.oweowe.notifications.NotificationCoordinator
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Owns the [AppContainer] so every Activity and ViewModel shares one wiring. */
class OweOweApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var notifications: NotificationCoordinator

    override fun onCreate() {
        super.onCreate()
        // google-services.json is git-ignored. A checkout without it has no
        // default FirebaseApp, so the app still runs on the in-memory backend.
        val useFirebase = FirebaseApp.getApps(this).isNotEmpty()
        container = AppContainer(
            useFirebase = useFirebase,
            connectivityObserver = ConnectivityManagerConnectivityObserver(this),
        )
        notifications = NotificationCoordinator(
            authRepository = container.authRepository,
            userRepository = container.userRepository,
            groupRepository = container.groupRepository,
            expenseRepository = container.expenseRepository,
            reminderRepository = container.reminderRepository,
            syncStatusStore = container.syncStatusStore,
            presenter = AndroidNotificationPresenter(this),
            scope = applicationScope,
        ).also { it.start() }
    }
}
