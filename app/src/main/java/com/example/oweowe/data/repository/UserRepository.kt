package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
import com.example.oweowe.data.model.User
interface UserRepository {
    fun observeMe(): Flow<User?>

    /** Profiles for a list of UIDs, e.g. a group's memberIds, to show names and initials. */
    fun observeUsers(uids: List<String>): Flow<List<User>>

    /**
     * FR2: update the signed-in user's own profile. The phone is deliberately
     * not a parameter: it identifies the account and stays read-only.
     */
    suspend fun updateProfile(
        fullName: String,
        email: String,
        /** FR2: the photo to keep, or null to leave the current one untouched. */
        photoUri: String? = null,
    ): AppResult<Unit>

    suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit>
}