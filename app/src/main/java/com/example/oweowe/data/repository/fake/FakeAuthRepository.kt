package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ErrorReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.update

class FakeAuthRepository(private val db: FakeBackend) : AuthRepository {

    override val currentUserId: Flow<String?> = db.currentUserId

    override suspend fun register(
        fullName: String, email: String, password: String, phone: String,
    ): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val mail = email.trim().lowercase()
        if (!mail.contains("@")) return AppResult.Error(ErrorReason.INVALID_EMAIL)
        if (password.length < 6) return AppResult.Error(ErrorReason.WEAK_PASSWORD)
        if (db.users.value.any { it.email == mail }) return AppResult.Error(ErrorReason.EMAIL_ALREADY_IN_USE)

        val uid = db.newId()
        db.users.update {
            it + User(
                uid = uid,
                fullName = fullName.trim(),
                email = mail,
                phone = phone.trim(),
                createdAt = db.now()
            )
        }
        db.passwords[mail] = password
        db.currentUserId.value = uid   // Firebase also signs the user in after registering
        return AppResult.Success(Unit)
    }

    override suspend fun signIn(email: String, password: String): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val mail = email.trim().lowercase()
        val user = db.users.value.find { it.email == mail }
        if (user == null || db.passwords[mail] != password) {
            return AppResult.Error(ErrorReason.WRONG_CREDENTIALS)   // never say which part was wrong
        }
        db.currentUserId.value = user.uid
        return AppResult.Success(Unit)
    }

    override suspend fun signOut() {
        db.currentUserId.value = null
    }
}