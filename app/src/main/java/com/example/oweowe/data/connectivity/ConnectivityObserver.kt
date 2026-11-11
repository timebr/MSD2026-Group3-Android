package com.example.oweowe.data.connectivity

import kotlinx.coroutines.flow.Flow

/**
 * Whether the device currently has a network connection. The UI shows an offline
 * banner from this (NFR3) but never blocks on it; the repository layer decides
 * what data is still served when offline.
 *
 * Implementations: [ConnectivityManagerConnectivityObserver] on the device,
 * [AlwaysOnlineObserver] / [AlwaysOfflineObserver] in tests and previews.
 */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
