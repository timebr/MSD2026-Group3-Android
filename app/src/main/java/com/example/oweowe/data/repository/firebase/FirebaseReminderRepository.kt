package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.ReminderRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseReminderRepository(
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
) : ReminderRepository {

    private fun reminders(groupId: String) =
        db.collection("groups").document(groupId).collection("reminders")

    override suspend fun sendReminder(groupId: String, toUserId: String, amountOre: Long): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (amountOre < 0) return AppResult.Error(ErrorReason.INVALID_AMOUNT)
        val group = try {
            db.collection("groups").document(groupId).get().await()
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        }
        val memberIds = (group.get("memberIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        if (uid !in memberIds || toUserId !in memberIds) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }
        val ref = reminders(groupId).document()
        ref.set(
            mapOf(
                "id" to ref.id,
                "groupId" to groupId,
                "fromUserId" to uid,
                "toUserId" to toUserId,
                "amountOre" to amountOre,
                "createdAt" to System.currentTimeMillis(),
            ),
        )
        return AppResult.Success(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRemindersForMe(): Flow<List<Reminder>> =
        auth.uidFlow().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(emptyList())
            } else {
                db.collectionGroup("reminders")
                    .whereEqualTo("toUserId", uid)
                    .snapshots()
                    .map { snapshot -> snapshot.documents.map { it.toReminder() } }
            }
        }
}
