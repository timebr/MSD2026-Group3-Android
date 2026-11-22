package com.example.oweowe.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen

/**
 * NFR4: a failed repository read shows [errorState] instead of leaving the screen
 * stuck, and each emission of [retry] re-collects the (cold) source flow.
 *
 * Firestore answers reads from its offline cache without throwing, so this path
 * only runs when a listener really fails (e.g. Security Rules). Anything the user
 * typed lives in the ViewModel's own state, not in these flows, so text already
 * entered is still there after a successful retry.
 */
internal fun <T> Flow<T>.retryOnReadError(
    retry: Flow<Unit>,
    errorState: () -> T,
): Flow<T> = retryWhen { cause, _ ->
    if (cause is CancellationException) return@retryWhen false
    emit(errorState())
    retry.first()
    true
}
