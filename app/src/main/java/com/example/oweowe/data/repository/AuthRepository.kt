package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
interface AuthRepository {
    /** UID of the signed-in user, or null when signed out. Drives "login screen or Home". */
    val currentUserId: Flow<String?>

    /** Creates the Auth account AND the users/{uid} profile document. */
    suspend fun register(fullName: String, email: String, password: String, phone: String): AppResult<Unit>

    suspend fun signIn(email: String, password: String): AppResult<Unit>

    suspend fun signOut()
}