package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirebaseUserRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    db: FirebaseFirestore = Firebase.firestore,
) : UserRepository {

    private val users = db.collection("users")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMe(): Flow<User?> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                users.document(uid).snapshots()
                    .map { snapshot -> snapshot.toObject(User::class.java) }
                    .catch { emit(null) }
            }
        }

    override fun observeUsers(uids: List<String>): Flow<List<User>> {
        if (uids.isEmpty()) return flowOf(emptyList())   // Firestore refuses an empty whereIn
        return users.whereIn(FieldPath.documentId(), uids.distinct().take(30))
            .snapshots()
            .map { snapshot -> snapshot.toObjects(User::class.java) }
            .catch { emit(emptyList()) }
    }

    override suspend fun updateProfile(fullName: String, phone: String): AppResult<Unit> =
        updateMe(mapOf("fullName" to fullName.trim(), "phone" to phone.trim()))

    override suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit> =
        updateMe(mapOf("pushEnabled" to enabled))

    private fun updateMe(fields: Map<String, Any>): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        // No await(): Firestore stores the change locally at once and syncs it when online (NFR1, NFR2).
        users.document(uid).update(fields)
        return AppResult.Success(Unit)
    }
}