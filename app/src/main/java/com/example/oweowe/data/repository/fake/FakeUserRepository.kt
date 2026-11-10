package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeUserRepository(private val db: FakeBackend) : UserRepository {

    override fun observeMe(): Flow<User?> =
        combine(db.currentUserId, db.users) { uid, users -> users.find { it.uid == uid } }

    override fun observeUsers(uids: List<String>): Flow<List<User>> =
        db.users.map { users -> users.filter { it.uid in uids } }

    override suspend fun updateProfile(fullName: String, phone: String): AppResult<Unit> =
        updateMe { it.copy(fullName = fullName.trim(), phone = phone.trim()) }

    override suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit> =
        updateMe { it.copy(pushEnabled = enabled) }

    /** Shared helper: change the signed-in user's profile. */
    private suspend fun updateMe(change: (User) -> User): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        db.users.update { users -> users.map { if (it.uid == me) change(it) else it } }
        return AppResult.Success(Unit)
    }
}