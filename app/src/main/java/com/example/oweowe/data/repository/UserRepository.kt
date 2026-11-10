package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
import com.example.oweowe.data.model.User
interface UserRepository {
    fun observeMe(): Flow<User?>

    /** Profiles for a list of UIDs, e.g. a group's memberIds, to show names and initials. */
    fun observeUsers(uids: List<String>): Flow<List<User>>

    suspend fun updateProfile(fullName: String, phone: String): AppResult<Unit>

    suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit>
}