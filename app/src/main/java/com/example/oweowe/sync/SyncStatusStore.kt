package com.example.oweowe.sync

import com.example.oweowe.data.model.SyncStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A write reached the server after being pending or failed (NFR4, NFR5). */
data class SyncEvent(
    val documentId: String,
    val label: String,
    val status: SyncStatus,
)

/**
 * Keeps the status of writes made by this process. Firestore's snapshot
 * metadata already says whether a document has pending writes; this store adds
 * the failed state and a one-off event when a pending write finally succeeds.
 */
class SyncStatusStore {

    private val _statuses = MutableStateFlow<Map<String, SyncStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, SyncStatus>> = _statuses.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    private val labels = mutableMapOf<String, String>()

    fun markPending(documentId: String, label: String) {
        labels[documentId] = label
        setStatus(documentId, SyncStatus.PENDING)
    }

    fun markSynced(documentId: String) {
        val previous = _statuses.value[documentId]
        setStatus(documentId, SyncStatus.SYNCED)
        if (previous == SyncStatus.PENDING || previous == SyncStatus.FAILED) {
            _events.tryEmit(
                SyncEvent(documentId, labels[documentId] ?: documentId, SyncStatus.SYNCED),
            )
        }
    }

    fun markFailed(documentId: String) {
        setStatus(documentId, SyncStatus.FAILED)
    }

    fun statusFor(documentId: String, metadataHasPendingWrites: Boolean): SyncStatus =
        _statuses.value[documentId]
            ?: if (metadataHasPendingWrites) SyncStatus.PENDING else SyncStatus.SYNCED

    private fun setStatus(documentId: String, status: SyncStatus) =
        _statuses.update { it + (documentId to status) }
}
