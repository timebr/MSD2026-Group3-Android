package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Reads the group's current member IDs. NFR2: a group that was just created can
 * exist only as a local pending write, and an offline group only in the cache,
 * so the server read falls back to [Source.CACHE] in those two cases. Real
 * failures keep their reason (offline is NETWORK, not NOT_ALLOWED) for NFR4.
 */
internal suspend fun FirebaseFirestore.groupMemberIds(groupId: String): AppResult<List<String>> {
    val ref = collection("groups").document(groupId)
    val server = try {
        ref.get().await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val cached = cachedMembers(ref)
        if (cached != null && (e.isUnavailable() || cached.metadata.hasPendingWrites())) {
            return AppResult.Success(cached.memberIds())
        }
        return AppResult.Error(e.toErrorReason())
    }
    if (server.exists()) return AppResult.Success(server.memberIds())

    // The server does not know the group yet; it may still be a pending create.
    val cached = cachedMembers(ref)
    if (cached != null && cached.metadata.hasPendingWrites()) {
        return AppResult.Success(cached.memberIds())
    }
    return AppResult.Error(ErrorReason.NOT_ALLOWED)
}

private suspend fun cachedMembers(ref: DocumentReference): DocumentSnapshot? =
    try {
        ref.get(Source.CACHE).await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

private fun DocumentSnapshot.memberIds(): List<String> =
    (get("memberIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()

private fun Exception.isUnavailable(): Boolean =
    this is FirebaseFirestoreException &&
        code == FirebaseFirestoreException.Code.UNAVAILABLE
