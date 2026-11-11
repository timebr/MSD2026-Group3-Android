package com.example.oweowe.ui.components

import com.example.oweowe.data.repository.ErrorReason

/**
 * State of a write triggered from a form (create group, add expense, save
 * profile, …). This is how every write shows pending / success / failure, with
 * Retry offered on failure (NFR2, NFR4): the user is never blocked while busy.
 */
sealed interface WriteState {
    data object Idle : WriteState
    data object Busy : WriteState
    data object Done : WriteState
    data class Failed(val reason: ErrorReason) : WriteState
}
