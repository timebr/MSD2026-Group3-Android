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
        db.users.map { users ->
            uids.distinct().map { uid -> users.find { it.uid == uid } ?: User(uid = uid, fullName = uid) }
        }

    override suspend fun updateProfile(
        fullName: String,
        email: String,
        photoUri: String?,
    ): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val mail = email.trim().lowercase()
        if (!mail.contains("@")) return AppResult.Error(ErrorReason.INVALID_EMAIL)
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (db.users.value.any { it.uid != me && it.email == mail }) {
            return AppResult.Error(ErrorReason.EMAIL_ALREADY_IN_USE)
        }
        val oldMail = db.users.value.find { it.uid == me }?.email
        if (oldMail != null && oldMail != mail) {
            // Sign-in looks the password up by email, so the entry has to move
            // with the address or the user could no longer sign in.
            val password = db.passwords.remove(oldMail) ?: return AppResult.Error(ErrorReason.UNKNOWN)
            db.passwords[mail] = password
        }
        db.users.update { users ->
            users.map {
                if (it.uid != me) {
                    it
                } else {
                    it.copy(
                        fullName = fullName.trim(),
                        email = mail,
                        // null means "no new photo", so saving the name keeps the old one.
                        photoUri = photoUri ?: it.photoUri,
                    )
                }
            }
        }
        return AppResult.Success(Unit)
    }

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