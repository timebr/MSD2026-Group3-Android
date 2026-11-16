package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Translates a Firebase exception into one of our named errors. */
internal fun Throwable.toErrorReason(): ErrorReason = when (this) {
    // order matters: WeakPassword is a special kind of InvalidCredentials
    is FirebaseAuthWeakPasswordException -> ErrorReason.WEAK_PASSWORD
    is FirebaseAuthUserCollisionException -> ErrorReason.EMAIL_ALREADY_IN_USE
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") ErrorReason.INVALID_EMAIL else ErrorReason.WRONG_CREDENTIALS
    is FirebaseAuthInvalidUserException -> ErrorReason.WRONG_CREDENTIALS
    is FirebaseNetworkException -> ErrorReason.NETWORK
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorReason.NOT_ALLOWED
        FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorReason.NETWORK
        else -> ErrorReason.UNKNOWN
    }
    else -> ErrorReason.UNKNOWN
}

/** Runs a Firebase call and returns Success, or Error with the right reason, instead of throwing. */
internal inline fun <T> firebaseCall(block: () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e   // never swallow coroutine cancellation
    } catch (e: Exception) {
        AppResult.Error(e.toErrorReason())
    }

/** The signed-in user's UID, or null when signed out; emits again on every login and logout. */
internal fun FirebaseAuth.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}