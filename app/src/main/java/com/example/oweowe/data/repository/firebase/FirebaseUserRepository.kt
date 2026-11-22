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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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
                    .map { snapshot -> if (snapshot.exists()) snapshot.toUser() else null }
            }
        }

    override fun observeUsers(uids: List<String>): Flow<List<User>> {
        val chunks = uids.distinct().chunked(30)   // whereIn accepts at most 30 values
        if (chunks.isEmpty()) return flowOf(emptyList())
        val flows = chunks.map { chunk ->
            users.whereIn(FieldPath.documentId(), chunk)
                .snapshots()
                .map { snapshot ->
                    val found = snapshot.documents.map { it.toUser() }.associateBy { it.uid }
                    chunk.map { uid -> found[uid] ?: User(uid = uid, fullName = User.UNKNOWN_FULL_NAME) }
                }
        }
        return combine(flows) { lists -> lists.flatMap { it }.distinctBy { it.uid } }
    }

    @Suppress("DEPRECATION")   // updateEmail is the synchronous profile-email change
    override suspend fun updateProfile(
        fullName: String,
        email: String,
        photoUri: String?,
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val mail = email.trim().lowercase()
        if (!mail.contains("@")) return AppResult.Error(ErrorReason.INVALID_EMAIL)

        return firebaseCall {
            // The email is the sign-in identity, so Auth and the profile document
            // must move together. Auth may demand a recent sign-in for this.
            if (user.email != mail) user.updateEmail(mail).await()

            val fields = mutableMapOf<String, Any>("fullName" to fullName.trim(), "email" to mail)
            if (photoUri != null) fields["photoUri"] = photoUri
            // No await(): Firestore applies the write locally at once and syncs it
            // when online (NFR1, NFR2).
            users.document(user.uid).update(fields)
            Unit
        }
    }

    override suspend fun setPushEnabled(enabled: Boolean): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        return firebaseCall {
            users.document(uid).update("pushEnabled", enabled)
            Unit
        }
    }
}
