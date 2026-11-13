package com.example.oweowe

import android.app.Application
import com.example.oweowe.data.connectivity.ConnectivityManagerConnectivityObserver

/** Owns the [AppContainer] so every Activity and ViewModel shares one wiring. */
class OweOweApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            connectivityObserver = ConnectivityManagerConnectivityObserver(this),
        )
    }
}
