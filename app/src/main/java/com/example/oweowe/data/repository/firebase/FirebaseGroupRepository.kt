package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.domain.normalizePhone
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseGroupRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
) : GroupRepository {

    private val groups = db.collection("groups")
    private val users = db.collection("users")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyGroups(): Flow<List<Group>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                groups.whereArrayContains("memberIds", uid)
                    .snapshots()
                    .map { snapshot ->
                        snapshot.documents
                            .mapNotNull { it.toObject(Group::class.java)?.copy(id = it.id) }
                            .sortedBy { it.name }
                    }
                    .catch { emit(emptyList()) }
            }
        }

    override fun observeGroup(groupId: String): Flow<Group?> =
        groups.document(groupId).snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null else snapshot.toObject(Group::class.java)?.copy(id = snapshot.id)
            }
            .catch { emit(null) }

    override suspend fun createGroup(name: String, description: String): AppResult<String> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (name.isBlank()) return AppResult.Error(ErrorReason.UNKNOWN)
        val ref = groups.document()
        val group = Group(
            id = ref.id,
            name = name.trim(),
            description = description.trim(),
            createdBy = uid,
            createdAt = System.currentTimeMillis(),
            memberIds = listOf(uid),
        )
        // No await(): the write is in the local cache immediately and syncs later.
        ref.set(group)
        return AppResult.Success(ref.id)
    }

    override suspend fun updateDetails(groupId: String, name: String, description: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (name.isBlank()) return AppResult.Error(ErrorReason.UNKNOWN)
        if (!isMember(groupId, uid)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        groups.document(groupId).update(
            mapOf("name" to name.trim(), "description" to description.trim()),
        )
        return AppResult.Success(Unit)
    }

    override suspend fun addMemberByEmail(groupId: String, email: String): AppResult<User> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (!isMember(groupId, uid)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val user = try {
            val doc = users.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
                .documents.firstOrNull()
            doc?.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        } ?: return AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_EMAIL)
        return addMember(groupId, uid, user)
    }

    override suspend fun addMemberByPhone(groupId: String, phone: String): AppResult<User> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (!isMember(groupId, uid)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val normalized = normalizePhone(phone)
        if (normalized.isEmpty()) return AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_PHONE)
        val user = try {
            val doc = users.whereEqualTo("phoneNormalized", normalized).limit(1).get().await()
                .documents.firstOrNull()
            doc?.toObject(User::class.java)?.copy(uid = doc.id)
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        } ?: return AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_PHONE)
        return addMember(groupId, uid, user)
    }

    private suspend fun addMember(groupId: String, callerId: String, user: User): AppResult<User> {
        val memberIds = currentMemberIds(groupId) ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (callerId !in memberIds) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (user.uid in memberIds) return AppResult.Error(ErrorReason.ALREADY_MEMBER)
        groups.document(groupId).update("memberIds", FieldValue.arrayUnion(user.uid))
        return AppResult.Success(user)
    }

    override suspend fun removeMember(groupId: String, userId: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (userId == uid) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val memberIds = currentMemberIds(groupId) ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (uid !in memberIds) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (userId !in memberIds) return AppResult.Error(ErrorReason.NOT_ALLOWED)

        val expenses = try {
            groups.document(groupId).collection("expenses").get().await()
                .documents.map { it.toExpense(groupId, SyncStatus.SYNCED) }
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        }
        if (!BalanceCalculator.isSettled(expenses, userId)) {
            return AppResult.Error(ErrorReason.BALANCE_NOT_ZERO)
        }
        groups.document(groupId).update("memberIds", FieldValue.arrayRemove(userId))
        return AppResult.Success(Unit)
    }

    private suspend fun isMember(groupId: String, uid: String): Boolean =
        currentMemberIds(groupId)?.contains(uid) == true

    private suspend fun currentMemberIds(groupId: String): List<String>? =
        when (val members = db.groupMemberIds(groupId)) {
            is AppResult.Success -> members.value
            is AppResult.Error -> null
        }
}
